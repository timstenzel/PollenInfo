# Tasks: User Accounts and Server-Side Diary

Derived from `requirements.md`. The Technical Annex was verified against the codebase on 2026-10-11
(`feature/account`, `a4d5f4d`). One deviation was agreed with the user: account validation gets its
own `AccountValidationResult` (`Valid(AccountSpec)` / `Invalid(message)`) instead of making the
alarm `ValidationResult` generic.

Interim states between tasks are intentional and named in each task (e.g. the login mode before
task 02, Home answers not reaching the chart between tasks 03 and 04). The database is not live, so
`V1__init.sql` is extended in place; reset the development database after each schema change.

Quality gates (apply to every task, repeated per task for checking off):

- **QG-server**: `./gradlew :server:test` passes.
- **QG-app**: `./gradlew :composeApp:testAndroidHostTest` and `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` pass.
- **QG-build**: `./gradlew :androidApp:assembleDebug` passes with no new compiler or deprecation warnings (only the two plugin deprecations documented in CLAUDE.md).
- **QG-l10n**: `./gradlew :composeApp:checkTranslations` passes, and the hard-coded-text grep from CLAUDE.md finds no new hit outside its allowlist.
- **QG-docs**: every CLAUDE.md section named in the task's docs step describes the behaviour as built.
- **QG-logs**: `AccountFlowLoggingTest` (from task 01 on) passes and covers every account endpoint the task added.

---

## Task 01-create-account-from-diary-gate

A signed-out user opening the Diary tab sees the gate: a heading, the explanation of why an account
is needed, and "Log in" / "Create account". "Create account" opens the registration form (email,
password, confirmation, each password with its own eye button, consent). Submitting it creates the
account, stores the session on the phone and returns to the Diary, which loads its content as
today. End to end: the `users` and `sessions` tables, registration validation, Argon2id hashing,
the session token, `POST /users` with the account rate limit, the app's session store (own DataStore
file, excluded from backup), `Screen.Account` in register mode, and the Diary's `SignedOut` state.

**Interim:** the gate's "Log in" opens `Screen.Account(LOGIN)`, which in this task shows only the
mode heading and the switch to "Create account"; login arrives in task 02. Signed in, the Diary
still plots the on-device answers (tasks 03–04 change that).

### Implementation steps

- [x] Extend `V1__init.sql` in place with `users` (incl. `consented_at`) and `sessions` as in the annex; mirror them in the Exposed table definitions.
- [x] Add `account/domain`: `AccountValidation` → `AccountValidationResult` (email trimmed + lowercased, ≤ 254, one `@` with text on each side; password 8–128 code points, never trimmed; consent required), `PasswordHasher` + `Argon2PasswordHasher` (Bouncy Castle via the version catalog, Argon2id, PHC string, constant-time verify, parameters injectable for tests, `verifyDummy`), `SessionToken` (same shape as `DeviceToken`), `UserId`.
- [x] Add `UserStore.register` (stores `consented_at`; unique violation → `EmailTaken`) and `SessionStore.create`, with Exposed implementations.
- [x] Add `POST /users` (`201 {sessionToken}`, `400 {error}`, `409 {error}`) via `configureAccountRouting(...)`, wired in `Application.module`; add `ACCOUNT_LIMIT` (per client address, `RATE_LIMIT_ACCOUNT_PER_HOUR`, default 10) to `RateLimits`, `ServerConfig.load` and `configureSecurity`.
- [x] Add `AccountFlowLoggingTest` (modelled on `DeviceFlowLoggingTest`) covering registration.
- [x] App: add `core/session` — `Session(token, email)`, `SessionRepository` (+`Impl`) with `session` flow, `register`, `clear`; `AccountApiService` mapping `409` → `EmailTakenException`; the logic-free session DataStore under a new `SESSION_DATA_STORE` qualifier (`polleninfo_session`) in both platform modules; add `datastore/polleninfo_session.preferences_pb` to `data_extraction_rules.xml` (cloud backup and device transfer) and `backup_rules.xml`; `FakeSessionRepository`.
- [x] App: add `AppError.EmailTaken`, `ErrorContext.REGISTER` and their `error_*` texts; the account exception mapping (`toSessionAppError()`).
- [x] App: add `feature/account` — pure `AccountFormState` (modes, fields, two independent visibility toggles, mismatch only once the confirmation has text, `canSubmit`, mode switch keeps the email), `AccountViewModel` (`isSubmitting`, `error` cleared by the next edit, `AccountEvent.Done`), `AccountScreen` (both password fields with eye buttons announced "Show/Hide password" with shown/hidden state, minimum-length hint, the line that a forgotten password cannot be recovered, consent row as one toggleable node, progress in the button, mode switch); register `Screen.Account(mode)` in `AppNavigation`, hidden from the bottom bar.
- [x] App: `DiaryViewModel` takes `SessionRepository`; `DiaryUiState.SignedOut` (nothing fetched while signed out; a session appearing loads the content); the gate screen with heading, explanation (answers are health information kept in the account, survive a lost or replaced phone, appear on every device logged in, email only used to log in) and both buttons.
- [x] All new strings in all four languages.
- [x] Docs: CLAUDE.md — REST API, Persistence (tables), Configuration, Abuse protection, `core/` table (`core/session`), Persisted user selections (session file, backup exclusion), Diary (gate), Navigation (`Screen.Account`), Localization keys (`account_*`, `diary_gate_*`).

### Acceptance criteria

- [x] `AccountValidationTest` pins email 254/255 characters, capitals and surrounding spaces normalised, password 7/8 and 128/129, missing consent; `AccountRoutesTest`: a valid `POST /users` is `201` with a 43-character token, each invalid case is `400 {error}`, and an email already registered in other capitals is `409`.
- [x] `ExposedAccountStoresTest` (Postgres): a registered user has `consented_at` set; two concurrent registrations of one email yield exactly one `Registered`. `PasswordHasherTest` (salted, verify right/wrong, parameters read from the string, malformed hash → false) and `SessionTokenTest` (shape, parse, hidden `toString`) pass. `MigrationTest` covers `users` and `sessions` for the app role.
- [x] `SecurityTest`: the eleventh `POST /users` from one address within an hour is `429` with `Retry-After`; `ServerConfigTest` covers `RATE_LIMIT_ACCOUNT_PER_HOUR` (default, valid, invalid); `AccountFlowLoggingTest` finds neither email, password nor token in the log.
- [x] App tests: `SessionRepositoryImplTest` (`MockEngine`) — `register` sends email, password and `consent: true`, stores token and email, maps `409` to `EmailTaken` and a transport failure to `Network`; `AccountFormStateTest` — every register rule of `canSubmit`, mismatch shown only with a non-empty confirmation, independent toggles, mode switch keeps the email; `AccountViewModelTest` — `isSubmitting` under a gate, `Done` exactly once, error cleared by the next edit; `AppErrorTextTest` — `EmailTaken` and `REGISTER`; `TopLevelDestinationTest` — no tab on `Screen.Account`.
- [x] `DiaryViewModelTest`: signed out → `SignedOut` with no station, species or history request; a session appearing → the existing load.
- [x] Both Android backup rule files name `datastore/polleninfo_session.preferences_pb`.
- [ ] ~~Manual (emulator against `:server:run`): the gate shows the four explanation points; Create account → valid form → back on the Diary with the chart loading; still logged in after an app restart; with the server stopped the form says the server could not be reached; TalkBack announces every field, both eye buttons with shown/hidden, the consent row and the buttons; "Log in" opens the interim login mode.~~ *(skipped: TalkBack could not be driven here. Everything else verified on the API 37 emulator against `:server:run` — gate with four points, interim login mode, mismatch hint and disabled button, independent eye toggles, offline message, registration back to the loaded Diary, still logged in after force-stop; the accessibility tree shows the field labels, "Show/Hide password" and "Show repeated password" names and the consent row as one checkable node, but the spoken shown/hidden state needs a TalkBack check by hand)*

### Quality gates

- [x] QG-server
- [x] QG-app
- [x] QG-build
- [x] QG-l10n
- [x] QG-docs
- [x] QG-logs

---

## Task 02-log-in-and-log-out

A user with an account logs in from the Diary gate (or from Settings while signed out), sees
"Logged in as …" in a new Settings "Account" section, and logs out there after a confirmation; the
Diary then shows the gate again. A session the server no longer knows (expired after 90 days unused,
or ended elsewhere) signs the app out on the next authenticated call. End to end: the `user` bearer
provider with expiry and daily refresh, `POST /users/session` with an identical `401` for unknown
email and wrong password, `DELETE /users/me/session`, the per-user rate limit, the app's `login` /
`logout` and `401` handling, the login mode of `Screen.Account`, and Settings' Account section.

### Implementation steps

- [x] Server: `SessionStore.authenticate` (unknown or unused for more than 90 days → `null`; `last_used_at` refreshed at most once per day) and `SessionStore.delete`.
- [x] Server: restructure authentication so one `Authentication` install registers both the `device` provider (unchanged) and the new `user` provider (`UserPrincipal(userId, token)`; shape check before lookup; unparseable header, other scheme, unknown or expired token → the same `401` with `WWW-Authenticate: Bearer`, before the body is read). Route tests installing only one side keep working.
- [x] Server: `UserStore.credentials(email)`; `POST /users/session` (`201 {sessionToken}`; `401 {error}` identical for unknown email and wrong password, `verifyDummy` for an unknown email; `400` malformed) under `ACCOUNT_LIMIT`; `DELETE /users/me/session` (`204`, only the calling session); a per-user limiter around `/users/me/…` inside `authenticate("user")`, keyed on `UserPrincipal` (`RATE_LIMIT_USER_PER_MINUTE`, default 60).
- [x] App: `SessionRepository.login` (`401` → `InvalidCredentialsException`) and `logout` (request, then clear locally even if it fails); `SignedOutException` for a `401` on any authenticated call, which calls `clear()`; `AppError.InvalidCredentials`, `AppError.SignedOut`, `ErrorContext.LOGIN` with texts.
- [x] App: `Screen.Account` login mode — email and password with eye button, `canSubmit`, progress, error, success → `Done`.
- [x] App: `SettingsViewModel` takes `SessionRepository`; Account section under "Default station": signed in "Logged in as {email}" + "Log out" (confirm dialog saying the diary stays in the account), signed out "Not logged in" + "Log in" → `Screen.Account(LOGIN)`.
- [x] App: the Diary switches to `SignedOut` and drops held content when the session becomes `null`.
- [x] Extend `AccountFlowLoggingTest` with login and logout; strings in four languages.
- [x] Docs: CLAUDE.md — REST API, Devices-and-alarms/authentication paragraph (two providers), Abuse protection, Configuration, Error handling (new `AppError`s), Settings.

### Acceptance criteria

- [x] `ExposedAccountStoresTest`: `authenticate` accepts a session unused exactly 90 days and refuses it 1 ms later; `last_used_at` changes at most once per day of use.
- [x] `AccountRoutesTest`: right credentials (email in any capitals) → `201` and a token accepted by `DELETE /users/me/session`; wrong password and unknown email return identical `401` bodies; after logout that token is `401` while a second session of the same user still works; a missing, malformed or unknown token is `401` with `WWW-Authenticate: Bearer`; every existing `AlarmRoutesTest` case still passes.
- [x] `SecurityTest`: login shares the `ACCOUNT_LIMIT` bucket with registration; the per-user limit returns `429` after 60 calls a minute for one user and not for another; `ServerConfigTest` covers `RATE_LIMIT_USER_PER_MINUTE`.
- [x] App tests: `SessionRepositoryImplTest` — `login` stores token and email and maps `401` to `InvalidCredentials`; `logout` sends the bearer `DELETE` and clears the session for `204` and for a failure; `AccountViewModelTest` — login submit, error and `Done` once; `SettingsViewModelTest` — account row signed in and out, the logout dialog, confirming logs out; `AppErrorTextTest` — `InvalidCredentials`, `SignedOut`, `LOGIN`.
- [x] `DiaryViewModelTest`: the session going to `null` while `Content` is shown → `SignedOut`; signing in again reloads.
- [ ] ~~Manual: log in from the Diary gate and from Settings; log out in Settings → the Diary shows the gate; the confirmation and the Account rows read correctly with TalkBack.~~ *(skipped: TalkBack could not be driven here. Everything else verified on the API 37 emulator against `:server:run` — Settings signed out shows "Not logged in" + "Log in"; login from Settings with a wrong password shows "Couldn’t log in. The email address or password is incorrect.", the right one with the email in other capitals returns to Settings showing "Logged in as manual@example.com"; the Diary then loads; "Log out" opens the confirmation with "Your diary stays in your account…", confirming sends `DELETE /users/me/session` (204) and the Diary shows the gate; logging in from the gate returns to the loaded Diary; the server log holds neither the email nor the password. The accessibility tree shows the row texts and dialog, but the spoken output needs a TalkBack check by hand)*

### Quality gates

- [x] QG-server
- [x] QG-app
- [x] QG-build
- [x] QG-l10n
- [x] QG-docs
- [x] QG-logs

---

## Task 03-feeling-answers-saved-to-account

Home's "How do you feel today?" prompt saves to the account. Signed in, it appears only once the
backend confirms today has no answer (and today is not dismissed); a tap sends the answer, which the
server files under today's Swiss date and never overwrites. Signed out, it appears unless dismissed,
and a tap opens `Screen.Account(LOGIN)` with nothing saved. "Not today" stays on the device. End to
end: the `diary_entries` table, `POST /users/me/diary` and `GET /users/me/diary/today`, the new
remote `DiaryRepository` (`record`, `today`), the local `FeelingPromptPreferences`, and the reworked
`HomeViewModel`.

**Interim:** the existing local repository is renamed `LocalDiaryRepository` and reduced to its
read-only `entries` flow, which only `DiaryViewModel` uses until task 04 deletes it. Answers given on
Home in this state do not reach the chart.

### Implementation steps

- [x] Server: `diary_entries` in `V1__init.sql` + table definition; `DiaryFeeling`; `DiaryStore.record` (`INSERT … ON CONFLICT DO NOTHING` + read → `Recorded` / `AlreadyAnswered`) and `entry(user, date)`.
- [x] Server: `POST /users/me/diary` (`201` / `200` with the stored entry / `400` unknown feeling) and `GET /users/me/diary/today` (`200` / `204`), date from `SWISS_ZONE` and the injected clock.
- [x] App: rename the local interface to `LocalDiaryRepository` (read-only `entries`), move the dismissal to `FeelingPromptPreferences` (logic-free DataStore in the shared store, `FakeFeelingPromptPreferences`), drop the local `record`.
- [x] App: `DiaryApiService`; new `DiaryRepository` with `record(feeling): Result<DiaryEntry>` and `today(): Result<DiaryEntry?>` through `DiaryRepositoryImpl(api, sessions)` (no session → `SignedOutException` without a request; `401` → `clear()`); new `FakeDiaryRepository` with `recordGate` / `todayGate`; `ErrorContext.SAVE_FEELING`.
- [x] App: `HomeViewModel(selectedStationRepository, getStationMeasurement, sessionRepository, diaryRepository, feelingPromptPreferences, clock)` — signed out: prompt shown unless dismissed, a tap sends `HomeEvent.OpenAccount` (the screen navigates to `Screen.Account(LOGIN)`); signed in: hidden until `today()` answers, shown for `null`, hidden for an entry or a failure, re-checked on load, refresh and every session change; `feelingSaveError: AppError?`.
- [x] Extend `AccountFlowLoggingTest` with an answer; strings in four languages.
- [x] Docs: CLAUDE.md — The feeling prompt, REST API, Persistence (`diary_entries`), Persisted user selections ("Diary answers": answers now leave the device; dismissal stays), `core/diary` row.

### Acceptance criteria

- [x] `AccountRoutesTest`: the first `POST /users/me/diary` of a Swiss day is `201` with that date; a second with another feeling is `200` returning the first; with the `MutableClock` at 23:59:59 and 00:00:00 Swiss time the two answers get different dates; `/today` is `204` before and `200` after; another user's `/today` stays `204`; an unknown feeling is `400`; a request without a token is `401` before the body is read.
- [x] `ExposedAccountStoresTest`: two concurrent `record`s for one user and day store exactly one entry, the first; `MigrationTest` covers `diary_entries`.
- [x] `DiaryRepositoryImplTest` (`MockEngine`): `record` succeeds for `201` and `200`, `today` maps `200` and `204`, no request without a session, a `401` clears the session and fails with `SignedOut`; `AppErrorTextTest` covers `SAVE_FEELING`.
- [x] `HomeViewModelTest` signed out: prompt shown unless dismissed (a dismissal from an earlier "session" of the fake preferences still hides it), a feeling tap emits `OpenAccount` once and records nothing, the prompt is still shown afterwards.
- [x] `HomeViewModelTest` signed in: prompt hidden while `today()` is gated, shown for `null`, hidden for an entry and for a failure; a successful `record` hides it; a failed `record` keeps it with its `AppError`, cleared by the next attempt; a `401` on `record` leaves the app signed out so the next tap emits `OpenAccount`; a session change re-checks.
- [x] Manual: answering on emulator A hides the prompt on emulator B with the same account after a pull-to-refresh; with no network the prompt stays up with the error; "Not today" survives an app restart; a signed-out tap opens login and Back returns to Home with the prompt still shown. *(API 37 emulator against `:server:run` on a reset database. Only one emulator was used: the second phone was a second session of the same account, driven over the REST API with curl — the same calls a second phone sends. Results: an answer from that session followed by a pull-to-refresh hid the prompt on the emulator (`/today` 204, then 200); an answer tapped on the emulator for another account was returned by that account's other session's `/today`; in airplane mode the prompt stayed up with "Your answer couldn’t be saved. Couldn’t reach the server. …"; "Not today" was still in force after a force-stop and restart; a signed-out tap on "Bad" opened Log in, Back returned to Home with the prompt up, and `diary_entries` stayed empty. The server log held no email, password or token.)*

### Quality gates

- [x] QG-server
- [x] QG-app
- [x] QG-build
- [x] QG-l10n
- [x] QG-docs
- [x] QG-logs

---

## Task 04-diary-chart-from-account

The Diary's feeling line comes from the account. Every history load (first open, Retry, station or
range change) fetches the pollen history and the account's answers for the same window in parallel;
either failing is `Error` with Retry, and a `401` returns to the gate. The on-device answer store is
removed. End to end: `GET /users/me/diary?from&until`, `DiaryRepository.entries`, the reworked
`DiaryViewModel`, and deleting `LocalDiaryRepository`, `DiaryCodec` and `DataStoreDiaryRepository`.

### Implementation steps

- [ ] Server: `DiaryStore.entries(user, from, until)` and `GET /users/me/diary?from&until` (`200` sorted, answered days only; `400` missing, unparseable, `until < from` or more than 366 days).
- [ ] App: `DiaryRepository.entries(from, until)`; `FakeDiaryRepository` gains scripted entries and a failure.
- [ ] App: `DiaryViewModel` takes `DiaryRepository` instead of `LocalDiaryRepository`; it computes the window as the server does (`range` days ending yesterday in `Europe/Zurich`), fetches history and entries in parallel, filters entries to `history.from..history.until` and before today as before, maps either failure to `Error`, `SignedOutException` to `SignedOut`.
- [ ] App: delete `LocalDiaryRepository`, `DiaryCodec`, `DataStoreDiaryRepository`, `List<DiaryEntry>.recording` and their tests (`DiaryCodecTest`, the recording cases of `DiaryEntryTest`).
- [ ] Extend `AccountFlowLoggingTest` with a range read.
- [ ] Docs: CLAUDE.md — Diary, `core/diary` row, Persisted user selections ("Diary answers" rewritten), Architecture (answers stored by the backend), Testing (fixtures).

### Acceptance criteria

- [ ] `AccountRoutesTest`: `GET /users/me/diary` returns only the caller's answered days within the range, sorted by date; a 366-day range is `200`; 367 days, `until < from`, an unparseable date and a missing parameter are `400`.
- [ ] `DiaryRepositoryImplTest`: `entries` sends `from` and `until` as ISO dates with the bearer header and maps the list; `401` → `SignedOut`.
- [ ] `DiaryViewModelTest`: history and entries are requested together on first load, Retry, station change and range change; with the clock at 23:59 and 00:01 Swiss time the requested `until` is the respective yesterday; entries failing while history succeeds → `Error`, and Retry recovers; a `401` → `SignedOut`; `hasNoEntries` still drives the hint.
- [ ] `grep -rn "DiaryCodec\|DataStoreDiaryRepository\|LocalDiaryRepository" composeApp/src` finds nothing.
- [ ] Manual: answers on earlier days (seeded through the API) appear on the feeling line for week, month and year at two stations; after logging out and in again the same answers are back.

### Quality gates

- [ ] QG-server
- [ ] QG-app
- [ ] QG-build
- [ ] QG-l10n
- [ ] QG-docs
- [ ] QG-logs

---

## Task 05-delete-account

A signed-in user deletes their account in Settings: a warning that the account and every answer are
deleted permanently, the password entered again, and on success they are signed out and everything
is gone. A wrong password keeps the dialog with its message. The Impressum says deletion can also be
requested by email. End to end: `DELETE /users/me` (`403` for a wrong password), the cascade,
`SessionRepository.deleteAccount`, and the Settings dialog.

### Implementation steps

- [ ] Server: `UserStore.delete` (cascades to sessions and diary entries); `DELETE /users/me` with `{password}` under the `user` provider — `204`, `403 {error}` wrong password, `400` malformed.
- [ ] App: `SessionRepository.deleteAccount(password)` (`403` → `InvalidCredentialsException`, session kept; `204` clears it); `ErrorContext.DELETE_ACCOUNT`.
- [ ] App: Settings "Delete account" row and dialog (warning, password field with eye button, confirm with progress, `deleteError`); `settings_account_deletion_note` with the contact address in the Impressum.
- [ ] Extend `AccountFlowLoggingTest` with the deletion; strings in four languages.
- [ ] Docs: CLAUDE.md — REST API, Persistence (cascade), Settings.

### Acceptance criteria

- [ ] `AccountRoutesTest`: a wrong password is `403` and the account keeps working; the right one is `204`, after which every session of that user is `401`, login fails, and registering the same email creates a new account whose `/today` is `204`.
- [ ] `ExposedAccountStoresTest`: deleting a user removes its sessions and diary entries and leaves another user's untouched.
- [ ] `SessionRepositoryImplTest`: `403` → `InvalidCredentials` with the session kept; `204` clears the session; `AppErrorTextTest` covers `DELETE_ACCOUNT`.
- [ ] `SettingsViewModelTest`: a wrong password keeps the dialog with its error; `isDeleting` under a gate; success closes the dialog and shows the signed-out row.
- [ ] Manual: delete in Settings → the Diary shows the gate; TalkBack reads the warning, the password field and its eye button; the Impressum note is present.

### Quality gates

- [ ] QG-server
- [ ] QG-app
- [ ] QG-build
- [ ] QG-l10n
- [ ] QG-docs
- [ ] QG-logs

---

## Task 06-password-manager

After a successful registration or login the phone offers to save the email and password, and the
login form offers a saved PollenInfo login when it opens. Fields carry autofill hints; iOS gets the
hints only. End to end: `CredentialStore` (Android Credential Manager, iOS no-op), its use in
`AccountViewModel`, and the autofill content types on `AccountScreen` and the delete dialog.

### Implementation steps

- [ ] Add `androidx.credentials` and its Play-services provider to the catalog and `androidMain`; define `CredentialStore` (`save(email, password)`, `offer(): SavedCredential?`) and bind it per platform — a Koin binding if an application context suffices, otherwise a `@Composable expect fun rememberCredentialStore()` handed to the ViewModel; every Credential Manager exception is swallowed and logged; iOS no-op actual; `FakeCredentialStore`.
- [ ] `AccountViewModel`: `save` after a successful register or login, before `Done`, never blocking it; on first open in login mode `offer()`, and a returned credential fills the form and submits.
- [ ] Content types: email/username, password, new password (register) on `AccountScreen` and password on the delete dialog.
- [ ] Docs: CLAUDE.md — `core/` table (`core/credentials`), iOS wrapper configuration (password autofill, Associated Domains), Firebase (Credential Manager is the second Play-services dependency).

### Acceptance criteria

- [ ] `AccountViewModelTest`: `save` is called once with the submitted email and password after a successful register and after a successful login, and never after a failure; a throwing `save` still emits `Done`.
- [ ] `AccountViewModelTest`: an offered credential fills the form and logs in; no offer leaves the form empty; register mode never calls `offer`.
- [ ] The iOS sources compile with the no-op actual (QG-app).
- [ ] Manual (device with Google Password Manager): registering shows the system save sheet and "Not now" leaves the user logged in; after logging out, opening the login form offers the saved login and choosing it logs in.

### Quality gates

- [ ] QG-app
- [ ] QG-build
- [ ] QG-l10n
- [ ] QG-docs

---

## Task 07-session-cleanup-rate-limit-message-and-release-check

Expired sessions are deleted once a day, a rate-limited login or registration says "Too many
attempts, try again later" in the app, and the whole feature is checked end to end in all four
languages with no regression for users without an account. End to end: `SessionStore.pruneExpired`
as the scheduler's third daily step, the app's mapping of `429`, the accepted limitations in the
docs, and the final walk-through.

### Implementation steps

- [ ] Server: `SessionStore.pruneExpired(now)`; `AlarmScheduler` takes the `SessionStore` and runs `pruneExpiredSessions` on a Swiss day's first tick with its own day marker and `try`, like `pruneInactiveDevices`; wire it in `Application.module`.
- [ ] App: map `429` from the account calls to a too-many-attempts error (an `AppError` case or `ErrorText` reason) with texts in four languages.
- [ ] Docs: CLAUDE.md — Alarm delivery (third prune), Testing, and the accepted limitations (no password reset or email verification; registration reveals whether an email has an account; a tapped answer is not kept through login).

### Acceptance criteria

- [ ] `ExposedAccountStoresTest`: `pruneExpired` removes sessions unused for more than 90 days, keeps one at exactly 90 days, and returns the count.
- [ ] `AlarmSchedulerTest`: the session prune runs on a Swiss day's first tick and not again that day; a throwing prune still lets the minute's report through and is retried on the next tick.
- [ ] `SessionRepositoryImplTest` / `AppErrorTextTest`: a `429` on register and on login maps to the too-many-attempts error and its text.
- [ ] Manual: eleven quick login attempts show the too-many-attempts message.
- [ ] Manual release check: in German, French and Italian the gate, registration, login, Home's prompt error and Settings' account section are fully translated; a fresh install completes onboarding, Home, All stations and Alarms without an account, and a due alarm still arrives while signed out; after `adb shell bmgr backupnow` and a restore onto a fresh install the app is signed out.

### Quality gates

- [ ] QG-server
- [ ] QG-app
- [ ] QG-build
- [ ] QG-l10n
- [ ] QG-docs
- [ ] QG-logs
