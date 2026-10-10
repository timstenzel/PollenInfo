# Tasks — backend-deployment

> Spec: `.sdd/backend-deployment/requirements.md`. Tasks are in implementation order; each depends
> only on tasks with a lower number. Task 02 amends the `V1` migration rather than adding `V2`
> (V1 has never been deployed; it is frozen from task 09's first production deploy on). Every
> amendment invalidates existing development databases through Flyway's checksum check — reset them
> with `docker compose -f deploy/compose.dev.yaml down -v`.

## Task 01-postgres-persistence

The backend keeps devices, alarms and the notification log in PostgreSQL instead of SQLite, end to
end: a developer starts a local Postgres with one command, runs the server against it with no
further configuration, and the existing app (registration, alarm list, create, edit, pause, delete,
delivery) works exactly as before. The schema is created by versioned Flyway migrations run as a
schema-owner role; the running server uses a separate login that can only read and write data.
Server configuration is read once into a typed configuration object with development defaults; in
production mode it refuses to start and names every missing or unreadable item, and never falls back
to the logging push sender. SQLite disappears from the server. The device and alarm API is unchanged
in this task (the device id is still the path credential — task 02 replaces it).

### Implementation steps

- [x] Add to the catalog and `:server`: PostgreSQL JDBC, HikariCP, Flyway core + `flyway-database-postgresql` (same version, a release listing PostgreSQL 18 as supported, so no "upgrade recommended" warning), the Exposed date/time module, Testcontainers **2.x** (`testcontainers-postgresql`); remove `sqlite-jdbc`.
- [x] Introduce the configuration object with `load(env, readFile)`: `POLLENINFO_ENV` (default development), `PORT` (default 8080), `DB_URL`, `DB_OWNER_USER`, `DB_OWNER_PASSWORD_FILE`, `DB_APP_USER`, `DB_APP_PASSWORD_FILE`, `FCM_CREDENTIALS`. Development defaults match the dev compose file; production requires the database URL, both password files and the FCM key, readable, and fails with one error listing every problem.
- [x] Wire `main()` and `Application.module(config)` through it: port from config, push sender from config (no logging fallback in production), database from config.
- [x] Add `deploy/compose.dev.yaml` (Postgres 18.x pinned, `127.0.0.1:5432`, a named network, committed dev-only passwords) and the shared role-init script (database `polleninfo`, `polleninfo_owner`, `polleninfo_app` with DML-only default privileges, `CREATE` on `public` revoked from `PUBLIC`), reading passwords from `*_FILE`.
- [x] Write `V1__init.sql`: `devices`, `alarms` (`text[]` species and days, `time` columns, `timestamptz`, `ON DELETE CASCADE` to devices), `notification_log` (`date`), index on `(device_id, created_at)`.
- [x] Rewrite the database entry point: Flyway migrate as owner on a short-lived connection, then a small Hikari pool (≈5) as the app role, closed on application stop. Remove `SchemaUtils.create`, `inMemory()`, the SQLite pragmas and `POLLENINFO_DB`.
- [x] Port the three Exposed stores to the new column types; the ten-alarm check locks the device row (`SELECT … FOR UPDATE`) so concurrent creates at nine cannot both pass.
- [x] Remove the database and store defaults from `configureRouting` (alarm routes take their stores explicitly), so health and pollen route tests need no database.
- [x] Add a shared Testcontainers Postgres for tests: one container per test JVM, the image tag from one constant (to be matched by `compose.yaml` in task 07), a clean schema per test class; without Docker these tests fail fast with a message saying Docker is required. Port `ExposedStoresTest`, `AlarmRoutesTest` and `AlarmSchedulerTest` onto it; delete `LegacyDatabaseCompatibilityTest` and `server/src/test/resources/fixtures/db/`.
- [x] Add tests for the configuration object and for migrations and roles.
- [x] Update `CLAUDE.md`: Commands (dev database up and reset, Docker needed for `:server:test`), Persistence, Server conventions tree, Testing; remove every SQLite, `POLLENINFO_DB` and `server/data/` reference.

### Acceptance criteria

- [ ] ~~With `deploy/compose.dev.yaml` up, `./gradlew :server:run` starts with no exported variable, applies V1 on an empty database, and the debug app on the emulator can register, create, edit, pause and delete an alarm (manual).~~ *(skipped: the emulator part needs interactive use of the app UI. Verified instead: the server starts with no exported variable and applies V1 on an empty database; the same REST calls the app makes (register, create, edit, pause, token update, list, delete) succeed via curl; a daily report due the next minute was sent by the scheduler.)*
- [x] After `docker compose -f deploy/compose.dev.yaml down -v` and `up -d`, the next server start runs on an empty, freshly migrated schema (manual).
- [x] A migration test proves: V1 applies to an empty database; a second migrate applies nothing; the app role can select, insert, update and delete in every table, while `CREATE TABLE`, `ALTER TABLE` and `DROP TABLE` fail for it.
- [x] A store test starts two creates concurrently for a device holding nine alarms and asserts exactly one `Created` and one `LimitReached`.
- [x] All previously existing store, route and scheduler test cases pass against Postgres (arrays, times and dates round-trip; creation order kept; the notification log survives a scheduler built a second time).
- [x] Configuration tests cover: development defaults; production with everything present; production missing the FCM key and both password files fails with one error naming all three; an unreadable password file; a malformed `PORT`.
- [x] With Docker stopped, `./gradlew :server:test --tests '*PollenRoutesTest' --tests '*RoutingTest' --tests '*CsvParserTest' --tests '*TtlCacheTest' --tests '*HistoryServiceTest' --tests '*AlarmRulesTest'` passes.

### Quality gates

- [x] `./gradlew :server:test` passes with Docker running.
- [x] `./gradlew :composeApp:testAndroidHostTest` passes (app untouched).
- [x] No new compiler warning or Gradle deprecation beyond the two documented plugin ones.
- [x] `grep -rni sqlite server/ gradle/libs.versions.toml` finds nothing.
- [x] `grep -n "POLLENINFO_DB\|polleninfo.db\|sqlite" CLAUDE.md` finds nothing.

## Task 02-device-bearer-token

An install proves who it is with a secret device token sent as a bearer header instead of an id in
the URL, end to end through server and app. Registration returns a 256-bit token once; the server
stores only its SHA-256 and identifies the device by an internal UUID. Every device call lives under
`/devices/me`; a missing or unknown token is `401`, which makes the app clear its stored token,
register once and retry once, exactly as the `404` did before. A `404` on a single alarm now only
means the alarm, so the app no longer probes the list. An install can erase itself with
`DELETE /devices/me`. The device's last-seen time is recorded at most once a day. On the phone the
token is kept in its own small preferences file, excluded from cloud backup and device transfer.

### Implementation steps

- [x] Amend `V1__init.sql` to the final `devices` shape (`id uuid`, `token_hash bytea unique`, `fcm_token`, `created_at`, `last_seen_at`) and `alarms.device_id uuid`.
- [x] Add the device-token domain (32 random bytes from `SecureRandom`, base64url without padding; SHA-256 hash) replacing the old id generator; `DeviceId` becomes the internal UUID.
- [x] Reshape the device store: `register` returns the token; `authenticate(token)` returns the device id or `null` and refreshes `last_seen_at` when it is older than a day; `updateFcmToken`; conditional `clearFcmToken`; `delete` (cascading). Drop `exists` and the alarm store's "unknown device" outcomes; adapt the scheduler's token drop.
- [x] Add `ktor-server-auth`; install a bearer provider `device` resolving to a device principal (`401` with `WWW-Authenticate: Bearer` otherwise) and rewrite the alarm routes to `POST /devices`, `DELETE /devices/me`, `PUT /devices/me/fcm-token`, `GET|POST /devices/me/alarms`, `PUT|DELETE /devices/me/alarms/{alarmId}`; authentication precedes body validation; a non-UUID alarm id is `404`.
- [x] App: `AlarmApiService` adds the bearer header to every device call, uses the new paths, maps `401` to `UnknownDeviceException` and a single-alarm `404` to `AlarmNotFoundException` with no list request; registration reads `deviceToken`.
- [x] App: `AlarmRepositoryImpl` and `DeviceRegistrationRepository` hold the token (renamed from id); `updateToken` on `401` clears only the token it used.
- [x] App: back `DeviceRegistrationRepository` with a second DataStore file `polleninfo_device`, provided by both platform modules under a Koin qualifier.
- [x] Android: data-extraction rules (API 31+) and full-backup rules (≤ API 30) excluding that file from cloud backup and device transfer, referenced from the manifest.
- [x] Update server and app tests; update `CLAUDE.md`: REST API table, Devices and alarms, Alarms (registration and `AlarmApiService`), Persisted user selections, Error handling.

### Acceptance criteria

- [x] Route tests cover every row of the new route table, `401` for a missing, malformed and unknown token, `401` (not `400`) for an invalid body without a token, `404` for another device's alarm and for a non-UUID alarm id, and `401` on every device call after `DELETE /devices/me`.
- [x] Store tests prove: register → authenticate round trip; an unknown token authenticates to `null`; the `devices` row holds the token's hash and not the token; `last_seen_at` is refreshed one day after the last refresh and not one millisecond before; deleting a device removes its alarms and their log.
- [x] `AlarmRepositoryImplTest` proves: every device call carries `Authorization: Bearer <token>`; a `401` re-registers once and retries once for each call; a second `401` is a `Failure`; a single-alarm `404` fails with `AlarmNotFoundException` with no list request and no registration; registration stores the returned token; `updateToken` on `401` clears the token only if it is still the one used.
- [x] A log-capture test drives a full device flow (register, list, create, update token, delete) through `testApplication` and asserts the captured log contains neither the device token nor the push address in full.
- [x] Manual: the debug app against the dev backend registers and manages alarms; after its `devices` row is deleted in `psql` it re-registers on its own on the next Alarms visit and shows an empty list without an error.
- [x] The merged debug and release manifests reference both backup-rules files, and both files exclude `datastore/polleninfo_device.preferences_pb` and nothing else.

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest :server:test` passes.
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [x] `./gradlew :composeApp:checkTranslations` passes.
- [x] `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` succeed with no new warning.
- [x] `grep -rn "{deviceId}\|checkedForAlarm\|deviceExists" server/src composeApp/src CLAUDE.md` finds nothing.

## Task 03-inactive-device-cleanup

Installs that can no longer be reached and have gone silent are removed automatically: on the first
scheduler tick of each Swiss day, every device without a push address whose last-seen time is older
than 90 days is deleted together with its alarms and notification log. A device with a push address
is never removed for inactivity, and a failing cleanup never costs that minute's alarms.

### Implementation steps

- [ ] Add `pruneInactive(now)` to the device store with a 90-day retention constant.
- [ ] Run it on the scheduler's first tick of each Swiss day next to the notification-log prune, each with its own failure handling and its own retry on the next tick.
- [ ] Tests for the store boundaries and the scheduler step; update `CLAUDE.md` (Devices and alarms, Alarm delivery).

### Acceptance criteria

- [ ] Store test: a device without a push address last seen 90 days and 1 ms ago is deleted with its alarms and log; one last seen exactly 90 days ago is kept; one with a push address last seen a year ago is kept.
- [ ] Scheduler test: the first tick of a Swiss day prunes inactive devices; a later tick the same day does not prune again.
- [ ] Scheduler test: a prune that throws still lets that minute's due alarms be sent, and the prune is retried on the next tick.
- [ ] Scheduler test: after `Unregistered` clears a device's push address, the device is kept until 90 days after its last call and deleted by the first daily prune after that.

### Quality gates

- [ ] `./gradlew :server:test` passes.
- [ ] No new compiler warning.

## Task 04-scheduler-restart-catch-up

A restart — a deploy or a crash — no longer silently drops the daily reports whose minute fell into
the gap. The scheduler persists the last Swiss minute it finished; on start it processes every
missed minute that is at most two minutes old, in order, before resuming the normal loop, and never
processes a minute twice. A longer outage is still never replayed, and threshold alerts keep their
per-day, per-type guarantee.

### Implementation steps

- [ ] Amend `V1__init.sql` with a one-row `scheduler_state(last_minute timestamptz)` table.
- [ ] A small store for it (read, write) on the app role.
- [ ] The scheduler writes the minute after its sends for that minute are done; on start it runs the missed minutes no older than `MAX_CATCH_UP = 2 minutes` (oldest first) through the same `tick` logic for that minute, then the normal loop.
- [ ] Tests with `MutableClock`; update `CLAUDE.md` (Alarm delivery: replace "There is no catch-up" with the two-minute rule and its reason).

### Acceptance criteria

- [ ] Scheduler test: last minute 07:59 stored, restart at 08:01:10 → the 08:00 daily report is sent exactly once, then 08:01 runs normally.
- [ ] Scheduler test: last minute 07:55 stored, restart at 08:01:10 → no report for 07:56–07:58 is sent (older than two minutes); 07:59 and 08:00 are processed.
- [ ] Scheduler test: a minute already stored as processed is not sent again by a second scheduler built on the same database.
- [ ] Scheduler test: an empty `scheduler_state` (first ever start) catches up nothing.
- [ ] Scheduler test: a threshold alert that already notified a type today does not notify it again during catch-up.

### Quality gates

- [ ] `./gradlew :server:test` passes.
- [ ] No new compiler warning.
- [ ] `grep -n "no catch-up" CLAUDE.md` finds no statement contradicting the two-minute rule.

## Task 05-abuse-protection

The server protects itself against floods and never leaks internals: registrations, device calls and
pollen reads are rate-limited with configurable limits (5/hour per address, 60/minute per device,
120/minute per address), answered with `429 {error}` and `Retry-After`; the client address comes from
the proxy's forwarding header only when the server is configured to trust its proxy; oversized bodies
are refused; any unexpected exception produces a generic `500 {"error":"internal error"}` with the
details only in the log; request logging never contains the authorization header or bodies; no
cross-origin access is granted.

### Implementation steps

- [ ] Add `ktor-server-rate-limit` and `ktor-server-forwarded-header`.
- [ ] Extend the configuration object with the three limits (`RATE_LIMIT_*`) and a trusted-proxy flag (default on in production, off in development).
- [ ] Install `XForwardedHeaders` only when trusted. Install three named limiters: `register` by address around `POST /devices`; `device` **inside** `authenticate("device")`, keyed on the principal, so random unknown tokens cannot each get a bucket; `pollen` by address around the pollen routes. `/health` unlimited.
- [ ] `StatusPages`: a `429` handler with an `{error}` body (keeping `Retry-After`), and a catch-all that logs and answers a generic `500 {error}`.
- [ ] Refuse bodies above 16 KB with `413`, for both `Content-Length` and chunked bodies (enforced while reading).
- [ ] Make sure `CallLogging` logs neither `Authorization` nor bodies; no CORS plugin.
- [ ] Route tests with small configured limits; update `CLAUDE.md` (REST API statuses, Server conventions, configuration variables).

### Acceptance criteria

- [ ] For each limiter, a route test shows request N succeeds and request N+1 gets `429` with a `Retry-After` header and an `{error}` body; two addresses (or two devices) are counted independently.
- [ ] A route test at the default pollen limit sends 120 measurement requests (8 × 15) from one address within a minute — all succeed — and the 121st gets `429`.
- [ ] With trusted proxy on, the bucket follows `X-Forwarded-For`; with it off, a spoofed `X-Forwarded-For` does not change the bucket.
- [ ] A 17 KB body is answered `413`, sent both with `Content-Length` and chunked, and the route's handler is not invoked (asserted through a fake store that records calls).
- [ ] A route that throws `IllegalStateException("secret detail")` answers `500` whose body contains neither "secret detail" nor a stack trace, and the log contains the exception.
- [ ] A request with an `Origin` header and a CORS preflight `OPTIONS` receive no `Access-Control-Allow-Origin` header.
- [ ] Configuration tests cover the limit variables' defaults and a malformed value.

### Quality gates

- [ ] `./gradlew :server:test` passes.
- [ ] No new compiler warning.

## Task 06-container-image-pipeline

Every push to `main` that touches the server produces a tested, vulnerability-scanned server image
in the private GitHub registry, tagged with the commit; pull requests run the tests and the scan
without publishing; dependency and action updates arrive as automated pull requests. The CI is laid
out so that Android and iOS store pipelines can be added later as their own workflows without
reworking this one: one workflow per shipped artifact with path filters, a shared JDK/Gradle setup
action, and least-privilege permissions. The image runs as a fixed non-root
user with a capped JVM heap and starts locally against the dev database.

### Implementation steps

- [ ] Commit `gradle-wrapper.jar` (remove it from `.gitignore`) so CI runs `./gradlew` from a clean checkout; update the CLAUDE.md note.
- [ ] Add a composite action `.github/actions/setup-jvm` (JDK 21 + `gradle/actions/setup-gradle`, which caches and validates the wrapper), for every present and future workflow.
- [ ] Runtime-only Dockerfile: a pinned `eclipse-temurin:21-jre` copying the `:server:installDist` output, a fixed non-root uid, `EXPOSE 8080`, `JAVA_OPTS` with `-Xmx512m` and `-XX:+ExitOnOutOfMemoryError`; `.dockerignore` keeps everything but the distribution out. The distribution is built by Gradle on the host or runner, because the project cannot be configured without an Android SDK.
- [ ] Workflow `server.yml`, triggered on pull requests and pushes to `main` filtered to `server/**`, `gradle/**`, the root Gradle files, `deploy/**`, `.github/actions/**` and itself: a test-and-scan job (shared setup, `:server:test` with Testcontainers, `:server:installDist`, image build, Trivy scan with `CRITICAL`, ignore unfixed, fail the job); a publish job on `main` only that pushes `ghcr.io/timstenzel/polleninfo-server:<sha>` and `:latest`.
- [ ] Permissions: workflow default `contents: read`; only the publish job gets `packages: write`. Every action pinned by commit SHA. No repository-wide secrets; document in `deploy/README.md` that future store credentials go into GitHub Environments with required reviewers, and that app workflows will be `android.yml` / `ios.yml`, triggered by `android-v…` / `ios-v…` tags or manually.
- [ ] Dependabot: `gradle`, `docker` (Dockerfile), `github-actions`, weekly.
- [ ] `CLAUDE.md` Commands: building and running the image locally; start `deploy/README.md` with that section.

### Acceptance criteria

- [ ] After `./gradlew :server:installDist`, `docker build` succeeds on a machine whose build does not need an Android SDK for the image step, and the image started with `POLLENINFO_ENV=development` and `DB_URL=jdbc:postgresql://postgres:5432/polleninfo` on the dev compose network answers `GET /health` with `OK`.
- [ ] `docker run --rm --entrypoint id <image> -u` prints the fixed non-zero uid.
- [ ] `docker run --rm --entrypoint find <image> / -name '*.json' -path '*fcm*' -o -name '*Test*.class'` finds nothing.
- [ ] A push to `main` touching `server/` produces a green run and a package `polleninfo-server` tagged with the commit SHA; a pull request run executes tests and scan and publishes nothing.
- [ ] A push that changes only files under `composeApp/` (or only `CLAUDE.md`) does not start `server.yml`.
- [ ] A throwaway pull request that pins the runtime base to an image with a known fixable critical CVE fails at the scan step and publishes nothing.
- [ ] GitHub's Dependabot tab shows the three configured ecosystems without configuration errors.

### Quality gates

- [ ] `actionlint` reports no errors for the workflow and the composite action.
- [ ] Every `uses:` in `.github/` references a 40-character commit SHA (grep).
- [ ] The workflow declares `permissions: contents: read` at top level and `packages: write` only on the publish job (grep or `actionlint` output).
- [ ] `docker build --check` (or `hadolint`) reports no errors for the Dockerfile.
- [ ] `./gradlew :server:test` passes.
- [ ] `git ls-files gradle/wrapper` lists `gradle-wrapper.jar`.

## Task 07-production-stack

The complete production stack is described in versioned files and runs on the developer's machine
over HTTPS: Caddy is the only service with published ports, terminates TLS and adds the security
headers; the server image and Postgres sit on an internal network with memory limits, log rotation,
no-new-privileges and secrets mounted as files readable only by their consumer; the server shuts
down gracefully within the compose stop period, so a deploy lets the running tick finish.

### Implementation steps

- [ ] `deploy/compose.yaml`: `caddy`, `server` (tag from `SERVER_TAG`), `postgres` (the Testcontainers tag, volume `pgdata`, `pg_isready` healthcheck, `shared_buffers` 128 MB, `max_connections` 20, `scram-sha-256` with `pg_hba` limited to the compose network); only Caddy publishes `80`, `443/tcp`, `443/udp`; `restart: unless-stopped`, memory limits, `json-file` rotation, `no-new-privileges`, server read-only root with a `/tmp` tmpfs, `depends_on` healthy Postgres, `stop_grace_period` longer than the Netty grace plus one tick.
- [ ] Compose secrets from `/opt/polleninfo/secrets/`; document and script that each file is owned by its reader's uid with mode `0400` (Postgres files uid 999, server files the image's uid), since non-Swarm compose ignores `uid`/`mode`.
- [ ] Postgres runs the shared role-init script with the production password files.
- [ ] `Caddyfile` for `api.polleninfo.ch`: reverse proxy to `server:8080`, HSTS `max-age=31536000; includeSubDomains`, `nosniff`, `no-referrer`, `Server` removed, 16 KB body limit, read/write timeouts, no access log.
- [ ] `deploy/compose.local.yaml` override (`localhost` site, local image), `deploy/secrets.example/`, `deploy/.env.example` with `SERVER_TAG`; `.gitignore` for real secrets and `.env`.
- [ ] Graceful shutdown: Netty grace period and timeout; the scheduler loop lets an in-flight tick finish before cancelling.
- [ ] Runbook section "Try the production stack locally"; `CLAUDE.md` gains a Deployment section pointing to `deploy/README.md`.

### Acceptance criteria

- [ ] Following the runbook's local section with real `0400` secret files, the stack starts; `curl https://localhost/health` returns `OK` with `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and no `Server` header; `http://localhost/health` redirects to HTTPS.
- [ ] `docker compose ps` shows published ports only on `caddy`; connections from the host to 5432 and 8080 are refused.
- [ ] `docker inspect` shows no password or key in the server's environment, and shows the memory limits, `no-new-privileges` and the server's read-only root filesystem.
- [ ] With `FCM_CREDENTIALS` pointing at a non-existent file, the server container exits with an error naming it.
- [ ] Inside the stack, `psql` as `polleninfo_app` is refused `DROP TABLE alarms`.
- [ ] On the local stack, requests with a forged `X-Forwarded-For` beyond the registration limit still get `429` (the limit follows the real address).
- [ ] `docker compose stop server` during a tick logs that tick's completion before the process exits (manual, with a log line), and `docker compose up -d` brings it back.

### Quality gates

- [ ] `docker compose --env-file deploy/.env.example -f deploy/compose.yaml config` validates without warnings.
- [ ] The Postgres image tag in `deploy/compose.yaml` and `deploy/compose.dev.yaml` equals the Testcontainers constant (a grep or a test).
- [ ] `git ls-files deploy` contains only placeholder secrets.
- [ ] Dependabot gains the `docker-compose` ecosystem for `deploy/`.
- [ ] `./gradlew :server:test` passes.

## Task 08-app-production-base-url

Release builds of the Android app talk only to `https://api.polleninfo.ch`; debug builds use the
local backend by default and can be pointed at any address with one Gradle property; iOS receives
its address from its entry point. The address flows into Koin from the platform entry point,
replacing the hard-coded per-platform constants.

### Implementation steps

- [ ] Enable `BuildConfig` in `:androidApp` with `API_BASE_URL` per build type: release `https://api.polleninfo.ch`; debug from the Gradle property `polleninfo.apiBaseUrl` (command line or `~/.gradle/gradle.properties`) or `http://10.0.2.2:8080`.
- [ ] Make `appModules` take the base address (or a Koin single holding it) and pass it from `PollenInfoApplication` and `MainViewController` (`http://localhost:8080`); delete the `apiBaseUrl` expect/actual pair.
- [ ] Update `CLAUDE.md` ("Talking to our own backend", iOS wrapper configuration).

### Acceptance criteria

- [ ] The generated release `BuildConfig` has `API_BASE_URL = "https://api.polleninfo.ch"`, and the merged release manifest has neither `usesCleartextTraffic` nor `ACCESS_LOCAL_NETWORK`.
- [ ] A debug build without the property reaches the dev backend at `10.0.2.2:8080` (manual: Home loads).
- [ ] `./gradlew :androidApp:assembleDebug -Ppolleninfo.apiBaseUrl=https://api.polleninfo.ch` produces a debug `BuildConfig` with that address.
- [ ] `grep -rn "expect val apiBaseUrl\|actual val apiBaseUrl" composeApp/src` finds nothing.

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] `./gradlew :composeApp:checkTranslations` passes.
- [ ] `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` succeed with no new warning.

## Task 09-vps-go-live

The backend runs in production on the Infomaniak VPS at `https://api.polleninfo.ch`: the VPS is
hardened by an idempotent provisioning script, DNS at Hostpoint points `api` at it, secrets are
generated on the server with the right owners, a chosen image tag is deployed and rolled back with
one command, and a release build of the app works end to end against it, including a delivered push
notification.

### Implementation steps

- [ ] `deploy/provision.sh` (idempotent, as root): sudo user with an authorized key; SSH drop-in named to sort first (e.g. `00-polleninfo.conf`, since cloud-init's file may enable passwords) with no root login and no password or keyboard-interactive auth, then restart `ssh`; `ufw` allowing OpenSSH, 80/tcp, 443/tcp, 443/udp, default deny; `fail2ban` sshd jail with `backend = systemd`; `unattended-upgrades` with automatic reboot at 04:00; Docker Engine and the Compose plugin from Docker's apt repository (verify a 26.04 suite exists, else document the fallback); `daemon.json` log defaults; 2 GB swap; `/opt/polleninfo/{secrets,deploy}`.
- [ ] `deploy/deploy.sh <tag>`: over SSH, set `SERVER_TAG`, `docker compose pull server && docker compose up -d`; rollback is the same with the previous tag.
- [ ] Complete `deploy/README.md`: Hostpoint `A` (and `AAAA` only if IPv6 is served) record and verifying it before the first start, Infomaniak firewall, provisioning, secret generation (`openssl rand -base64 32`) and ownership, FCM key upload, `docker login ghcr.io` with a `read:packages` token, first start, routine deploy and rollback, manual `pg_dump -Fc`, SSH-tunnel `psql`, the "only Caddy publishes ports" rule, V1 frozen from now on, the privacy-policy requirement for a public Play release.
- [ ] Provision the VPS, create the DNS record, deploy, verify.

### Acceptance criteria

- [ ] `dig +short A api.polleninfo.ch` returns the VPS address; `https://api.polleninfo.ch/health` returns `OK` with a publicly trusted certificate; `http://` redirects to `https://`.
- [ ] From outside only 22, 80 and 443 answer (`nmap`); SSH as root and SSH with a password are refused.
- [ ] On the VPS: `ufw status` shows exactly the allowed ports; `fail2ban-client status sshd` shows the jail active; `systemctl is-enabled unattended-upgrades` is `enabled`; `docker compose version` works; `docker inspect` of each container shows `json-file` with size caps.
- [ ] Running `provision.sh` a second time completes without errors and reports no change.
- [ ] After `sudo reboot` the API answers again without manual action.
- [ ] `deploy.sh <sha>` switches the running version within a minute; `deploy.sh <previous-sha>` rolls back.
- [ ] A release build on a real phone loads Home and the Diary, creates a daily report due a few minutes later, and receives its notification.

### Quality gates

- [ ] `shellcheck deploy/*.sh` reports no errors.
- [ ] `git ls-files` lists no secret file.
- [ ] Every command and path in `CLAUDE.md`'s Deployment section matches `deploy/README.md`.
