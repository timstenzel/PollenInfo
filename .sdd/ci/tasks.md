# Tasks: ci

## Task [01-branching-model]

The git workflow becomes the project's written rule and the repository files follow it: the
assistant's branch, commit and pull-request handover rules are in the project instructions,
dependency updates target `develop`, keystores cannot be committed, and the runbook holds the
one-time repository setup checklist (back-merge, rulesets, merge methods, auto-delete) that the
maintainer applies by hand.

### Implementation steps

- [x] Commit the `.sdd/ci/` specification as the branch's first commit.
- [x] Add a "Git workflow" section to the project instructions: branch `feature/<sdd-dir>` from freshly fetched `origin/develop` at the first task (stop and ask on a dirty tree or an existing branch); spec files as first commit; one commit per finished task named `<feature>: <task title>`, pushed right away; never push to `develop`/`main`, never force-push (except the own feature branch after a rebase the maintainer asked for), never merge, never open or edit pull requests; after the last task a handover message with the compare link, the title `feature: <name>` and a Markdown description (Summary, Changes, Quality gates, For the reviewer) — no file, no attribution line; release pull requests `release: <versionName>` (`develop` → `main`) belong to the maintainer.
- [x] Set `target-branch: develop` on every entry of the Dependabot configuration.
- [x] Add `*.jks` to `.gitignore` next to `*.keystore`.
- [x] Add a "Repository setup" section to the runbook: 1. back-merge `main` into `develop` (exact commands) before any ruleset; 2. ruleset `develop`: pull request required with 0 approvals, squash only, force push and deletion blocked, required checks `Test, build and scan`, `android-gate`, `ios-compile`, "require branches to be up to date" off; 3. ruleset `main`: the same with merge commits only; 4. "Automatically delete head branches" on. (The signing environment is added to this checklist by task 07.)

### Acceptance criteria

- [x] The project instructions contain every git rule listed in the requirements' Technical Annex "Documentation" part, and no rule elsewhere in them contradicts it.
- [x] Every entry under `updates` in the Dependabot configuration has `target-branch: develop` (4 of 4).
- [x] A file `x.jks` created anywhere in the working tree does not appear in `git status`.
- [x] The runbook's setup checklist names the back-merge as its first step, with the commands, before any ruleset.
- [x] The checklist states squash-only for `develop` and merge-commit-only for `main`, both with a pull request required, force push and deletion blocked, and the three required check names exactly as above.

### Quality gates

- [x] The Dependabot configuration parses as YAML (`ruby -ryaml -e 'YAML.load_file(ARGV[0])' <file>`).
- [x] No file other than documentation, `.gitignore`, the Dependabot configuration and `.sdd/ci/` changed.

## Task [02-server-gate-on-every-pull-request]

Pull requests to `develop` and `main` and pushes to `main` always start the server pipeline. A
shared change-detection script decides from the changed paths whether the server gate actually
runs, so a skipped gate reports green and can be a required check. Publishing the image on `main`
keeps working, now only after a gate that ran.

### Implementation steps

- [ ] Add the change-detection script: takes base and head refs, diffs from their merge base, maps paths to `server` / `android` / `ios` exactly as the annex table (including `android.yml` → `android` and `ios`), writes `key=true|false` lines to `$GITHUB_OUTPUT` (stdout when unset), treats a missing or all-zero base as "everything changed".
- [ ] Rework the server workflow: drop both `paths` filters; `pull_request` limited to `develop` and `main`; keep `push` to `main`; no `workflow_dispatch`. Add a `changes` job (checkout with `fetch-depth: 0`, base = PR base sha or `github.event.before`); the gate job `needs: changes` and runs only when `server` is true. The publish job keeps `needs: test-and-scan` and its `main` condition — a skipped gate therefore skips publishing; do not add `always()`.
- [ ] Keep action pins, the `contents: read` default, concurrency, the job name `Test, build and scan` and the gate's steps unchanged; rewrite the header comment (no more "own path filters").
- [ ] Update the runbook's CI section for the server pipeline (always triggered, change detection, path table) and the project instructions' CI line (server gate per component).

### Acceptance criteria

- [ ] Run locally on a commit range touching only `server/`, the script outputs `server=true`, `android=false`, `ios=false`.
- [ ] On a range touching only `*.md` / `.sdd/` it outputs all three `false`; on a range touching `gradle/libs.versions.toml` all three `true`; with base `0000000000000000000000000000000000000000` all three `true`.
- [ ] Run with base `origin/main` and head `origin/develop`, it reports the components changed between the two (the release pull request case).
- [ ] The server workflow has no `paths` filter and no `workflow_dispatch`, triggers on `pull_request` to `develop`/`main` and `push` to `main`, its `changes` checkout uses `fetch-depth: 0`, and the gate job's `if` reads the `changes` job's `server` output.
- [ ] The publish job still runs only on `push` to `main` and `needs` the gate job, without `always()`.

### Quality gates

- [ ] The script passes `bash -n` and, if `shellcheck` is installed, `shellcheck` without findings.
- [ ] The workflow parses as YAML; every `uses:` is pinned by a 40-character SHA with a version comment.
- [ ] `./gradlew :server:test` passes (needs Docker; without it the non-database tests pass alone).

## Task [03-android-gate-on-pull-requests]

Every pull request that touches app code runs the Android gate: app tests, translation check, debug
and release builds, and the check that cleartext / local-network permissions and the "crash reports
off" flag exist only in the debug manifest. A pure server or documentation change skips it.

### Implementation steps

- [ ] Add the Android workflow with the same triggers, `changes` job and change-detection script as the server workflow (no `workflow_dispatch`); concurrency `android-<ref>`, cancelled in progress on pull requests only; default `contents: read`; setup through the shared JVM action.
- [ ] Add the gate job with `name: android-gate` (Linux), running only when `android` is true: `:composeApp:testAndroidHostTest :composeApp:checkTranslations :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`.
- [ ] Add the two merged-manifest checks as one step: for `usesCleartextTraffic\|ACCESS_LOCAL_NETWORK` and for `firebase_crashlytics_collection_enabled`, `grep -l` over the merged manifests must list exactly the debug manifest; otherwise the step fails with a message naming the pattern.
- [ ] Runbook: the CI section describes the Android pipeline; the rule "app pipelines triggered by a release tag … never on every push" is replaced. Project instructions: the CI line names the Android gate.

### Acceptance criteria

- [ ] The manifest-check step's script, run locally after `assembleDebug assembleRelease`, passes on the current code.
- [ ] The same script fails when `android:usesCleartextTraffic="true"` is temporarily added to the main manifest (checked locally, then reverted).
- [ ] The gate job's `name` is exactly `android-gate`, its `if` reads the `changes` job's `android` output, and its Gradle invocation is the one above with the mapping upload disabled.
- [ ] The runbook no longer contains the tag-triggered app pipeline rule and describes the Android pipeline's triggers and gate.

### Quality gates

- [ ] The gate's Gradle invocation passes locally.
- [ ] The workflow parses as YAML; every action pinned by SHA with a version comment; no job has more than `contents: read`.

## Task [04-lint-gate]

Android Lint becomes part of the Android gate and fails on any warning, covering all Android code
(app module, the shared module's Android code, theme), with the network-dependent version checks
off and the current findings fixed.

### Implementation steps

- [ ] Configure lint in the app module: warnings as errors, abort on error, check dependencies, disable `NewerVersionAvailable` and `GradleDependency`, no baseline.
- [ ] Make sure the shared module's `androidMain` and the theme are analysed — for the KMP library plugin, lint is configured in `kotlin { android { lint { } } }` if app-level `checkDependencies` does not reach them; prove coverage with a deliberate temporary finding in the shared module's Android code, then remove it.
- [ ] Fix the current findings: merge `mipmap-anydpi-v26` into `mipmap-anydpi`, add a `<monochrome>` layer to both adaptive launcher icons, mark `localeConfig` as API 33 (`tools:targetApi`); fix any further findings the wider coverage reveals.
- [ ] Amend the Android gate's Gradle invocation with `:androidApp:lintRelease` (six tasks); upload the lint report as an artifact when the job fails.
- [ ] Add lint to the local gates in the project instructions' Commands table.

### Acceptance criteria

- [ ] `./gradlew :androidApp:lintRelease` passes with 0 errors and 0 warnings, and its report holds no `NewerVersionAvailable` or `GradleDependency` entry.
- [ ] A temporary lint finding in the shared module's Android code makes `:androidApp:lintRelease` fail (checked, then removed).
- [ ] A temporary lint warning (not an error) in the app module makes it fail (checked, then removed).
- [ ] The Android gate's Gradle invocation contains `:androidApp:lintRelease`.
- [ ] On an emulator with API 33 or higher, the installed debug build shows its launcher icon, and with themed icons switched on the icon is tinted.

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` passes.
- [ ] No lint baseline file exists in the repository.

## Task [05-ios-compile-gate]

Pull requests that touch shared app code, the theme or shared build configuration compile the shared
code and its tests for the iOS simulator on a macOS runner, so JVM-only code in shared sources fails
the pull request.

### Implementation steps

- [ ] Add the job with `name: ios-compile` to the Android workflow (macOS runner), running only when `ios` is true: shared JVM setup, then `:composeApp:compileTestKotlinIosSimulatorArm64`.
- [ ] Runbook: describe the job in the CI section and note that iOS publishing comes with the iOS feature. Project instructions: the CI line names the iOS compile gate.

### Acceptance criteria

- [ ] Run locally, the change-detection script reports `ios=true` for ranges touching `composeApp/`, `theme/`, `gradle/libs.versions.toml` or the Android workflow file, and `ios=false` for a range touching only `androidApp/`.
- [ ] The job's `name` is exactly `ios-compile`, its `if` reads the `changes` job's `ios` output, and it runs only `:composeApp:compileTestKotlinIosSimulatorArm64` (nothing that needs an Xcode project).

### Quality gates

- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes locally.
- [ ] The workflow parses as YAML; actions pinned by SHA with a version comment.

## Task [06-kotlin-warnings-fail-the-build]

Any Kotlin compiler warning in any module fails the build, locally and in every CI gate, replacing
the "no new warning" convention.

### Implementation steps

- [ ] First list the existing warnings: run the compilations of all modules with `--rerun-tasks` without the flag (including generated code: Compose resources, serialization, `expect`/`actual`).
- [ ] Enable `allWarningsAsErrors` in the shared module at the top-level `kotlin { compilerOptions { } }` (so it reaches the iOS targets, not only inside `android { }`), in the theme, in the app module (AGP 9 built-in Kotlin) and in the server.
- [ ] Fix every warning this reveals; a warning that cannot be fixed in our code is reported to the maintainer before anything is suppressed.
- [ ] Project instructions: the "no new deprecation or compiler warning" rule now says Kotlin warnings fail the build; the two accepted plugin messages (Gradle deprecations) stay listed.

### Acceptance criteria

- [ ] `./gradlew :theme:compileAndroidMain :composeApp:testAndroidHostTest :composeApp:compileTestKotlinIosSimulatorArm64 :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false :server:compileKotlin :server:compileTestKotlin` passes.
- [ ] A temporary unused variable in shared `commonMain` code fails both the Android and the iOS compilation (checked, then removed).
- [ ] A temporary unused variable in server main code fails `:server:compileKotlin`, and one in the app module fails its compilation (checked, then removed).
- [ ] A temporary unused variable in the theme fails its compilation (checked, then removed).

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :server:test` passes (server database tests need Docker).
- [ ] A second identical run reports "Reusing configuration cache".

## Task [07-signed-android-release-on-main]

A merge to `main` that changes the app builds a signed release APK and app bundle with an increasing
version code, uploads the R8 mapping to Crashlytics, and publishes the APK, bundle and mapping as a
GitHub prerelease tagged `android-v<versionName>-<versionCode>`. Without the signing credentials,
release builds stay unsigned as today.

### Implementation steps

- [ ] App build: a release signing config filled from `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` (environment variables), applied only when all four are set.
- [ ] Version code from `git rev-list --count HEAD` (configuration-cache safe value source, fallback `1`); version name stays a hand-set literal. A small Gradle task prints both for CI.
- [ ] Add the `release` job to the Android workflow: only on `push` to `main`, `needs: [changes, android-gate]`, only when `android` is true; environment `android-release`; `contents: write` for this job only; full-history checkout; decode the keystore secret into the runner's temp directory; fail with a clear message if any secret is empty; build APK and bundle with the mapping upload on; read name and code from the printing task; copy the outputs to `polleninfo-<versionName>-<versionCode>.apk` / `.aab`; create a prerelease with generated notes, tagged `android-v<versionName>-<versionCode>` on the pushed commit, with APK, bundle and `mapping.txt`.
- [ ] Runbook: "Android release signing" (the four secrets and how to produce the base64 value, keystore safe-keeping, signature difference to future Play builds, shallow PR builds get version code 1, a new commit invalidates the configuration cache); add the `android-release` environment (deployment branches limited to `main`, no required reviewers, the four secrets) to the Repository setup checklist; update the secrets rule (environments limited to `main`; required reviewers only for jobs that publish outward); add "Cutting a release" (optionally raise `versionName` on `develop`, open `develop` → `main` titled `release: <versionName>`, merge commit, check the GitHub release and the image, deploy with the deploy script).
- [ ] In the handover's "For the reviewer": check once by hand that Android Studio's signing wizard still produces a signed build.

### Acceptance criteria

- [ ] Without the four variables, `./gradlew :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` produces an unsigned APK whose `versionCode` (`aapt2 dump badging`) equals `git rev-list --count HEAD`.
- [ ] With the four variables pointing at a throwaway keystore, `assembleRelease bundleRelease` (mapping upload off) produces an APK that `apksigner verify` accepts and a bundle that `jarsigner -verify` reports as verified.
- [ ] With only three of the four variables set, the release build stays unsigned.
- [ ] The printing task outputs the version name and code matching the build.
- [ ] The `release` job runs only on push to `main`, needs `changes` and `android-gate`, uses environment `android-release`, is the only job with `contents: write`, and creates a prerelease tagged `android-v<versionName>-<versionCode>` with the renamed APK, bundle and the mapping attached.
- [ ] The runbook contains the signing section, the environment in the setup checklist, the updated secrets rule and the release checklist.

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :androidApp:lintRelease :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` passes; a second identical run reuses the configuration cache.
- [ ] No keystore, password or alias is in the repository; the workflow parses as YAML with every action pinned by SHA.
