# CI pipeline and git workflow

## Problem Statement

PollenInfo is developed feature by feature, mostly by an AI coding assistant, and reviewed by a
single maintainer. Today only the backend has an automated pipeline; the mobile app's code is
checked only when someone remembers to run the checks locally, and nothing guards the shared code
against breaking the iOS build. There are no agreed rules for how feature work reaches the
integration branch or how a release reaches the main branch: branch names drift from feature
names, dependency updates land directly on the main branch, and the two long-lived branches have
quietly diverged in history. A merge to the main branch produces a backend image but no installable
Android build, so testing a release on a phone means building and signing it by hand.

## Solution

A fixed branching model with automated quality gates:

- Every feature is developed on its own branch named after the feature, and reaches the integration
  branch `develop` through a pull request that the maintainer opens, reviews and merges. The
  assistant prepares the branch, the commits and a ready-to-paste pull request title and
  description, but never opens or merges pull requests itself.
- A release is a pull request from `develop` to `main`, opened by the maintainer.
- Every pull request — feature or release — runs only the quality gates of the components it
  touches: backend, Android app, and a compile check of the shared code for iOS. A pull request that
  only changes documentation runs nothing. The gates always report, so they can be required before
  merging.
- Each merge to `main` publishes the backend image (as today, when the backend changed) and, when
  the app changed, a signed, installable Android build attached to a GitHub release.
- The repository's branch rules enforce the model: no direct pushes to `develop` or `main`, gates
  must pass, features are squashed into `develop`, releases are merged into `main` with a merge
  commit.

## User Stories

### Branching and pull requests

1. As the maintainer, I want every feature developed on a branch called `feature/<name>`, where
   `<name>` is the feature's specification folder, so that I can tell from a branch which feature it
   belongs to.
2. As the maintainer, I want each feature branch to start from the latest `develop`, so that it
   contains every feature merged before it.
3. As the maintainer, I want the feature's specification to be part of its branch, so that the
   pull request shows what was asked for alongside what was built.
4. As the maintainer, I want one commit per completed task, so that I can review a large feature
   step by step.
5. As the maintainer, I want only finished tasks committed, so that no commit holds half-done work.
6. As the maintainer, I want the feature branch pushed after every task, so that the work is backed
   up outside my machine.
7. As the maintainer, I want the assistant to stop and ask when the working tree is not clean or
   the branch already exists elsewhere, so that no work is overwritten by a guess.
8. As the maintainer, I want the assistant never to push to `develop` or `main`, never to
   force-push, and never to merge, so that every change to the shared branches passes through my
   review.
9. As the maintainer, I want to open pull requests myself, so that I decide when a feature is ready
   for review.
10. As the maintainer, I want the assistant to give me, once all tasks are done, the link that opens
    the pull request against `develop`, the title `feature: <name>` and a ready-to-paste
    description, so that opening it takes seconds.
11. As the reviewer, I want the description to hold a short summary, the changes per task, the
    quality gates run locally and their results, and anything I must do or check by hand, so that I
    know what to look at.
12. As the maintainer, I want features squashed into `develop`, so that `develop` has one commit per
    feature named `feature: <name>`.
13. As the maintainer, I want releases merged into `main` with a merge commit, so that `develop` and
    `main` never drift apart and later release pull requests show only new changes.
14. As the maintainer, I want merged feature branches deleted automatically, so that the branch
    list stays short.
15. As the maintainer, I want dependency update pull requests to target `develop`, so that they
    reach `main` with the next release like everything else.
16. As the maintainer, I want `develop` to contain `main`'s history once before the rules are
    switched on, so that the first release pull request does not carry old commits.
17. As the maintainer, I want nothing to land on `main` except release merges from `develop`, so
    that `main` always equals a reviewed release.

### Quality gates

18. As the reviewer, I want every pull request to run the gates of the components it touches, so
    that problems are found before merging.
19. As the maintainer, I want a pull request that touches only the backend to skip the app gates,
    and the reverse, so that runs are fast and cheap.
20. As the maintainer, I want a change to shared build configuration (the dependency catalog, the
    Gradle wrapper, root build files, the shared CI setup) to run every gate, so that a version bump
    cannot break a component unnoticed.
21. As the maintainer, I want a pull request that only changes documentation or specifications to
    run no gate, so that writing docs costs no build minutes.
22. As the maintainer, I want the gates required for merging into `develop` and `main`, and still
    shown as passed when they were skipped, so that a docs-only pull request is not blocked forever.
23. As the maintainer, I want the release pull request to run the same gates, chosen by everything
    that changed since the last release, so that the release is checked as a whole.
24. As the reviewer, I want the backend gate to keep running the server tests, the image build and
    the vulnerability scan, so that nothing the server pipeline checks today is lost.
25. As the reviewer, I want the Android gate to run the app's tests, the translation check, the
    Android linter and both a debug and a release build, so that a missing shrinker rule or
    resource problem is found before release.
26. As the reviewer, I want the Android gate to verify that the cleartext and local-network
    permissions and the "crash reports off" switch appear only in the debug build, so that a
    release can never talk to a server unencrypted or ship with crash reporting disabled by the
    debug setting.
27. As the reviewer, I want the linter to fail on warnings, not only errors, so that the app stays
    free of lint findings.
28. As the maintainer, I want the linter not to complain that newer library versions exist, so that
    an unchanged pull request does not turn red because a library was released overnight, and
    because dependency updates already have their own process.
29. As the reviewer, I want the linter to cover all of the app's Android code, including the shared
    module and the theme, so that no part escapes it.
30. As the reviewer, I want any Kotlin compiler warning to fail the build, locally and in CI, so
    that the "no new warning" rule is enforced instead of remembered.
31. As the iOS developer to come, I want shared-code changes compiled for iOS on every pull request,
    so that the iOS feature does not start by repairing breakage.
32. As the maintainer, I want the iOS compile to run only when shared app code or shared build
    configuration changes, so that expensive macOS minutes are spent only where they matter.

### Releases

33. As the maintainer, I want each merge to `main` that changes the backend to publish the backend
    image as today, so that I can deploy the release.
34. As the maintainer, I want each merge to `main` that changes the app to produce a signed release
    APK and app bundle, so that I can install the release on a phone and keep the bundle for Play.
35. As the maintainer, I want those files attached to a GitHub release tagged with the version, so
    that they are kept and easy to download on a phone.
36. As the maintainer, I want the release's shrinker mapping attached and uploaded to the crash
    reporting service, so that crashes from installed releases have readable stack traces.
37. As the maintainer, I want every release build to carry a higher version code than the one
    before, so that a new release installs over the old one.
38. As the maintainer, I want to set the human-readable version name myself, so that I decide when
    a release counts as a new version.
39. As the maintainer, I want the signing key's credentials stored as secrets of a GitHub
    environment that only `main` can use, so that no pull request or feature branch can read them.
40. As the maintainer, I want release builds without signing credentials (pull requests, local
    builds) to stay unsigned as today, so that nobody needs the key to work on the app.
41. As the maintainer, I want to keep signing locally through Android Studio's signing wizard, so
    that I can produce a signed build without CI.
42. As the maintainer, I want the GitHub release marked as a prerelease, so that it is not mistaken
    for a store release while Play publishing does not exist.
43. As the maintainer, I want only the job that creates the release to have write access to the
    repository, so that every other job stays read-only.
44. As the maintainer, I want a keystore file to be ignored by git, so that it can never be
    committed by accident.

### Documentation

45. As the maintainer, I want a checklist for the one-time repository setup (back-merge, branch
    rules, merge methods, signing environment and secrets), so that I can apply it by hand.
46. As the maintainer, I want a checklist for cutting a release, so that releases are done the same
    way every time.
47. As a future contributor (human or assistant), I want the git rules and the CI gates written in
    the project instructions, so that every session follows them.
48. As the maintainer, I want the operations runbook to describe the app pipeline and its rules,
    replacing the old plan of tag-triggered app pipelines, so that the docs match what runs.

## User Acceptance Tests

### Branching and pull requests

1. Given a new feature specification called `x` and a clean working tree on any branch, when the
   assistant starts the first task, then it is working on `feature/x` created from the latest
   `origin/develop`, and the specification is its first commit.
2. Given uncommitted changes in the working tree, when the assistant starts the first task of a
   feature, then it stops and asks instead of creating the branch.
3. Given a feature with three tasks, when all three are done, then the feature branch on GitHub has
   one commit per task (after the specification commit), each named after its task.
4. Given the last task of feature `x` is done, when the assistant reports, then its message holds a
   link that opens a pull request from `feature/x` into `develop`, the title `feature: x`, and a
   description with a summary, the changes, the quality gates run and notes for the reviewer — and
   no pull request has been opened on GitHub.
5. Given the branch rules are switched on, when anyone tries to push directly to `develop` or
   `main`, then GitHub refuses the push.
6. Given a feature pull request into `develop`, when the maintainer merges it, then only "squash and
   merge" is offered, and `develop` gains one commit titled `feature: <name>`, and the feature
   branch is deleted.
7. Given a release pull request from `develop` into `main`, when the maintainer merges it, then only
   a merge commit is offered.
8. Given dependency updates are due, when Dependabot opens pull requests, then they target
   `develop`.

### Quality gates

9. Given a pull request that changes only backend code, when it runs, then the backend gate runs
   and the Android and iOS gates show as skipped and passed.
10. Given a pull request that changes only the Android app module, when it runs, then the Android
    gate runs and the backend and iOS gates show as skipped and passed.
11. Given a pull request that changes the shared app code or the theme, when it runs, then the
    Android gate and the iOS compile run, and the backend gate is skipped.
12. Given a pull request that changes the dependency catalog, when it runs, then all three gates
    run.
13. Given a pull request that changes only Markdown files or specifications, when it runs, then
    every gate shows as skipped and passed, and the pull request can be merged.
14. Given a pull request with an Android lint warning, when the Android gate runs, then it fails and
    names the warning.
15. Given a pull request with a Kotlin compiler warning in any module, when the gate for that module
    runs, then it fails.
16. Given a pull request whose shared code uses a JVM-only API, when the iOS compile runs, then it
    fails.
17. Given a pull request that adds the cleartext permission to the release manifest, when the
    Android gate runs, then it fails.
18. Given a pull request with a failing app test, when the Android gate runs, then it fails and the
    pull request cannot be merged.
19. Given no library change at all, when a newer version of a library is published, then a rerun of
    an earlier green pull request is still green.
20. Given a release pull request from `develop` into `main`, when it runs, then the gates of every
    component changed since the last release run.

### Releases

21. Given the signing secrets are set up and a release that changes the app is merged into `main`,
    when the pipeline finishes, then a GitHub prerelease exists, tagged
    `android-v<versionName>-<versionCode>`, holding a signed APK, a signed app bundle and the
    mapping file.
22. Given that release's APK, when it is installed on a phone over the previous release's APK, then
    the installation succeeds without uninstalling first.
23. Given a crash in that installed release, when it appears in the crash reporting console, then
    its stack trace is readable.
24. Given a release that changes only the backend is merged into `main`, when the pipeline finishes,
    then a new backend image is published and no Android release is created.
25. Given a release that changes only the app is merged into `main`, when the pipeline finishes,
    then no backend image is published.
26. Given a pull request, when its Android gate builds the release, then the build is unsigned and
    no signing secret was available to the job.
27. Given the maintainer has the keystore locally, when they use Android Studio's signing wizard,
    then they get a signed build without any extra configuration.
28. Given a keystore file anywhere in the working tree, when the maintainer runs `git status`, then
    the file is not listed.

### Documentation

29. Given the runbook, when the maintainer follows the one-time setup checklist, then the
    back-merge, branch rules, merge methods, signing environment and secrets are all in place.
30. Given the project instructions, when a new assistant session starts a feature, then the git
    rules it follows are the ones listed in this specification.

## Definition of Done

- All user acceptance tests that need no GitHub setup pass locally; those that need it are listed
  for the maintainer in the pull request description.
- The Android gate's checks pass on the current code: all existing lint findings are fixed and the
  codebase compiles without Kotlin warnings in every module.
- The backend gate checks at least everything it checked before this feature.
- The release build stays unsigned without the signing credentials and is otherwise unchanged for
  pull requests and local builds.
- The project instructions describe the git workflow and the CI gates.
- The operations runbook describes both pipelines, the one-time repository setup and the release
  checklist, and no longer describes tag-triggered app pipelines.
- No regression in existing features; all existing app and server tests pass.

## Out of Scope

- Publishing to Google Play (internal or any other track), the Play Console setup and its service
  account — a separate feature.
- Publishing to TestFlight and anything iOS beyond compiling the shared code; there is no Xcode
  project yet — comes with the iOS feature.
- Automatic deployment of the backend; deployment stays manual with the existing deploy script.
- Automated tests of the pipelines or of the change detection; the pipelines prove themselves on
  the first real pull request and the first release.
- Hotfixes directly to `main`.
- A check that the version name was raised for a release.
- Required approvals on pull requests (GitHub does not let the sole maintainer approve their own).
- The assistant opening, updating or merging pull requests, or storing the pull request text in a
  file.
- Running Android tests on a device or emulator in CI.

## Further Notes

- The workflows cannot be run before they are on GitHub, so the first real check of the pull
  request gates is this feature's own pull request, and of the `main` jobs the next release.
- The one-time back-merge of `main` into `develop` must happen by hand **before** the `develop`
  branch rule is switched on, because a squash merge carries no history and the assistant never
  pushes to `develop`.
- An APK signed with the maintainer's key and a later Play-delivered build of the same app have
  different signatures (Play App Signing), so testers switching between the two must uninstall
  first.
- The existing keystore becomes the upload key when Play publishing is added; it must be kept safe
  outside the repository, because losing it means a new app identity.

---

## Technical Annex
> Written against codebase as of: 2026-10-18

### Architectural Decisions

**Change detection — `.github/scripts/changed-components.sh`**

- The single place for the path → component mapping, called by both workflows. Input: a base and a
  head ref; it lists `git diff --name-only <base>...<head>` (merge-base diff) and prints
  `server=true|false`, `android=true|false`, `ios=true|false` in `$GITHUB_OUTPUT` format.
- Base: on `pull_request` the PR base (`github.event.pull_request.base.sha`); on `push` to `main`
  `github.event.before`. If the base is missing or all zeros (first push, forced history), every
  component is `true`.
- Mapping:

  | Paths | server | android | ios |
  | --- | --- | --- | --- |
  | `server/**`, `deploy/**`, `.github/workflows/server.yml` | ✅ | | |
  | `androidApp/**`, `.github/workflows/android.yml` | | ✅ | |
  | `composeApp/**`, `theme/**` | | ✅ | ✅ |
  | `gradle/**`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `buildSrc/**`, `.github/actions/**`, `.github/scripts/**` | ✅ | ✅ | ✅ |
  | anything else (`*.md`, `.sdd/**`, `.github/dependabot.yml`, `iosApp/**`, …) | | | |

  `android.yml` changing also sets `ios` (the iOS job lives in that workflow). `buildSrc/` currently
  holds only build output; the entry is kept because a `buildSrc` would affect every build.
- Checkout needs `fetch-depth: 0` (or enough depth for the merge base) in the detection job.

**`.github/workflows/server.yml` (rework)**

- `on: pull_request` (branches `develop`, `main`, **no `paths` filter**) and `push` to `main` (no
  `paths` filter). A first job `changes` runs the script; `test-and-scan` gets
  `needs: changes` and `if: needs.changes.outputs.server == 'true'`; `publish` keeps its `main`
  condition and runs only when `test-and-scan` ran and succeeded. Job contents, Trivy settings,
  image tags (`:<sha>`, `:latest`), concurrency and `permissions: contents: read` default unchanged.
- The skipped `test-and-scan` is the required check; a skipped job reports success.

**`.github/workflows/android.yml` (new)**

- Same triggers and `changes` job as `server.yml`. Concurrency group `android-${{ github.ref }}`,
  cancel in progress on PRs only. Default `permissions: contents: read`. Every action pinned by full
  commit SHA with the version in a comment; JDK/Gradle via `./.github/actions/setup-jvm`.
- Job `android-gate` (`ubuntu-24.04`, if `android`):
  `./gradlew :composeApp:testAndroidHostTest :composeApp:checkTranslations :androidApp:lintRelease
  :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`,
  then the two merged-manifest checks from `CLAUDE.md` (each `grep -l` over
  `androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` must
  match only the debug manifest, for `usesCleartextTraffic\|ACCESS_LOCAL_NETWORK` and for
  `firebase_crashlytics_collection_enabled`). On failure, upload the lint report as an artifact.
- Job `ios-compile` (`macos-latest`, if `ios`): setup-jvm, then
  `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`. Konan cache through Gradle's cache is
  sufficient; no Xcode project involved.
- Job `release` (only `push` to `main`, if `android`, `needs: android-gate`):
  - `environment: android-release`; `permissions: contents: write` for this job only.
  - Decodes `ANDROID_KEYSTORE_BASE64` to a file in `$RUNNER_TEMP`, exports its path plus
    `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` to Gradle; fails if any
    secret is empty.
  - `./gradlew :androidApp:assembleRelease :androidApp:bundleRelease` with the mapping upload on
    (default), checkout with full history for the version code.
  - Creates the release with the GitHub CLI preinstalled on the runner
    (`gh release create android-v<versionName>-<versionCode> --prerelease --generate-notes
    --target $GITHUB_SHA` with the APK, AAB and `mapping.txt`), using `GITHUB_TOKEN`. Version name
    and code are read from the build (e.g. a small Gradle task printing them, or the output file
    names).
- No `workflow_dispatch`.

**Release build configuration — `androidApp/build.gradle.kts`**

- `signingConfigs { create("release") { … } }` filled from environment variables read through
  `providers.environmentVariable(...)`: `ANDROID_KEYSTORE_FILE` (path written by CI),
  `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. The `release` build type
  uses it **only when all four are present**; otherwise it stays unsigned exactly as today.
  Android Studio's wizard injects its own signing properties and works unchanged. No Gradle
  properties, no `keystore.properties`.
- `versionCode` = `git rev-list --count HEAD` via `providers.exec` (configuration-cache safe),
  falling back to `1` when git fails. `versionName` stays a literal (`"1.0.0"`), raised by hand.
- Release file names `polleninfo-<versionName>-<versionCode>.apk` / `.aab` are given by the CI
  `release` job when it copies the outputs (AGP 9 has no public API to rename an APK, none for an
  AAB); the build's own output names stay AGP's defaults. Version name and code reach the job
  through a small Gradle task that prints them. PR checkouts are shallow, so their (unsigned)
  builds get version code 1; the commit count is a configuration-cache input, so a new commit
  invalidates the cache entry (expected).
- Configuration cache stays on; no new deprecation or warning.

**Lint**

- `android { lint { warningsAsErrors = true; abortOnError = true; checkDependencies = true;
  disable += setOf("NewerVersionAvailable", "GradleDependency") } }` in `:androidApp`; equivalent
  `lint { }` configuration in the KMP library modules if needed so that `:composeApp`'s
  `androidMain` and `:theme` are analysed (the run on 2026-10-18 only showed
  `:composeApp:lintAnalyzeAndroidHostTest`; verify coverage, e.g. by a deliberate finding in
  `composeApp/src/androidMain`). No baseline file.
- Current findings to fix (as of 2026-10-18, `:androidApp:lintRelease`, 0 errors / 16 warnings):
  `MonochromeLauncherIcon` ×2 (add `<monochrome>` to the adaptive launcher icons),
  `UnusedAttribute` ×1 and `ObsoleteSdkInt` ×1 (manifest / `res/`); the other 12 are the two
  disabled version checks. Any new findings that wider coverage reveals are fixed too.

**Kotlin warnings as errors**

- `compilerOptions { allWarningsAsErrors.set(true) }` at the extension level in `:composeApp`
  (`kotlin { compilerOptions { … } }`, all targets including iOS), `:theme`, `:androidApp` (AGP
  built-in Kotlin) and `:server`. Always on, not CI-only. Existing warnings, if any, are fixed.
  Gradle deprecation messages from AGP/KGP (`Configuration.setVisible`,
  `KotlinNativeBundleArtifactsTypes`) are not Kotlin warnings and stay accepted.

**Repository files**

- `.github/dependabot.yml`: `target-branch: develop` on all four ecosystem entries (gradle, docker, github-actions, docker-compose).
- `.gitignore`: add `*.jks` next to `*.keystore`.

**Repository settings (manual, documented in `deploy/README.md`)**

1. Back-merge first: `git switch develop && git merge main && git push` (main is at `6b5ae40`,
   two commits ahead of `develop` at `6776be9`, identical content).
2. Ruleset `develop`: restrict deletions, block force pushes, require a pull request (0 approvals),
   allowed merge method **squash** only, required checks `Test, build and scan` (server),
   `android-gate`, `ios-compile` (exact names as the jobs' `name:`), require branches up to date: no.
3. Ruleset `main`: same, allowed merge method **merge** only.
4. Repository setting "Automatically delete head branches" on.
5. Environment `android-release`: deployment branches limited to `main`, no required reviewers,
   secrets `ANDROID_KEYSTORE_BASE64` (`base64 -i upload.jks`), `ANDROID_KEYSTORE_PASSWORD`,
   `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

**Documentation**

- `CLAUDE.md`, new section "Git workflow":
  - branch `feature/<sdd-dir>` from freshly fetched `origin/develop` at the first task; stop and
    ask if the tree is dirty or the branch exists; the `.sdd/<name>/` files are the first commit;
  - one commit per finished task, message `<feature>: <task title>`, pushed right away;
  - never push to `develop`/`main`, never force-push (except the own feature branch after a rebase
    the maintainer asked for), never merge, never open, edit or merge pull requests (no `gh`);
  - after the last task: report the compare link
    `https://github.com/timstenzel/PollenInfo/compare/develop...feature/<name>?expand=1`, the title
    `feature: <name>` and a Markdown description (Summary, Changes, Quality gates, For the
    reviewer) — no file, no attribution line;
  - release PRs (`release: <versionName>`, `develop` → `main`) belong to the maintainer.
  - Commands/CI part: the CI gates per component, lint (`:androidApp:lintRelease`) as a local gate,
    `allWarningsAsErrors` replacing "no new warning" by convention (the two accepted plugin
    messages stay listed).
- `deploy/README.md`: CI section covers both workflows and the change detection; the rule "app
  pipelines … triggered by a release tag … never on every push" is replaced; the secrets rule
  becomes "environments limited to `main`; required reviewers only for jobs that publish outward
  (Play, TestFlight later)"; new sections "Repository setup" (the checklist above), "Android release
  signing" (secrets, keystore safe-keeping, signature difference to Play builds), "Cutting a
  release" (optionally raise `versionName` on `develop`, open `develop` → `main` titled
  `release: <versionName>`, merge commit, check the GitHub release and the image, deploy with
  `deploy/deploy.sh <sha>`).

### Automated Testing Decisions

- **No new automated tests**, by decision of the maintainer — neither for the workflows nor for the
  change detection script. The pipelines are verified by this feature's own pull request (gates)
  and the next release (`main` jobs).
- What is verified locally during implementation: the existing suites still pass
  (`./gradlew :composeApp:testAndroidHostTest :server:test`), `:androidApp:lintRelease` is clean,
  every module compiles with `allWarningsAsErrors`, `:composeApp:compileTestKotlinIosSimulatorArm64`
  passes, and `assembleRelease` without signing variables still produces the unsigned build.
- Existing tests that must keep passing and relate to this area: `DeployFilesTest` (deploy files,
  image tag, grace periods), `ServerConfigTest`.
