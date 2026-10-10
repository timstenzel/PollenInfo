# Backend Deployment

## Problem Statement

PollenInfo's backend only runs on the developer's machine. The apps talk to it over plain HTTP on
emulator-only addresses, it keeps its data in a local SQLite file, and the only thing protecting an
install's alarms is an identifier that travels in every request's address — where it ends up in
logs — and is stored in readable form. Nothing about the server, the machine it would run on, or the
way new versions reach it is ready for real users.

The developer has bought the domain `polleninfo.ch` (DNS at Hostpoint) and a small virtual server at
Infomaniak (Ubuntu 26.04, 1 vCPU, 2 GB RAM, 20 GB disk) and wants the backend to run there for
production use: reachable under the domain over HTTPS, protected against abuse and casual attack,
storing its data in a proper PostgreSQL database that is itself locked down, and built and shipped
in a reproducible way. A later feature will add user accounts, so the way an install proves who it
is must be sound enough to build on.

## Solution

The backend runs on the Infomaniak server as a small set of containers: a web front door that
terminates HTTPS with automatically renewed certificates, the PollenInfo server, and a PostgreSQL
database that is never reachable from outside the machine. The API is served at
`https://api.polleninfo.ch`; release builds of the Android app use it, development builds keep
talking to a local backend unless the developer points them elsewhere.

An install now registers and receives a secret device token, which it sends in a request header
rather than in the address. The server keeps only a fingerprint of that token, so neither its logs
nor its database can be used to impersonate an install. When the server does not recognise the
token, the app registers again on its own, as it does today. An install can have all its data
erased, and installs that have gone silent and can no longer receive notifications are removed
automatically after 90 days.

The server limits how often a single client can register, call its device endpoints or read pollen
data, refuses oversized requests, never reveals internal error details, and in production refuses
to start unless every required setting and secret is present. Secrets live as protected files on the
server, never in the repository.

The machine itself is set up by one repeatable script and a written runbook: key-only SSH, a
firewall that admits only SSH and web traffic, brute-force protection and automatic security
updates. Every change to `main` is tested, packaged into a container image, scanned for known
vulnerabilities and published to a private registry; the developer then deploys a chosen version
with one command.

Developers keep a simple local setup: one command starts a local PostgreSQL, and the server runs
against it as before.

## User Stories

### App users

1. As an app user, I want the app to reach the PollenInfo backend over the internet, so that I can see pollen readings on my own phone and not only on an emulator.
2. As an app user, I want every connection between my app and the backend to be encrypted, so that nobody on the same network can read or alter what is exchanged.
3. As an app user, I want my alarms to keep working after the backend moves to its production home, so that I keep getting daily reports and threshold alerts.
4. As an app user, I want my install's secret to be impossible to read from server logs or the server's database, so that nobody with access to either can read or change my alarms.
5. As an app user, I want my install's secret not to be copied into my cloud backup or to a new phone, so that two phones never fight over one identity.
6. As an app user restoring my phone from a backup, I want the app to register afresh on its own, so that alarms keep working on the new phone without any action from me.
7. As an app user, I want the app to recover by itself when the backend no longer knows my install, so that I am never stuck with an alarms screen that only shows errors.
8. As an app user, I want my alarms to keep being delivered after the backend is restarted or updated, so that a deployment does not silently cost me a notification.
9. As an app user, I want the backend to stay responsive even when someone tries to flood it, so that my readings and alarms keep working.
10. As an app user, I want error messages in the app to stay the same friendly sentences, so that technical details never appear on my screen.
11. As an app user, I want information about my install to be deleted when it can no longer be used, so that the service does not keep data about me forever.
12. As an app user, I want my data to be stored in Switzerland on a server the operator controls, so that I can trust where it lives.

### Operator (the developer running the service)

13. As the operator, I want the API to be served at `https://api.polleninfo.ch`, so that the apps have one stable production address.
14. As the operator, I want HTTPS certificates to be obtained and renewed automatically, so that the service never goes down because a certificate expired.
15. As the operator, I want plain HTTP requests redirected to HTTPS and browsers told to insist on HTTPS, so that no client accidentally talks to the API unencrypted.
16. As the operator, I want the whole production setup described in versioned files, so that I can rebuild the server from scratch or move to another provider.
17. As the operator, I want one repeatable provisioning script for a fresh server, so that hardening is never a half-remembered list of manual steps.
18. As the operator, I want SSH to accept only key-based logins by a non-root user, so that password guessing and direct root access are impossible.
19. As the operator, I want a firewall that admits only SSH and web traffic, so that nothing else on the machine is reachable from the internet.
20. As the operator, I want repeated failed SSH logins to be blocked automatically, so that brute-force attempts stop on their own.
21. As the operator, I want security updates to install automatically and the machine to reboot at a quiet hour when needed, so that known holes are closed without my attention.
22. As the operator, I want the services to come back by themselves after a reboot, so that an update or a crash does not leave the API down.
23. As the operator, I want the database never to be reachable from outside the machine, so that it cannot be attacked directly.
24. As the operator, I want the running server to use a database login that can only read and write data, not change the schema, so that a bug in the server cannot destroy tables.
25. As the operator, I want schema changes to be versioned and applied automatically on start, so that every environment has exactly the same schema and future changes (such as accounts) are safe.
26. As the operator, I want all passwords and keys kept as protected files on the server and never in the repository, so that a leaked repository leaks no credentials.
27. As the operator, I want the server to refuse to start in production when any required setting or secret is missing, with a message naming every missing item, so that a misconfiguration is caught at once instead of failing silently later.
28. As the operator, I want the production server never to fall back to merely logging notifications, so that a missing push key cannot go unnoticed.
29. As the operator, I want registrations limited per client address, so that a script cannot fill the disk with fake installs.
30. As the operator, I want each install's calls and each client's pollen reads limited to a sensible rate, so that one client cannot exhaust a one-CPU server.
31. As the operator, I want clients over a limit to be told when they may try again, so that well-behaved clients back off correctly.
32. As the operator, I want the rate limits to see each caller's real address behind the HTTPS front door, without trusting addresses a client claims for itself, so that limits cannot be dodged or turned against other users.
33. As the operator, I want oversized requests rejected, so that nobody can tie up memory with huge bodies.
34. As the operator, I want unexpected errors to produce a generic answer while the details go to the server log, so that no stack trace, SQL or internal name ever reaches a client.
35. As the operator, I want logs that contain no device secrets, no full push addresses and no request bodies, so that log access does not grant access to users' installs.
36. As the operator, I want logs rotated with a size cap, so that they never fill the 20 GB disk.
37. As the operator, I want the server and the database to have memory limits suited to 2 GB, so that one cannot starve the other or the machine.
38. As the operator, I want every change on `main` tested and packaged as a container image automatically, so that what I deploy is always a tested build.
39. As the operator, I want each image scanned for known critical vulnerabilities before it is published, so that I do not ship a fixable hole.
40. As the operator, I want images published to a private registry tagged by commit, so that I can deploy or roll back to an exact version.
41. As the operator, I want to deploy a chosen version with a single command, so that shipping is deliberate and quick.
42. As the operator, I want a deployment to cause at most a few seconds of downtime, to let a running alarm check finish, and to deliver alarms whose minute fell into the restart a little late rather than not at all, so that users barely notice it.
43. As the operator, I want automated pull requests for outdated dependencies, base images and CI actions, so that the stack does not quietly age.
44. As the operator, I want a runbook covering DNS at Hostpoint, provisioning, secret generation, first deployment, routine deployment and a manual database dump, so that I can operate the service without relying on memory.
45. As the operator, I want the runbook to tell me that the DNS record must point at the server before the first start, so that certificate issuance does not fail and get rate-limited.
46. As the operator, I want installs that have lost their push address and have not been seen for 90 days removed automatically, so that the database does not accumulate unusable personal data.
47. As the operator, I want to be able to erase one install's data on request, so that I can honour a data-deletion request.
48. As the operator, I want the production stack to be tryable on my own machine, so that I can test deployment changes before they reach the server.

### Developer

49. As a developer, I want one command to start a local PostgreSQL for development, so that running the server locally stays as easy as before.
50. As a developer, I want the local database to use the same schema migrations and the same roles as production, so that local behaviour matches production.
51. As a developer, I want to reset my local database with one command, so that I can start from a clean backend.
52. As a developer, I want the server's database tests to run against the same PostgreSQL version as production, so that tests catch what production would hit.
53. As a developer, I want tests that do not touch the database to keep running without one, so that most of the test suite stays fast.
54. As a developer, I want debug app builds to use the local backend by default, so that development never touches production data by accident.
55. As a developer, I want to point a debug build at the production backend with one setting, so that I can test on a real phone.
56. As a developer, I want release app builds to use only the production HTTPS address, so that a release can never talk to a development server or over plain HTTP.
57. As a developer, I want the device credential flow to leave room for user accounts, so that the account feature can add sign-in without reworking device identity.
58. As a developer, I want the project documentation updated to the new commands, API and storage, so that the next feature starts from accurate instructions.

## User Acceptance Tests

### Reachability and HTTPS

1. Given the DNS record for `api.polleninfo.ch` points at the server and the stack is running, when a client requests `https://api.polleninfo.ch/health`, then it receives `OK` over a valid, publicly trusted certificate.
2. Given the stack is running, when a client requests `http://api.polleninfo.ch/health`, then it is redirected to the HTTPS address.
3. Given the stack is running, when a client requests any HTTPS address of the API, then the response tells browsers to use HTTPS only and does not name the server software.
4. Given the server's public address, when someone scans it from the internet, then only SSH and the web ports answer; the database port and the server's internal port do not.
5. Given a release build of the Android app on a real phone with mobile data, when the user opens Home, then the station's current reading loads from the production backend.

### Device registration and authentication

6. Given a fresh install of the release app, when the user opens the Alarms tab and allows notifications, then the install registers and an empty alarm list is shown.
7. Given a registered install, when the user creates, edits, pauses and deletes an alarm, then each action succeeds as before.
8. Given a registered install, when the operator inspects the server logs and the database, then the install's device token appears in neither — the database holds only a fingerprint of it.
9. Given a request to an install's alarm list without a device token or with an unknown one, when it reaches the server, then the server answers that the caller is not authenticated.
10. Given an install whose device record has been deleted on the server, when the user next opens the Alarms tab, then the app registers again on its own and shows an empty list without an error.
11. Given two registered installs, when one requests, changes or deletes an alarm that belongs to the other, then the server answers as if the alarm did not exist, and the other install's alarm is unchanged.
12. Given a registered install, when it requests deletion of its own data, then its device record, its alarms and their notification history are removed, and its next alarm call leads to a fresh registration.
13. Given a phone restored from a cloud backup of a phone that had registered, when the user opens the Alarms tab on the restored phone, then the app registers as a new install rather than reusing the old phone's identity.
14. Given a registered install, when Firebase hands the app a new push address, then the server stores the new address and later notifications reach the install.

### Abuse protection and error handling

15. Given a single client address, when it attempts more than five registrations within an hour, then further attempts are refused with a "too many requests" answer that says when to retry.
16. Given a single install, when it makes more than 60 device calls in a minute, then further calls are refused with a "too many requests" answer that says when to retry.
17. Given a single client address, when it opens the All stations tab (fifteen parallel readings) several times in a row, then no reading is refused.
18. Given a single client address, when it makes more than 120 pollen requests in a minute, then further requests are refused with a "too many requests" answer that says when to retry.
19. Given a request that claims a different client address in a forwarding header, when it reaches the server through the front door, then the rate limit is applied to the real address, not the claimed one.
20. Given a request with a body larger than the allowed size, when it reaches the server, then it is refused without being processed.
21. Given an unexpected internal failure while handling a request, when the client receives the answer, then it is a generic error with no stack trace, SQL or internal detail, and the details appear in the server log.
22. Given a browser page on another site, when it tries to call the API from script, then the browser refuses, because the API grants no cross-site access.

### Configuration and secrets

23. Given the server is started in production mode with the push key and both database passwords missing, when it starts, then it refuses to start and its error names all missing items.
24. Given the server is started in production mode with every required secret present, when it starts, then it applies pending schema migrations and serves requests.
25. Given the repository, when it is searched for passwords, private keys or push credentials, then none are found; only placeholder names for the secret files exist.
26. Given the running stack, when the operator inspects the server container's environment, then no password or key appears in it.

### Database

27. Given the running stack, when the operator connects to the database as the server's login, then reading and writing data works but creating, altering or dropping a table is refused.
28. Given an empty database, when the server starts for the first time, then the full schema is created by the versioned migrations.
29. Given a database already at the latest schema version, when the server restarts, then no migration runs again and all data is intact.
30. Given an install at nine alarms, when two new alarms are created at the same moment, then exactly one succeeds and the other is refused for the alarm limit.

### Delivery and cleanup

31. Given an alarm due in the next minute, when the operator deploys a new version shortly before that minute, then the notification is still delivered once the new version runs (at most two minutes late), or was already delivered by the old one — never twice. Given instead that the server was down for longer than two minutes, when it starts again, then no daily report from the missed minutes is sent late.
32. Given a threshold alert that already notified about a pollen type today, when the server is restarted, then that pollen type is not notified again the same day.
33. Given an install whose push address was reported unregistered and which has made no call for more than 90 days, when the first alarm check of a new Swiss day runs, then the install and its alarms are deleted.
34. Given an install with a valid push address that has made no call for more than 90 days, when the daily cleanup runs, then it is kept.
35. Given an install whose push address was reported unregistered but which made a call within the last 90 days, when the daily cleanup runs, then it is kept.

### Server operation

36. Given a fresh Ubuntu 26.04 server, when the operator runs the provisioning script as described in the runbook, then SSH rejects password and root logins, the firewall admits only SSH and web traffic, brute-force protection and automatic security updates are active, and Docker is installed.
37. Given an already provisioned server, when the provisioning script is run again, then it completes without errors and changes nothing that is already in place.
38. Given the provisioned server, when it reboots, then the API is reachable again without manual action.
39. Given a commit pushed to `main`, when the pipeline finishes, then the server tests have passed, an image tagged with the commit has been published to the private registry, and it was scanned for critical vulnerabilities.
40. Given an image with a critical vulnerability that has a fix available, when the pipeline scans it, then the pipeline fails and the image is not published.
41. Given a published image tag, when the operator runs the deploy command with that tag, then the server runs that version within a minute and the previous version can be restored the same way.
42. Given the stack has run for a long time, when the operator checks the disk, then container logs have stayed within their size cap.
43. Given the operator's own machine with Docker, when they follow the runbook section for trying the production stack locally, then the API answers over HTTPS on `localhost`.

### Developer setup

44. Given a fresh checkout with Docker running, when a developer starts the development database with the documented command and then runs the server, then the server starts against it without any further configuration.
45. Given a development database with data, when the developer runs the documented reset command, then the next start begins with an empty schema.
46. Given Docker running, when the developer runs the server tests, then the database tests run against the production PostgreSQL version and pass.
47. Given a debug build with no override, when it starts in the emulator, then it talks to the local development backend.
48. Given a debug build with the production override set, when it starts on a real phone, then it talks to `https://api.polleninfo.ch`.
49. Given a release build, when its network configuration is inspected, then it allows no plain HTTP and contains only the production address.

## Definition of Done

- All user acceptance tests pass.
- All existing app and server automated tests pass, adapted where the API changed, and the new automated tests described in the annex exist and pass.
- The iOS sources still compile.
- The translation check still passes.
- No new build deprecation or compiler warning is introduced.
- The production stack runs on the Infomaniak server and the API answers at `https://api.polleninfo.ch`.
- A release build of the Android app works end to end against production: readings, history, alarm creation and a delivered push notification.
- No secret of any kind is committed to the repository.
- The database is not reachable from outside the server.
- The CI pipeline is green on `main` and publishes a scanned image.
- Automated dependency update pull requests are configured.
- The runbook covers DNS, provisioning, secrets, first and routine deployment, rollback, local trial of the production stack, the manual database dump, and the privacy-policy requirement for a public Play release.
- The project documentation reflects the new commands, REST API, persistence, configuration and app base address.
- SQLite is no longer used anywhere in the server.

## Out of Scope

- **Automated backups.** Accepted risk: losing the server or its database volume loses all alarms; installs re-register by themselves, but users must recreate their alarms. Backups should come before or together with the account feature. Only a manual dump command is documented.
- **Monitoring, alerting and metrics.** No uptime monitor, no readiness endpoint, no dashboards.
- **App integrity checks** (Firebase App Check / Play Integrity) — a separate later feature.
- **User accounts**, sign-in, and linking installs to people.
- **The Diary's storage and backup behaviour** — handled in the account feature.
- **A privacy-policy page** and anything served on `polleninfo.ch` or `www.polleninfo.ch`; only noted in the runbook.
- **A user interface** for deleting one's own data.
- **Migrating existing SQLite data** — nothing is live.
- **Certificate pinning** in the app.
- **Zero-downtime (blue/green) deployment** and **automatic deployment** from CI.
- **A staging environment.**
- **Managed database services** or configuration-management tools such as Ansible.
- **Encrypting the device token on the phone** beyond the app's private storage.
- **An iOS app project** — iOS only receives its base address from its entry point.
- **HSTS preloading.**

## Further Notes

- **DNS precondition.** `api.polleninfo.ch` currently resolves to `217.26.48.101` — Hostpoint's
  default address, probably via a wildcard record. Before the first start an `A` record (and an
  `AAAA` record only if the server really serves IPv6) for `api` must be created at Hostpoint and
  verified to resolve to the server. Otherwise the certificate authority's validation fails, and
  repeated failures are rate-limited. An entry in Infomaniak's panel does not affect this; it is at
  most reverse DNS, which nothing here needs.
- **Starting values for rate limits** are 5 registrations per hour per address, 60 device calls per
  minute per install and 120 pollen requests per minute per address. All are configurable.
- **Clean cut.** There are no live users, so the API change is not backwards compatible and the app
  needs no shim for the old identifier: an old install simply registers anew.
- **Docker is now required for the server's database tests** and for local development. Any of
  Docker Desktop, OrbStack or Colima works on macOS.
- A **public Play release** additionally needs a privacy-policy URL, because the app sends a device
  identifier to the backend. That is outside this feature.

---

## Technical Annex
> Written against codebase as of: 2026-10-10 (branch `feature/backend-refactoring`, after `bf49902`)

### Architectural Decisions

#### Runtime topology (`deploy/compose.yaml`)

- Three services on one internal bridge network:
  - **`caddy`** — official `caddy:2` image (pinned minor). The **only** service with published ports: `80/tcp`, `443/tcp`, `443/udp`.
  - **`server`** — `ghcr.io/timstenzel/polleninfo-server:<tag>`, listening on `0.0.0.0:8080` inside the network only.
  - **`postgres`** — `postgres:18.x` (pinned minor, the **same tag** as the Testcontainers tests), with a named volume `pgdata`. No published port.
- Every service has:
  - `restart: unless-stopped`
  - `security_opt: [no-new-privileges:true]`
  - a memory limit — around 700 MB for `server`, 512 MB for `postgres`, 128 MB for `caddy`
  - the `json-file` log driver with `max-size` / `max-file`
- `server` also has `read_only: true` with a `tmpfs` for `/tmp`, runs as a non-root user, and uses
  `depends_on: postgres: condition: service_healthy`. Postgres's healthcheck is `pg_isready`.
- **JVM:** `-Xmx512m` (or `-XX:MaxRAMPercentage`), `-XX:+ExitOnOutOfMemoryError`.
- **Postgres tuning:** `shared_buffers` 128 MB, `max_connections` 20, `password_encryption = scram-sha-256`;
  `pg_hba` allows only `scram-sha-256` from the compose network.
- Secrets are compose `secrets:` mapped from `/opt/polleninfo/secrets/*` (root, `600`) into
  `/run/secrets/*`:
  - `postgres_superuser_password`
  - `db_owner_password`
  - `db_app_password`
  - `fcm_credentials.json`

  The GHCR read token is used by `docker login` on the host only and never reaches a container.
  Compose (non-Swarm) bind-mounts secret files without applying `uid`/`mode`, so each file is owned
  by the uid that reads it, mode `0400`: the Postgres files by the image's `postgres` uid (999), the
  server files by the server image's fixed uid. `provision.sh` and the runbook set this.
- **`Caddyfile`:**
  - `api.polleninfo.ch { reverse_proxy server:8080 }`
  - headers: `Strict-Transport-Security "max-age=31536000; includeSubDomains"`,
    `X-Content-Type-Options nosniff`, `Referrer-Policy no-referrer`, `-Server`
  - `request_body max_size 16KB`
  - read and write timeouts
  - no access log (Ktor's `CallLogging` covers requests)
  - Caddy sets `X-Forwarded-For` / `X-Forwarded-Proto` itself; client-supplied values are discarded
    (`trusted_proxies` not configured)
- **`deploy/compose.local.yaml`** (override): uses the `localhost` site block, so Caddy issues itself
  a local certificate, and builds or uses a local image.

#### Development setup (`deploy/compose.dev.yaml`)

- `postgres` only, published on `127.0.0.1:5432`, with committed **development-only** passwords. It
  mounts the same `deploy/postgres/init/` role script as production.
- Reset: `docker compose -f deploy/compose.dev.yaml down -v`.
- `./gradlew :server:run` uses development defaults:
  - `POLLENINFO_ENV=development`
  - `jdbc:postgresql://localhost:5432/polleninfo`
  - the development owner and app passwords
- `server/data/` and the `POLLENINFO_DB` variable are removed.

#### Database roles and migrations

- **`deploy/postgres/init/01-roles.sh`** runs once at volume creation as the superuser:
  - creates database `polleninfo`
  - creates role `polleninfo_owner` (owner of the database and schema `public`) and role
    `polleninfo_app` (LOGIN, no CREATE on schema)
  - `REVOKE CREATE ON SCHEMA public FROM PUBLIC`
  - `ALTER DEFAULT PRIVILEGES FOR ROLE polleninfo_owner GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO polleninfo_app`
    (plus `USAGE` on sequences)

  It reads the passwords from the `*_FILE` secrets.
- **Flyway** (`org.flywaydb:flyway-core` + `flyway-database-postgresql`) runs on startup **as
  `polleninfo_owner`** through its own short-lived connection, before the Hikari pool for
  `polleninfo_app` is opened. Migrations live in `server/src/main/resources/db/migration/`.
  `SchemaUtils.create` is removed.
- **`V1__init.sql`:**

```sql
CREATE TABLE devices (
    id            uuid        PRIMARY KEY,
    token_hash    bytea       NOT NULL UNIQUE,      -- SHA-256 of the device token
    fcm_token     text,                             -- NULL once FCM reported it unregistered
    created_at    timestamptz NOT NULL,
    last_seen_at  timestamptz NOT NULL
);
CREATE TABLE alarms (
    id            uuid        PRIMARY KEY,
    device_id     uuid        NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    enabled       boolean     NOT NULL,
    station_abbr  varchar(3)  NOT NULL,
    species       text[]      NOT NULL,
    min_severity  varchar(16) NOT NULL,
    days          text[]      NOT NULL,
    type          varchar(16) NOT NULL,             -- 'daily' | 'threshold'
    at_time       time,
    from_time     time,
    until_time    time,
    created_at    timestamptz NOT NULL
);
CREATE INDEX alarms_device_created ON alarms(device_id, created_at);
CREATE TABLE notification_log (
    alarm_id      uuid        NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
    species       varchar(16) NOT NULL,
    local_date    date        NOT NULL,
    PRIMARY KEY (alarm_id, species, local_date)
);
```

  `alarms.device_id` now cascades too, so deleting a device removes its alarms and their log.
  `created_at` stays strictly increasing per device, as today. `devices` deliberately has no
  `user_id` yet; the account feature adds a nullable one in its own migration.
- The **alarm-limit race** (10 per device): the count and the insert run in one transaction that
  locks the device row (`SELECT … FOR UPDATE` on `devices`). Postgres's default `READ COMMITTED`
  alone would let two concurrent creates at nine both pass. Default isolation stays `READ COMMITTED`.
- **Exposed 1.5.1** with the `exposed-java-time` module for `timestamp`/`time`/`date` columns (or the
  `kotlin.time`/datetime equivalent already compatible with the server), and `array<String>` for
  `text[]`.
- Dependencies: `org.postgresql:postgresql`, `com.zaxxer:HikariCP`.
- `sqlite-jdbc` is removed from the catalog. `LegacyDatabaseCompatibilityTest` and
  `server/src/test/resources/fixtures/db/` are deleted.

#### `ServerConfig` (new, `server/.../config/ServerConfig.kt`) — deep, pure

```kotlin
data class ServerConfig(
    val environment: Environment,           // PRODUCTION | DEVELOPMENT
    val port: Int,                          // PORT, default 8080
    val database: DatabaseConfig,           // url, ownerUser, ownerPassword, appUser, appPassword
    val fcmCredentialsPath: Path?,          // FCM_CREDENTIALS; required in PRODUCTION
    val rateLimits: RateLimits,             // registrations/hour/IP, device calls/min/token, pollen/min/IP
    val trustedProxy: Boolean,              // honour X-Forwarded-* (true in PRODUCTION)
) {
    companion object {
        /** Throws ConfigException listing *every* problem at once. */
        fun load(env: Map<String, String>, readFile: (Path) -> String?): ServerConfig
    }
}
```

- Variables:
  - `POLLENINFO_ENV`
  - `PORT`
  - `DB_URL`
  - `DB_OWNER_USER`, `DB_OWNER_PASSWORD_FILE`
  - `DB_APP_USER`, `DB_APP_PASSWORD_FILE`
  - `FCM_CREDENTIALS`
  - `RATE_LIMIT_REGISTER_PER_HOUR`, `RATE_LIMIT_DEVICE_PER_MINUTE`, `RATE_LIMIT_POLLEN_PER_MINUTE`
- **`DEVELOPMENT`**: every value has a local default matching `compose.dev.yaml`; a missing
  `FCM_CREDENTIALS` gives `LoggingPushSender`, as today.
- **`PRODUCTION`**: database URL, both password files and `FCM_CREDENTIALS` are mandatory and must
  be readable; there is no `LoggingPushSender` fallback.
- `pushSenderFromEnvironment()` becomes `pushSender(config)`.
- `main()` reads `ServerConfig.load(System.getenv(), …)` and passes it to `embeddedServer(port = config.port)` and
  `Application.module(config)`. Shutdown: Netty's `shutdownGracePeriod` / `shutdownTimeout`, the
  scheduler loop finishes its current `tick()` before cancelling, and the Hikari pool closes on
  `ApplicationStopped`.

#### Device identity (`alarm/domain/DeviceToken.kt`, replaces `DeviceIds.kt`) — deep, pure

```kotlin
@JvmInline value class DeviceToken(val value: String)        // 32 random bytes, base64url no padding (43 chars)
@JvmInline value class DeviceId(val value: UUID)             // internal key, never sent to the app
fun newDeviceToken(random: SecureRandom = secureRandom): DeviceToken
fun DeviceToken.hash(): ByteArray                            // SHA-256 of the UTF-8 value
```

- A fast hash is deliberate: the token is 256 bits of random data, not a password. Lookup is by
  `token_hash` (unique index), so comparison happens in the database and nothing is compared in a
  timing-sensitive way in Kotlin.

#### `DeviceStore` (reshaped)

```kotlin
interface DeviceStore {
    suspend fun register(fcmToken: String): DeviceToken          // inserts id, hash, fcm, created_at = last_seen_at = now
    suspend fun authenticate(token: DeviceToken): DeviceId?      // null if unknown; touches last_seen_at if older than 1 day
    suspend fun updateFcmToken(id: DeviceId, fcmToken: String): Boolean
    suspend fun clearFcmToken(id: DeviceId, fcmToken: String)    // conditional, as today's clearToken
    suspend fun delete(id: DeviceId): Boolean                    // cascades to alarms and notification_log
    suspend fun pruneInactive(now: Instant): Int                 // fcm_token IS NULL AND last_seen_at < now - 90 days
}
```

- `exists` is dropped; authentication replaces it. `AlarmStore` keeps its methods with the UUID
  `DeviceId`.
- Because authentication has already established the device, `list` no longer needs a `null`
  "unknown device" result, and `CreateResult.UnknownDevice` disappears, since a device can only
  vanish between authentication and use through its own `DELETE /devices/me`.
- `AlarmWithToken` and `enabledWithDeliverableDevice()` are unchanged in meaning.
- `INACTIVE_DEVICE_RETENTION = 90.days` and `LAST_SEEN_RESOLUTION = 1.days` are constants in `alarm/store`.

#### Authentication and routes

- `plugins/Authentication.kt`: `install(Authentication) { bearer("device") { authenticate { cred -> devices.authenticate(DeviceToken(cred.token))?.let(::DevicePrincipal) } } }`.
  A missing, malformed or unknown token is **`401`** with `WWW-Authenticate: Bearer`.
- `alarmRoutes(devices, alarms)`:

| Method | Path | Auth | Result |
| --- | --- | --- | --- |
| POST | `/devices` | — | `{fcmToken}` → `201 {deviceToken}`; `400 {error}` missing/blank |
| DELETE | `/devices/me` | device | `204` |
| PUT | `/devices/me/fcm-token` | device | `{fcmToken}` → `204`; `400 {error}` |
| GET | `/devices/me/alarms` | device | `200 [Alarm]` in creation order |
| POST | `/devices/me/alarms` | device | `201 Alarm`; `400 {error}`; `409 {error}` at 10 |
| PUT | `/devices/me/alarms/{alarmId}` | device | `200 Alarm`; `400 {error}`; `404` unknown or another device's alarm |
| DELETE | `/devices/me/alarms/{alarmId}` | device | `204`; `404` as for `PUT` |

- **Ordering:** authentication runs **before** body validation, so an invalid body from an
  unauthenticated caller is a `401`, not a `400` (today validation came first). Within an
  authenticated call, validation still precedes the store lookup.
- An `alarmId` that is not a UUID is a `404`, not a `400`, so it reveals nothing.
- DTO change: `RegisterDeviceResponse(deviceToken: String)`; `UpdateTokenRequest` is unchanged in shape.

#### HTTP hardening (`plugins/Security.kt` or split per plugin)

- **`XForwardedHeaders`** installed only when `config.trustedProxy` is set. Caddy is the only peer
  that can reach the server, so the last hop is trusted.
- **`RateLimit`** (ktor-server-rate-limit) with three named limiters:
  - `register`: per `call.request.origin.remoteAddress`
  - `device`: keyed on the hash of the bearer token, falling back to the address
  - `pollen`: per address

  Each wraps its routes (`rateLimit(RateLimitName("pollen")) { pollenRoutes(…) }`), and `/health`
  is unlimited. A limited call gets `429` with `Retry-After`; `429` bodies are `{error}`.
- **Body size:** a size check rejecting `Content-Length` > 16 KB with `413` (Caddy enforces the same
  limit in front).
- **`StatusPages`:** `exception<Throwable>` → log with call context → `500 {"error":"internal error"}`.
  `BadRequestException` handling for malformed bodies stays in the routes, as today.
- **`CallLogging`:** the logged format excludes `Authorization` and bodies, and paths no longer
  contain identifiers. `LoggingPushSender` keeps logging only the push address's last six
  characters.
- No `CORS` plugin.

#### Scheduler

- **Short catch-up after a restart** (decided after the planning session, replacing the strict
  "no catch-up" rule for the restart case): the scheduler persists the last Swiss minute it
  processed (a one-row `scheduler_state(last_minute timestamptz)` table, in V1). On start, every
  minute after the stored one that is at most `MAX_CATCH_UP = 2 minutes` old is processed in order
  before the normal loop; older minutes are skipped. The stored minute is written once a minute's
  sends are done, so a minute is never processed twice across a restart. Threshold alerts are
  unaffected (their window is re-evaluated anyway and the notification log prevents repeats). A
  longer outage is still never replayed.
- `AlarmScheduler.tick()`'s first-tick-of-the-Swiss-day step runs `NotificationLog.pruneBefore(today)`
  **and** `DeviceStore.pruneInactive(now)`. Each has its own `try`, a failure is retried next tick,
  and neither costs that minute's alarms, as today's prune does.
- `dropToken` calls `clearFcmToken`.

#### App (`:composeApp`)

- **`AlarmApiService(client, baseUrl)`:**
  - every device call adds `bearerAuth(token)` and uses `/devices/me/...`
  - `401` → `UnknownDeviceException`
  - `404` on a single alarm's path → `AlarmNotFoundException` directly (the `checkedForAlarm`
    list probe is deleted)
  - `400` / `409` mapping unchanged
  - `registerDevice(fcmToken)` returns `RegisterDeviceResponseDto(deviceToken)`
  - `updateToken` → `PUT /devices/me/fcm-token`
- `deleteDevice()` is added only on the server side; the app gets no call for it in this feature.
- **`AlarmRepositoryImpl`:** the `withDevice {}` / `Mutex` / register-once-retry-once logic is
  unchanged, but now triggered by `UnknownDeviceException` from a `401`. `updateToken` on `401`
  clears the stored token if it is still the one used, as today on `404`.
- **`DeviceRegistrationRepository`:** `Flow<String?>` / `store` / `clear` now holds the device
  token (field and key renamed `deviceToken`). `DataStoreDeviceRegistrationRepository` is backed by
  a **second** `DataStore<Preferences>` file, `polleninfo_device`, provided by `platformModule` under
  a Koin qualifier on both platforms. It stays logic-free and untested.
- **Base URL:**
  - `:androidApp` enables `buildFeatures.buildConfig` and sets `buildConfigField("String", "API_BASE_URL", …)`:
    - release: `"https://api.polleninfo.ch"`
    - debug: `providers.gradleProperty("polleninfo.apiBaseUrl")` or `"http://10.0.2.2:8080"`
  - `PollenInfoApplication` passes it into Koin, e.g. `modules(appModules(apiBaseUrl = BuildConfig.API_BASE_URL))`
    or a `single(named("apiBaseUrl"))` / `ApiConfig(baseUrl)` single.
  - `MainViewController` passes `"http://localhost:8080"`.
  - `ApiConfig.kt`'s `expect val apiBaseUrl` and both actuals are deleted. `appModules` must stay
    usable from both entry points.
- **Backup exclusion** (`:androidApp`):
  - `res/xml/data_extraction_rules.xml` (API 31+): `<cloud-backup>` and `<device-transfer>` each
    `<exclude domain="file" path="datastore/polleninfo_device.preferences_pb"/>`
  - `res/xml/backup_rules.xml` (`fullBackupContent`, ≤ API 30): the same exclusion
  - both referenced from the manifest
  - the main `polleninfo_preferences` file, with the Diary, is left as it is

#### Container image (`server/Dockerfile` or `deploy/server.Dockerfile`)

- **Not a multi-stage Gradle build** (changed after review): `settings.gradle.kts` includes the
  Android modules, which AGP cannot configure without an Android SDK. The distribution is built by
  `./gradlew :server:installDist` on the CI runner (or the developer's machine), and the Dockerfile
  only copies `server/build/install/server` into a pinned `eclipse-temurin:21-jre` with a fixed
  non-root uid, `EXPOSE 8080`, and the JVM flags via `JAVA_OPTS`.
- `gradle-wrapper.jar` is committed (and checked by Gradle's wrapper-validation action) so CI can
  run `./gradlew` from a clean checkout.
- The image contains no secrets and no test fixtures.

#### CI/CD (`.github/workflows/server.yml`, `.github/actions/setup-jvm/`, `.github/dependabot.yml`)

- **Structured for later app pipelines** (decided after the planning session, so that Android AAB →
  Play internal testing and iOS → TestFlight workflows can be added as a separate feature without
  rework):
  - **One workflow per shipped artifact.** This feature adds only `server.yml`; `android.yml` and
    `ios.yml` come later. Each workflow has `paths` filters, so a change touching only one target
    does not run another. `server.yml` triggers on `server/**`, `gradle/**`, the root build files,
    `deploy/**` and its own workflow file.
  - **A shared composite action** (`.github/actions/setup-jvm`) sets up JDK 21 and Gradle with
    `gradle/actions/setup-gradle`, which provides caching and wrapper validation. Every workflow
    uses it.
  - **Least privilege.** The workflow default is `permissions: contents: read`. Only the publish
    job gets `packages: write`, and it runs only on `main`. Every action is pinned by commit SHA.
    Credentials that must not reach ordinary runs (later the Play and App Store keys; the server
    pipeline needs none beyond `GITHUB_TOKEN`) belong in **GitHub Environments** with required
    reviewers, never in repository-wide secrets.
  - App uploads will trigger on a release tag or a manual run (`workflow_dispatch`), never on every
    push. Tags are prefixed per target (`server-v…`, `android-v…`, `ios-v…`) so the three release
    independently.

- **Trigger:** push to `main` and pull requests, both filtered by the paths above; publishing only on `main`.
- **Steps:**
  1. JDK 21
  2. `./gradlew :server:test` (Testcontainers on the Ubuntu runner)
  3. Docker build
  4. Trivy scan: `severity: CRITICAL`, `ignore-unfixed: true`, `exit-code: 1`
  5. push to `ghcr.io/timstenzel/polleninfo-server:{sha}` and `:latest`, using the workflow's
     `GITHUB_TOKEN` with `packages: write`
- The package stays private.
- **Dependabot:** `gradle` (root), `docker` (Dockerfile and `deploy/`), `github-actions`, weekly.

#### Operations scripts (`deploy/`)

- **`provision.sh`** (idempotent, run as root once):
  - creates the sudo user with an authorized key
  - `sshd_config.d` drop-in: `PermitRootLogin no`, `PasswordAuthentication no`,
    `KbdInteractiveAuthentication no`
  - `ufw`: allow OpenSSH, 80/tcp, 443/tcp, 443/udp, default deny
  - `fail2ban` with the sshd jail
  - `unattended-upgrades` with `Automatic-Reboot "true"` at `04:00`
  - Docker Engine and the Compose plugin from Docker's apt repository
  - `/etc/docker/daemon.json` log defaults
  - a 2 GB swap file
  - `/opt/polleninfo/{secrets,deploy}` with the correct ownership
- **`deploy.sh <tag>`:** over SSH, sets `SERVER_TAG=<tag>` in `/opt/polleninfo/deploy/.env`, then
  runs `docker compose pull server && docker compose up -d`. Rollback is the same command with the
  previous tag.
- **`README.md` runbook:**
  - Hostpoint DNS
  - Infomaniak firewall (if offered)
  - provisioning
  - secret generation (`openssl rand -base64 32`)
  - the FCM key upload
  - `docker login ghcr.io` with a `read:packages` token
  - first start (verify DNS first)
  - routine deploy and rollback
  - manual `pg_dump -Fc` via `docker compose exec`
  - SSH-tunnel `psql` access
  - trying the stack locally
  - privacy-policy note
  - the rule that only `caddy` publishes ports

#### Documentation

- **`CLAUDE.md`:**
  - commands table (dev database, Docker requirement for `:server:test`)
  - Persistence (Postgres, roles, Flyway, types)
  - REST API (`/devices/me…`, `401`, `DELETE /devices/me`)
  - Devices and alarms (token hashed, last seen, cleanup)
  - "Talking to our own backend" (BuildConfig base address, release HTTPS)
  - "Alarms" (401-driven re-registration, list probe removed)
  - iOS wrapper configuration (base URL)
  - removal of SQLite and `LegacyDatabaseCompatibilityTest`
  - a new "Deployment" section pointing to `deploy/README.md`

### Automated Testing Decisions

**What makes a good test here:** it exercises a module through its public interface and asserts
observable behaviour — HTTP status and body, rows visible through the store interface, a decision
returned — never private helpers or SQL text. Time-dependent behaviour is driven by an injected
`java.time.Clock` (`MutableClock`), never by sleeping. Boundaries are pinned from both sides, as
`SpeciesThresholdsTest` does: 90 days exactly vs. one millisecond more, the rate limit at N and N+1,
`last_seen_at` refreshed at one day and not before.

| Module | Type | What is tested | Prior art |
| --- | --- | --- | --- |
| `ServerConfig.load` | Unit | Development defaults; production with everything present; production missing several items fails with **all** of them listed; unreadable file; malformed port and limits | — (new); style of `AlarmValidationTest` |
| `DeviceToken` | Unit | Token length and alphabet; two tokens differ; hash deterministic and 32 bytes; different tokens give different hashes | `DeviceIds` tests, if any, else new |
| Flyway + `PollenInfoDatabase` | Integration (Testcontainers, `postgres:18.x`) | Migrations apply to an empty database; re-running is a no-op; the **app role cannot** `CREATE`/`DROP`/`ALTER`; the app role can read and write | `LegacyDatabaseCompatibilityTest` (removed) for shape |
| `ExposedDeviceStore` / `ExposedAlarmStore` / `ExposedNotificationLog` | Integration (Testcontainers) | All current `ExposedStoresTest` cases ported, plus: register → authenticate round trip; unknown token → `null`; `last_seen_at` touched only after a day; `delete` cascades to alarms and log; `pruneInactive` boundaries (null FCM and >90 days deleted; valid FCM kept; recent kept); **concurrent creates at nine → exactly one succeeds**; arrays and times round-trip | `ExposedStoresTest`, `AlarmFixtures.kt` |
| `alarmRoutes` + device authentication | Route tests (`testApplication`, fake or Testcontainers stores) | Every row of the route table; `401` without, with a malformed and with an unknown token; another device's alarm `404`; non-UUID `alarmId` `404`; `401` precedes `400`; `DELETE /devices/me` then `401` | `AlarmRoutesTest` |
| Rate limiting, forwarded headers, body size, `StatusPages` | Route tests | Limit N passes and N+1 gets `429` with `Retry-After` per limiter; keys are independent (two addresses, two tokens); `X-Forwarded-For` honoured only with `trustedProxy`; `413` above 16 KB; a throwing route gives a generic `500` with no exception text | `PollenRoutesTest`, `RoutingTest` |
| `AlarmScheduler` | Integration (Testcontainers + fakes) | All existing cases on Postgres; first tick of a Swiss day prunes inactive devices; a failing prune does not cost the minute's alarms; `Unregistered` clears the FCM token and the device becomes prunable after 90 days | `AlarmSchedulerTest` |
| `AlarmApiService` / `AlarmRepositoryImpl` (app) | Unit through a real `HttpClient` + `MockEngine` | Bearer header present on every device call; `/devices/me` paths; `401` → re-register once and retry once for every call; a second `401` → `Failure`; single-alarm `404` → `AlarmNotFoundException` **without** re-registering and without a list request; `updateToken` `401` clears only the token used; registration stores the returned `deviceToken` | `AlarmRepositoryImplTest` |

- Testcontainers runs **one** Postgres container per test JVM (a shared singleton), with each test
  class starting from a clean schema via Flyway `clean` + `migrate` (clean enabled only in tests) or
  truncation. The image tag comes from one constant shared with `compose.yaml`'s documented version.
- Tests that need no database — `PollenRoutesTest`, the parser, cache, history and rules tests — keep
  running without Docker. `configureRouting`'s defaults no longer create a database; routes that
  need stores take them explicitly, and the pollen-only route tests install only the pollen routes.
- **Not automated, checked by hand or by CI itself:**
  - `DataStoreDeviceRegistrationRepository` (logic-free)
  - the backup-rules XML
  - BuildConfig base-URL wiring
  - Dockerfile, compose files and Caddyfile
  - `provision.sh` / `deploy.sh`
  - the GitHub workflow
  - Dependabot
- Unchanged rules apply: app tests in `commonTest` only; backtick test names without commas; iOS test
  sources compiled after touching `commonMain`.
