# Deploying the PollenInfo backend

The runbook for the server's container image, the production stack and the VPS it runs on.

## Build and run the server image locally

The image holds only a JRE and the server distribution; Gradle builds the distribution outside
Docker, because the project cannot be configured without an Android SDK (`settings.gradle.kts`
includes the Android modules).

```sh
./gradlew :server:installDist
docker build -t polleninfo-server:local server
```

Run it against the development database (`compose.dev.yaml` puts Postgres on the network
`polleninfo-dev`, under the host name `postgres`):

```sh
docker compose -f deploy/compose.dev.yaml up -d --wait
docker run --rm --network polleninfo-dev -p 127.0.0.1:8080:8080 \
  -e POLLENINFO_ENV=development \
  -e DB_URL=jdbc:postgresql://postgres:5432/polleninfo \
  polleninfo-server:local
curl http://localhost:8080/health   # OK
```

The development passwords are `ServerConfig`'s defaults, so nothing else is needed. If the server
stops at a Flyway checksum mismatch, the development database predates the current `V1` migration:
reset it with `docker compose -f deploy/compose.dev.yaml down -v`.

What the image is:

- `eclipse-temurin` 21 JRE on Ubuntu, pinned by tag and digest (Dependabot proposes updates).
- Runs as the fixed non-root uid/gid **10001** — production secret files are owned by this uid with
  mode `0400`.
- `JAVA_OPTS="-Xmx512m -XX:+ExitOnOutOfMemoryError"`; override it with `-e JAVA_OPTS=…`.
- Listens on `8080`. Configuration comes from the environment (see "Configuration" in `CLAUDE.md`);
  the image contains no secrets and no test classes (`server/.dockerignore` admits only
  `build/install/server`).

## The production stack

`deploy/compose.yaml` is the whole production setup, three services:

| Service | Image | Published ports | Memory | Notes |
| --- | --- | --- | --- | --- |
| `caddy` | `caddy:2.11.7` | `80`, `443/tcp`, `443/udp` | 128 MB | HTTPS for `SITE_ADDRESS` (default `api.polleninfo.ch`), certificates obtained and renewed automatically |
| `server` | `ghcr.io/timstenzel/polleninfo-server:${SERVER_TAG}` | none | 700 MB | Read-only root, `/tmp` a tmpfs, the image's uid 10001, `POLLENINFO_ENV=production` |
| `postgres` | `postgres:18.6` (= `TestPostgres.IMAGE`) | none | 512 MB | Volume `pgdata`, `shared_buffers` 128 MB, `max_connections` 20, SCRAM only |

- **Only `caddy` publishes ports — never add a `ports:` to another service.** The database and the
  server's `8080` are reachable only on the stack's networks: `edge` (Caddy and the server, with a
  route out for certificates, MeteoSwiss and FCM) and `db` (the server and Postgres, `internal`, no
  route out). `postgres/pg_hba.conf` lets the two PollenInfo roles in by password from those
  networks only, and the superuser only over the container's socket as the OS user `postgres`.
- Every service: `restart: unless-stopped`, `no-new-privileges`, a memory limit and `json-file`
  logs capped at 3 × 10 MB.
- **`caddy/Caddyfile`:** reverse proxy to `server:8080`; `Strict-Transport-Security
  "max-age=31536000; includeSubDomains"`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: no-referrer`; no `Server` or `Via` header; bodies over 16 KB refused; header,
  body, write and idle timeouts; no access log (the server's call log covers requests); plain HTTP
  redirected to HTTPS. Caddy sets `X-Forwarded-For` itself and drops what a client sent, which is
  why the server may trust it (`TRUSTED_PROXY`, `true` in production).
- **Stopping** (`docker compose stop`, a deploy) sends `SIGTERM`: Netty refuses new requests and
  gives running ones 2 s (at most 5 s), then the alarm scheduler lets a running tick finish (at
  most 45 s, `TICK_STOP_TIMEOUT`) and logs "Alarm scheduler: the running tick finished, stopped",
  then the database pool closes. `stop_grace_period: 75s` covers all of it; `DeployFilesTest`
  keeps the two in step. Usually no tick is running and the server is gone in about 2 s.
- `deploy/.env` (from `.env.example`, never committed) sets `SERVER_TAG` and `SECRETS_DIR`.

### Secrets

Every password and key is a file under `SECRETS_DIR` (default `/opt/polleninfo/secrets`), mounted
into `/run/secrets/` — never an environment variable, never in the repository
(`secrets.example/` holds placeholders of the layout). Compose without Swarm bind-mounts a secret
file as it is and ignores `uid` / `mode`, so **each file is owned by the uid of the container that
reads it, mode `0400`**: the `postgres/` files by 999 (`postgres` in the Postgres image), the
`server/` files by 10001 (the server image). The two database passwords are needed by both
containers, so each exists twice. `install-secrets.sh` creates all of it:

```sh
sudo deploy/install-secrets.sh --fcm-key ~/polleninfo-firebase-adminsdk.json   # /opt/polleninfo/secrets
```

It generates each missing password (`openssl rand -base64 32`), keeps existing ones — Postgres reads
them only when its volume is created, so a new one would lock the server out — and installs or
replaces the FCM key. Run again, it only re-asserts ownership and modes.

### Pulling the image

The image's package on GHCR is private, so the VPS has to log in before `docker compose pull` (or
the first `up`) can fetch `ghcr.io/timstenzel/polleninfo-server`. Create a **classic** personal
access token with the scope **`read:packages` only** (GitHub Packages does not accept fine-grained
tokens) and an expiry, then, as the user that runs compose:

```sh
echo <token> | docker login ghcr.io -u timstenzel --password-stdin
```

Docker keeps it in that user's `~/.docker/config.json`, base64-encoded rather than encrypted, so the
token can do nothing but read packages. When it expires, pulls fail with `denied`: create a new one
and log in again. The running stack is unaffected until the next pull.

### Looking into the database

Postgres publishes no port, so there is no connection from outside the VPS: SSH in and run `psql`
inside the container (`postgres/pg_hba.conf` allows exactly that). A GUI client would need a
published port, which this stack forbids on purpose.

```sh
cd /opt/polleninfo/deploy

# As the app login (preferred): DML only, it cannot create, alter, drop or truncate a table.
# The password comes from the secret file mounted in the container; it is never typed or shown.
docker compose exec -u postgres postgres sh -c \
  'PGPASSWORD=$(cat /run/secrets/db_app_password) psql -U polleninfo_app -d polleninfo'

# As the superuser (peer authentication over the socket, no password) — for structural repairs only.
docker compose exec -u postgres postgres psql -d polleninfo
```

To only look, start the session with `SET default_transaction_read_only = on;`. Useful queries:

```sql
\dt                                    -- the tables
SELECT id, fcm_token IS NOT NULL AS deliverable, created_at, last_seen_at
  FROM devices ORDER BY last_seen_at DESC;
SELECT id, device_id, enabled, station_abbr, type, at_time, from_time, until_time, species, days
  FROM alarms ORDER BY created_at;
SELECT * FROM notification_log ORDER BY local_date DESC;
SELECT * FROM scheduler_state;         -- the last minute the alarm scheduler finished
```

What it cannot tell you: which phone a device is (only the token's SHA-256 is stored, and devices
are anonymous), anything from the diary (it never leaves the phone), or whether a push arrived —
for what was sent and FCM's answer, see `docker compose logs server`.

## Running it on the VPS

Production is one Infomaniak VPS (Ubuntu 26.04, 1 vCPU, 2 GB RAM, 20 GB disk) at
`179.237.126.96` / `2001:1600:18:20b::24c`, answering as `https://api.polleninfo.ch`. The operator
logs in as `tim` (key only); the stack lives in `/opt/polleninfo/deploy`, its secrets in
`/opt/polleninfo/secrets`.

### DNS at Hostpoint

`polleninfo.ch`'s DNS is at Hostpoint, whose wildcard `*.polleninfo.ch` points at its own hosting
(`217.26.48.101`). `api` needs records of its own, which win over the wildcard:

| Name | Type | Value | TTL |
| --- | --- | --- | --- |
| `api.polleninfo.ch` | `A` | `179.237.126.96` | 300 |
| `api.polleninfo.ch` | `AAAA` | `2001:1600:18:20b::24c` | 300 |

Add the `AAAA` record only while the server really answers on that address
(`ip -6 addr show scope global` lists it, and the firewall below admits IPv6): clients prefer IPv6,
and an address that does not answer breaks them and certificate validation. Leave the other records
alone. Nothing in Infomaniak's panel affects this — at most reverse DNS, which nothing here needs.

**Before the first start, every Hostpoint name server must give the new address.** Caddy asks Let's
Encrypt for a certificate as soon as it starts; validation against the old address fails, and
repeated failures are rate-limited for hours:

```sh
for ns in ns.hostpoint.ch ns2.hostpoint.ch ns3.hostpoint.ch 1.1.1.1; do
  echo "$ns: $(dig @$ns +short A api.polleninfo.ch) $(dig @$ns +short AAAA api.polleninfo.ch)"
done
```

### Infomaniak's firewall

The panel's firewall sits in front of the VPS; `ufw` on the server enforces the same set again.
Allow, from everywhere (IPv4 and IPv6), and nothing else: TCP 22, TCP 80 (certificate validation
and the redirect to HTTPS), TCP 443, UDP 443 (HTTP/3) and ICMP (IPv6 needs it for path-MTU
discovery). Never 5432 or 8080 — see "Only Caddy publishes ports" below.

### Provisioning

Once, on the fresh server, as root over SSH with your key in `/root/.ssh/authorized_keys`:

```sh
scp deploy/provision.sh root@179.237.126.96:
ssh -t root@179.237.126.96 bash provision.sh          # --user <name> for another operator than tim
```

`provision.sh` creates the operator (sudo and docker groups, root's authorized key, a password that
it asks for — sudo needs it, SSH never accepts it), switches off root and password login over SSH,
enables `ufw` (OpenSSH, 80/tcp, 443/tcp, 443/udp), fail2ban's sshd jail and unattended security
upgrades (rebooting at 04:00 when one needs it), sets the time zone to `Europe/Zurich` (so 04:00 is
Swiss night, away from the alarms), installs Docker Engine and the Compose plugin from Docker's apt
repository with capped `json-file` logs, adds a 2 GB swap file and creates `/opt/polleninfo/`. Its
header lists every step.

**Keep the root session open** until `ssh tim@179.237.126.96` works in a second terminal and
`ssh root@179.237.126.96` is refused. Run again, the script re-checks everything and ends with "No
change: the server was already provisioned." Docker publishes a `resolute` (26.04) suite; on a
release it does not support yet the script stops before touching apt — use Ubuntu's own `docker.io`
and `docker-compose-v2` packages until it does.

### First start

1. **Secrets.** Download a service-account key in the Firebase console (Project settings → Service
   accounts → Generate new private key), copy it and the secrets script over, install, and delete
   the copies:

   ```sh
   scp deploy/install-secrets.sh ~/Downloads/polleninfo-firebase-adminsdk-*.json tim@api.polleninfo.ch:
   ssh -t tim@api.polleninfo.ch 'sudo ./install-secrets.sh --fcm-key polleninfo-firebase-adminsdk-*.json \
     && rm install-secrets.sh polleninfo-firebase-adminsdk-*.json'
   ```

   The database passwords are generated on the server (`openssl rand -base64 32`) and never leave
   it. **Never re-create them once Postgres has started** — it read them into its volume.
2. **Registry login** as `tim` (see "Pulling the image"): `ssh tim@api.polleninfo.ch`, then
   `docker login ghcr.io -u timstenzel --password-stdin`.
3. **DNS** resolves on every name server (above).
4. **Deploy** a tag CI published (below). The first start creates the database, runs `V1` and
   obtains the certificate; then `https://api.polleninfo.ch/health` answers `OK`.

**`V1__init.sql` is frozen from the first production start on.** Every schema change is a new
migration; editing an applied one makes Flyway refuse to start.

### Deploying and rolling back

From a clean checkout on your machine, with the full commit SHA of a green `server.yml` run on
`main` (its package `polleninfo-server` on GitHub lists them):

```sh
deploy/deploy.sh <sha>
```

It copies `compose.yaml`, `caddy/`, `postgres/`, `install-secrets.sh` and `.env.example` to
`/opt/polleninfo/deploy`, sets `SERVER_TAG` in `.env` there (created from `.env.example` the first
time), runs `docker compose pull server && docker compose up -d --wait`, waits up to a minute for
`/health` and prints the tag it replaced. **Rollback is the same command with that tag** — mind that
it also copies this checkout's stack files, so roll those back in git first if they changed. Only the
server is replaced; Postgres and Caddy are recreated only when their entry in `compose.yaml`
changes. A changed `caddy/Caddyfile` or `postgres/pg_hba.conf` is a mounted file compose does not
compare, so restart that service afterwards:
`ssh tim@api.polleninfo.ch 'cd /opt/polleninfo/deploy && docker compose restart caddy'`. `POLLENINFO_SSH`
overrides the login (default `tim@api.polleninfo.ch`).

After a reboot (unattended upgrades, or `sudo reboot`) Docker starts on boot and every service is
`restart: unless-stopped`, so the API comes back without anything to do.

### Backups

There are no automatic backups yet. Dump by hand, in Postgres's custom format, and copy the file off
the server:

```sh
ssh tim@api.polleninfo.ch 'cd /opt/polleninfo/deploy && docker compose exec -T -u postgres postgres \
  pg_dump -Fc -d polleninfo' > polleninfo-$(date +%F).dump
```

Restore into an empty database with `pg_restore -d polleninfo` (as the superuser, inside the
container). The file holds the device table (token hashes and push addresses), so keep it private.

To look into the database from your machine, open an SSH session into the container's `psql` —
Postgres publishes no port, so there is nothing to forward a port to (see "Looking into the
database" for the logins):

```sh
ssh -t tim@api.polleninfo.ch 'cd /opt/polleninfo/deploy && docker compose exec -u postgres postgres sh -c \
  "PGPASSWORD=\$(cat /run/secrets/db_app_password) psql -U polleninfo_app -d polleninfo"'
```

### Only Caddy publishes ports

Never add `ports:` to `server` or `postgres`. Besides exposing them, Docker writes its own iptables
rules for a published port, **ahead of `ufw`** — a published 5432 would be open to the internet
whatever `ufw status` says. Only Infomaniak's firewall would still stand in the way.

### Before a public Play release

The app sends a device identifier and a push address to this server, so a public listing on Google
Play needs a privacy-policy URL (for example a page on `polleninfo.ch`, which this stack does not
serve). Internal and closed testing tracks do not.

## Try the production stack locally

The same `compose.yaml`, with `compose.local.yaml` on top: Caddy serves `localhost` with a
certificate from its own local CA, and the server runs the image built from this checkout. It runs
in production mode, so it needs a full set of secrets, written to the git-ignored
`deploy/local-secrets/`. Without a real Firebase key, `--fake-fcm-key` writes a well-formed one with
a throw-away RSA key: the server starts, and every push fails and is logged.

```sh
# Ports 80 and 443 must be free; the development database does not conflict with the stack, but
# stop it to check that nothing answers on 5432.
./gradlew :server:installDist
docker build -t polleninfo-server:local server

# The secrets, owned and moded as on the server. Run as root in a throw-away container, so no sudo
# is needed on macOS (Docker Desktop shows bind-mounted files to every uid, so the ownership is only
# enforced on Linux).
docker run --rm -u 0 -v "$PWD/deploy:/deploy" -w /deploy --entrypoint bash postgres:18.6 \
  ./install-secrets.sh --fake-fcm-key local-secrets

export SERVER_TAG=local SECRETS_DIR=./local-secrets
docker compose -f deploy/compose.yaml -f deploy/compose.local.yaml up -d --wait

# Trust Caddy's local CA for curl (or pass -k).
docker compose -f deploy/compose.yaml -f deploy/compose.local.yaml \
  cp caddy:/data/caddy/pki/authorities/local/root.crt /tmp/polleninfo-local-ca.crt
curl -i --cacert /tmp/polleninfo-local-ca.crt https://localhost/health   # OK, with the headers above
curl -i http://localhost/health                                         # 301 to https://localhost/health
```

Things worth checking there:

```sh
c() { docker compose -f deploy/compose.yaml -f deploy/compose.local.yaml "$@"; }
c ps                                          # ports on caddy only
c logs -f server                              # the server's log
c exec -u postgres postgres sh -c \
  'PGPASSWORD=$(cat /run/secrets/db_app_password) psql -U polleninfo_app -d polleninfo'
                                              # the app login: DML yes, DROP TABLE refused
c down                                        # stop; add -v to drop the database and certificates
```

A browser does not trust Caddy's local CA unless it is imported; the HSTS header then pins
`localhost` to HTTPS in that browser, so prefer `curl`.

## CI: `.github/workflows/server.yml`

Runs on pull requests and on pushes to `main` that touch `server/`, `gradle/`, the root Gradle
files, `deploy/`, `.github/actions/` or the workflow itself:

1. `./gradlew :server:test :server:installDist` (the database tests use Testcontainers on the
   runner's Docker),
2. `docker build`,
3. a Trivy scan that fails the run on any **critical** vulnerability with a fix available,
4. on `main` only, a separate job pushes the scanned image as
   `ghcr.io/timstenzel/polleninfo-server:<commit sha>` and `:latest`. The package stays private.

Rules every workflow in `.github/` follows:

- **One workflow per shipped artifact**, each with its own `paths` filter. The app pipelines will be
  `android.yml` (AAB → Play internal testing) and `ios.yml` (TestFlight), triggered by a release tag
  (`android-v…` / `ios-v…`; the server's would be `server-v…`) or by hand (`workflow_dispatch`),
  never on every push.
- **Shared setup** through the composite action `.github/actions/setup-jvm` (JDK 21, Gradle with
  caching and wrapper validation). `gradle/wrapper/gradle-wrapper.jar` is committed for it.
- **Least privilege.** The workflow default is `permissions: contents: read`; only a job that
  publishes gets more (`packages: write` for the server's publish job).
- **Every action is pinned by its full commit SHA**, with the version in a trailing comment.
  Dependabot (`.github/dependabot.yml`) updates the pins, the Gradle catalog and the image's base
  weekly.
- **No repository-wide secrets.** The server pipeline needs only the workflow's own
  `GITHUB_TOKEN`. Store credentials (Play service account, App Store Connect key) go into
  **GitHub Environments with required reviewers**, so only an approved run of the job that uploads
  can read them.
