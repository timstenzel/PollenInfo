# Deploying the PollenInfo backend

The runbook for the server's container image and, later, the production stack.

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
