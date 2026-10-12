# User Accounts and Server-Side Diary

## Problem Statement

PollenInfo's diary — the user's daily answer to "How do you feel today?", compared in a chart with
the pollen levels at a station — is kept only on the phone. Replacing, losing or resetting the phone,
clearing the app's data or reinstalling erases every answer, and there is no way to see the same
diary on a second device. Because the answers are health information collected over whole seasons
and years, losing them silently is the worst outcome the diary can have.

## Solution

Users can create a personal account with their email address and a password, and log in to it on
any phone. Their diary answers are stored in that account on the PollenInfo backend instead of on
the device.

The app stays usable without an account: onboarding, Home's pollen readings, All stations and
Alarms work exactly as before. The account is asked for only where it is needed:

- **The Diary tab**, when nobody is logged in, explains why an account is needed and offers to log
  in or create one. Once logged in it shows the same chart as today, with the answers loaded from
  the account.
- **Home's feeling prompt** still appears once per day. A logged-in user's answer is sent to the
  account; tapping an answer while logged out opens the login screen instead.
- **Settings** shows which account is logged in, and lets the user log out or delete the account
  with all its answers.

When creating an account or logging in, the phone offers to save the password in its password
manager, and on the next login offers the saved one. Registration asks for explicit consent to store
the answers. Email verification and password reset are not part of this feature; a forgotten
password means a lost account, and the app says so plainly where it matters.

## User Stories

### Using the app without an account

1. As a new user, I want to finish onboarding without creating an account, so that I can see pollen levels straight away.
2. As a user without an account, I want Home, All stations, Alarms and Settings to work as before, so that the account only matters for the diary.
3. As a user without an account, I want my alarms to keep working, so that creating an account is never required for notifications.
4. As a user without an account, I want to still see the daily "How do you feel today?" prompt on Home, so that I am reminded the diary exists.
5. As a user without an account, I want tapping an answer on the prompt to take me to the login screen, so that I learn I need an account to save it.
6. As a user without an account, I want "Not today" on the prompt to still close it for the day, so that I am not asked again today on this phone.
7. As a user who backs out of the login screen without logging in, I want to return to Home with the prompt unchanged, so that nothing happened that I did not choose.

### The Diary gate

8. As a user without an account, I want the Diary tab to tell me that the diary needs an account, so that I am not shown an empty or broken chart.
9. As a user without an account, I want the Diary tab to explain why an account is needed — that the answers are health information kept safely in my account, survive a lost or replaced phone and appear on every device I log in on — so that I can decide whether I want one.
10. As a user without an account, I want to be told that my email address is only used to log in, so that I am not worried about marketing mail.
11. As a user without an account, I want a "Log in" and a "Create account" button on the Diary tab, so that I can start either directly.
12. As a user who just logged in or registered from the Diary tab, I want to land back on the Diary with my chart loading, so that I see what I came for.

### Creating an account

13. As a new user, I want to create an account with my email address and a password, so that my diary is kept safely.
14. As a new user, I want to enter my password twice, so that a typo does not lock me out of my account.
15. As a new user, I want a show/hide button on each password field, so that I can check what I typed.
16. As a new user, I want to be told when the two passwords do not match, so that I can fix it before submitting.
17. As a new user, I want to be told the minimum password length, so that I can choose a valid password the first time.
18. As a new user, I want to agree explicitly that my daily answers are stored in my account, so that I know what I am consenting to.
19. As a new user, I want the "Create account" button to stay disabled until my email, password, confirmation and consent are all valid, so that I cannot submit a form that will be refused.
20. As a new user, I want to be told when an account with my email already exists, so that I can log in instead.
21. As a new user, I want to be logged in right after creating my account, so that I do not have to enter my details twice.
22. As a new user, I want my email address treated without regard to capitals, so that "Tim@GMX.ch" and "tim@gmx.ch" are the same account.
23. As a new user, I want to switch from "Create account" to "Log in" without losing the email I typed, so that I do not retype it.

### Logging in

24. As a returning user, I want to log in with my email address and password, so that I see my diary on this phone.
25. As a returning user on a new phone, I want my whole diary history to appear after logging in, so that changing phones loses nothing.
26. As a returning user, I want one clear message when my email or password is wrong, so that I know to try again.
27. As a returning user, I want to stay logged in across app restarts, so that I do not log in every day.
28. As a returning user who has not opened the app for a long time (over 90 days), I want to be asked to log in again, so that a forgotten phone does not stay logged in forever.
29. As a user with two phones, I want to be logged in on both at once, so that I can answer on whichever I have with me.
30. As a user, I want the form to show that it is working while it logs me in or creates my account, so that I do not tap twice.
31. As a user with no connection, I want to be told that the server could not be reached, so that I know it is not my password.

### Password manager

32. As a user, I want the phone to offer to save my email and password after I create an account or log in, so that I do not have to remember them.
33. As a user, I want to be able to decline saving the password, so that nothing is stored that I did not want.
34. As a user who saved a password, I want the login screen to offer my saved PollenInfo login, so that logging in again is one tap.
35. As a user, I want password managers to recognise the email and password fields, so that autofill and strong-password suggestions work.

### Answering how I feel

36. As a logged-in user, I want my answer on Home to be saved to my account, so that it appears in my diary on every phone.
37. As a logged-in user, I want the prompt to disappear once I have answered, so that I know the answer was saved.
38. As a logged-in user, I want an answer to be filed under today's Swiss date, so that it lines up with the pollen levels it is compared with.
39. As a logged-in user who already answered today on another phone, I want this phone not to ask me again, so that I answer once per day.
40. As a logged-in user, I want my first answer of a day to be final, so that a second tap or a second phone can never overwrite it.
41. As a logged-in user whose answer could not be saved, I want the prompt to stay up with a message saying so, so that I can try again.
42. As a logged-in user, I want the prompt to stay hidden while the app is still checking whether I already answered today, so that it does not flash up for a day I already answered.
43. As a logged-in user whose session has expired, I want my next tap on the prompt to take me to the login screen, so that I can log in and answer.

### The diary chart

44. As a logged-in user, I want the Diary to show my feeling line together with the pollen levels exactly as before, so that the comparison still works.
45. As a logged-in user, I want my answers to load for the range and station I choose, so that the chart always matches what I selected.
46. As a logged-in user, I want a clear error with "Retry" when my answers could not be loaded, so that a missing feeling line is never mistaken for "no answers".
47. As a logged-in user without any answers in the shown range, I want the existing hint to answer the prompt on Home, so that I know how to fill the chart.
48. As a logged-in user whose session has expired, I want the Diary to show the login screen state again, so that I can log back in.

### Settings

49. As a logged-in user, I want Settings to show which email address is logged in, so that I know which account I am using.
50. As a logged-in user, I want to log out from Settings, so that someone else using this phone cannot see my diary.
51. As a logged-in user, I want to confirm before logging out, so that I do not log out by accident.
52. As a logged-in user, I want to be reassured that logging out keeps my diary in my account, so that I am not afraid to log out.
53. As a user who just logged out, I want the Diary to show the account screen state and no longer show my answers, so that my health information is not visible on the phone.
54. As a user who logged out, I want Home's prompt and my alarms to keep working as for a user without an account, so that logging out takes nothing else away.
55. As a user without an account, I want Settings to say "Not logged in" and offer "Log in", so that I can find the login from there too.
56. As a logged-in user, I want to delete my account and all its answers from Settings, so that I stay in control of my health information.
57. As a logged-in user, I want a clear warning that deleting the account cannot be undone, so that I do not lose my diary by accident.
58. As a logged-in user, I want to re-enter my password to confirm deleting my account, so that someone holding my unlocked phone cannot delete it with two taps.
59. As a user who cannot open the app any more, I want to read in the legal section that I can request deletion by email, so that I can still have my data removed.

### Privacy and security

60. As a user, I want my password never stored in readable form, so that a leak of the server's data does not reveal it.
61. As a user, I want my email address, password and login never written to server logs, so that operators do not see them.
62. As a user, I want the login not to reveal whether an email address has an account, so that strangers cannot test addresses through it.
63. As the operator, I want repeated login and registration attempts from one address to be limited, so that passwords cannot be guessed at scale.
64. As the operator, I want expired logins to be cleaned up automatically, so that the server does not keep useless secrets.
65. As a user, I want my account and diary to be kept however long I do not use them, so that a skipped season does not erase my history.
66. As a user, I want my login on a phone excluded from cloud backups and device transfers, so that a restored backup does not carry my login to another device.

### Languages and accessibility

67. As a user who reads German, French or Italian, I want every new screen, message and dialog in my language, so that the account feature reads like the rest of the app.
68. As a screen-reader user, I want every field, toggle, checkbox and button on the account screens properly named and announced, so that I can register and log in without sight.
69. As a screen-reader user, I want the show/hide buttons to announce whether the password is shown, so that I know what is being read out.

## User Acceptance Tests

### Without an account

1. Given a fresh install, when the user completes onboarding, then Home appears without any request to create an account.
2. Given a user without an account, when they open All stations, Alarms and Settings, then each works as it did before this feature.
3. Given a user without an account who has not answered or dismissed today, when they open Home, then the "How do you feel today?" prompt is shown.
4. Given a user without an account on Home, when they tap "Bad" on the prompt, then the login screen opens, and no answer is stored anywhere.
5. Given that login screen opened from Home, when the user goes back without logging in, then Home is shown with the prompt still visible.
6. Given a user without an account on Home, when they tap "Not today", then the prompt disappears and does not return that day after restarting the app.
7. Given a user without an account, when they open the Diary tab, then they see a heading saying the diary needs an account, the explanation of why, and "Log in" and "Create account" buttons — and no chart.
8. Given a user without an account with existing alarms, when an alarm is due, then its notification still arrives.

### Creating an account

9. Given the Diary gate, when the user taps "Create account", then the registration form opens with email, password, password confirmation and a consent checkbox.
10. Given the registration form, when the password and its confirmation differ, then a "passwords don't match" message is shown under the confirmation and "Create account" is disabled.
11. Given the registration form with a password of 7 characters, then "Create account" is disabled; with 8 characters, a matching confirmation, a valid email and the consent checked, it is enabled.
12. Given a valid registration form with the consent unchecked, then "Create account" is disabled.
13. Given each password field, when the user taps its eye button, then that field's text becomes visible and the other field is unaffected; tapping again hides it.
14. Given a valid registration form, when the user taps "Create account", then a progress indicator is shown, the fields are disabled, and on success the user returns to where they came from, logged in.
15. Given an account for "tim@example.com" already exists, when someone registers "Tim@Example.com", then they are told an account with this email already exists.
16. Given the registration form with an email entered, when the user switches to "Log in", then the email field still holds that email.
17. Given an Android phone with a password manager, when registration succeeds, then the system offers to save the email and password; declining it leaves the user logged in.

### Logging in

18. Given an existing account, when the user logs in with the right email and password, then they return to the previous screen, logged in.
19. Given an existing account, when the user logs in with a wrong password, then "Email or password is incorrect" is shown; logging in with an email that has no account shows exactly the same message.
20. Given an existing account, when the user logs in with the email in different capitals, then the login succeeds.
21. Given no network connection, when the user tries to log in, then a message says the server could not be reached.
22. Given a logged-in user, when they close and reopen the app, then they are still logged in.
23. Given an Android phone with a saved PollenInfo login, when the login form opens, then the system offers the saved login, and choosing it logs the user in.
24. Given the same account, when the user logs in on two phones, then both stay logged in and both show the same diary.
25. Given eleven login attempts from one network address within an hour, then the eleventh is refused with a message to try again later.

### Answering how I feel

26. Given a logged-in user who has not answered today, when they open Home, then the prompt is shown; when they tap "Good", then the prompt disappears.
27. Given that answer, when the user opens the Diary on the next day, then the "Good" answer appears on the feeling line for the day it was given.
28. Given a user who answered today on phone A, when they open Home on phone B with the same account, then the prompt is not shown on phone B.
29. Given a logged-in user who answered "Good" today, when another phone of the same account sends "Very bad" for today, then the stored answer stays "Good".
30. Given a logged-in user without network, when they tap an answer, then the prompt stays up with a message that the answer could not be saved; with the network back, tapping again saves it.
31. Given a logged-in user whose login was ended on the server (for example by deleting the account on another phone), when they tap an answer, then the app treats them as logged out and the next tap opens the login screen.
32. Given a user logs in through the Diary while Home is still open in the background, when they return to Home, then the prompt reflects the account's answer for today.

### The diary chart

33. Given a logged-in user with answers in the last month, when they open the Diary, then the chart shows the pollen lines and their feeling line as before this feature.
34. Given the Diary open, when the user switches the range to "Year" or chooses another station, then the chart reloads with the answers for that window.
35. Given a logged-in user, when the answers cannot be loaded but the pollen history can, then the Diary shows an error with "Retry" instead of a chart without a feeling line.
36. Given a logged-in user with no answers in the shown range, then the chart shows the hint to answer the prompt on Home.
37. Given a logged-in user on the Diary, when they log in or register from the Diary gate, then the Diary loads its chart without leaving the tab.

### Settings

38. Given a logged-in user, when they open Settings, then the Account section shows "Logged in as" with their email, a "Log out" row and a "Delete account" row.
39. Given a logged-in user, when they tap "Log out", then a confirmation says the diary stays in the account; confirming logs them out.
40. Given a user who just logged out, when they open the Diary, then the gate is shown and none of their answers are visible.
41. Given a user who just logged out, when they log in again, then all their earlier answers are back in the Diary.
42. Given a user without an account, when they open Settings, then the Account section shows "Not logged in" and a "Log in" row that opens the login form.
43. Given a logged-in user, when they tap "Delete account", then a warning says the account and all answers will be deleted permanently and asks for the password.
44. Given that warning, when the user enters a wrong password, then the account is not deleted and they are told the password is incorrect.
45. Given that warning, when the user enters the right password and confirms, then they are logged out, and logging in with that email and password afterwards fails.
46. Given a deleted account, when someone registers with the same email, then a new, empty account is created.
47. Given the legal section of Settings, then it says account deletion can also be requested by email at the contact address.

### Languages, accessibility and privacy

48. Given the app set to German, French and Italian in turn, when the user goes through the gate, registration, login, Home's prompt errors and Settings' account section, then every text is in that language.
49. Given a screen reader is on, when the user moves through the registration form, then every field, both eye buttons (with "shown"/"hidden"), the consent checkbox and the buttons are announced with their purpose.
50. Given the server logs after a registration, login, answer, logout and account deletion, then none of the email, the password or the login token appears in them.
51. Given a phone restored from a cloud backup of a logged-in phone, when the app opens, then the user is not logged in.

## Definition of Done

- All user acceptance tests pass on Android (emulator and a physical device with Google Play services).
- The shared code for iOS compiles; the iOS notes for whoever creates the iOS wrapper are updated (password autofill, Associated Domains).
- All automated tests listed in the Technical Annex exist and pass, and no existing test regresses.
- Diary answers are no longer stored on the device; the only place they exist is the user's account.
- Onboarding, Home's readings, All stations and Alarms work for users without an account exactly as before.
- Every new text exists in English, German, French and Italian, and the translation check passes.
- The new screens and dialogs can be completed with a screen reader.
- No email address, password or login token appears in server logs.
- Logins and registrations are rate limited, with the limit configurable for production.
- The project documentation (architecture, REST API, persistence, configuration, Diary, Settings, localization) describes the new behaviour, including the accepted limitations below.
- The build has no new deprecation or compiler warnings.

## Out of Scope

- **Email verification** and **password reset**. There is no email delivery; a forgotten password means the account and its diary are lost. The user may register a new account.
- **Changing email or password** while logged in.
- **Social or third-party login** (Google, Apple or other providers).
- **Moving alarms to accounts.** Alarms stay tied to the anonymous device, with or without an account.
- **Keeping answers on the device**: no offline queue for unsent answers and no local copy of the diary.
- **Migrating existing on-device answers** to an account. They are dropped; no real user has any yet.
- **Keeping a tapped answer through login**: an answer tapped while logged out is not saved after logging in; the user taps again on Home.
- **Editing or deleting individual answers**, and answering for a past day.
- **A web page for account deletion.** Deletion outside the app is by email request.
- **An explicit password-save prompt on iOS**; iOS gets field hints only.
- **Removing accounts for inactivity.**
- **Writing the privacy policy** itself (see Further Notes).

## Further Notes

- **Release prerequisites outside the code:** a privacy policy page with a public URL, linked from
  the store listing (and later from Settings); and the store listing's account-deletion information
  pointing to the email request. Google Play requires both once an app lets users create accounts.
- **Accepted limitation — no password reset.** It must be stated in the documentation and should be
  reconsidered as soon as email delivery exists (password reset and changing the password belong
  together then).
- **Accepted limitation — the registration reveals whether an email address has an account.** The
  alternative only works with email delivery. The login does not reveal it.
- **Swiss data protection.** Diary answers are sensitive personal data (health information) under
  the nDSG. Explicit consent at registration, deletion in the app, and never logging personal data
  are the baseline chosen here; this is not legal advice.
- The database is not live yet, so the schema is extended in place instead of through a new
  migration. If production has started on the current schema before this feature ships, that
  decision has to be revisited.
- The Diary never plots today's answer, so it does not need to follow Home live: an answer given on
  Home only matters to the Diary from the next day on.

---

## Technical Annex
> Written against codebase as of: 2026-10-11 (branch `feature/account`, `a4d5f4d`)

### Architectural Decisions

#### Server (`:server`)

New package `server/.../account/`, laid out like `alarm/`:

```
account/
├── domain/   AccountValidation (+ AccountSpec, Email), PasswordHasher (Argon2PasswordHasher),
│             SessionToken, UserId, DiaryFeeling (enum), DiaryEntry
├── store/    UserStore, SessionStore, DiaryStore (+ Exposed implementations)
├── model/    Wire DTOs (RegisterDto, LoginDto, DeleteAccountDto, SessionDto, DiaryAnswerDto,
│             DiaryEntryDto)
└── AccountRoutes.kt
```

**Schema** — `V1__init.sql` is extended **in place** (user decision: not live yet; CLAUDE.md's
"frozen from the first production start on" still holds afterwards). `MigrationTest` and the role
grants cover the new tables automatically through the owner's default privileges.

```sql
CREATE TABLE users (
    id            uuid        PRIMARY KEY,
    email         text        NOT NULL UNIQUE,      -- trimmed + lowercased
    password_hash text        NOT NULL,             -- Argon2id, PHC string with its parameters
    consented_at  timestamptz NOT NULL,
    created_at    timestamptz NOT NULL
);

CREATE TABLE sessions (
    token_hash    bytea       PRIMARY KEY,          -- SHA-256 of the session token
    user_id       uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    timestamptz NOT NULL,
    last_used_at  timestamptz NOT NULL              -- refreshed at most once a day
);
CREATE INDEX sessions_user ON sessions(user_id);

CREATE TABLE diary_entries (
    user_id       uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date          date        NOT NULL,             -- Swiss calendar day, chosen by the server
    feeling       varchar(16) NOT NULL,             -- VERY_BAD | BAD | GOOD | VERY_GOOD
    created_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, date)
);
```

`Tables.kt` mirrors them (it moves to, or gets a sibling in, `account/store`).

**Domain**

- `AccountValidation` — pure, like `AlarmValidation`: `validateRegistration(email, password,
  consent): ValidationResult<AccountSpec>` and `normalizeEmail(raw): String?`. Email: trimmed,
  lowercased, ≤ 254 characters, exactly one `@` with something on each side. Password: 8–128
  characters (code points), never trimmed, no character-type rules. `consent` must be `true`.
  Invalid → `Invalid(message)` → `400 {error}`.
- `PasswordHasher` — `hash(password): String`, `verify(password, hash): Boolean`,
  `verifyDummy(password)` (runs one verify against a fixed hash, for an unknown email, so timing does
  not reveal whether an account exists). `Argon2PasswordHasher` uses Bouncy Castle's
  `Argon2BytesGenerator` (`org.bouncycastle:bcprov-jdk18on`, pure JVM, via the version catalog),
  Argon2id, 16-byte salt from `SecureRandom`, OWASP baseline parameters (m = 19 MiB, t = 2, p = 1),
  encoded as a PHC string (`$argon2id$v=19$m=…,t=…,p=…$salt$hash`) so they can be raised later;
  `verify` reads the parameters from the string and compares in constant time. Tests can pass cheap
  parameters through the constructor.
- `SessionToken` — same shape as `DeviceToken` (256 bits, base64url without padding, 43
  characters, `parseOrNull`, `hash()` = SHA-256, `toString` hidden). Extracting a shared
  `OpaqueToken` helper is allowed if it stays trivial; `DeviceToken`'s behaviour must not change.
- `DiaryFeeling` — `VERY_BAD`, `BAD`, `GOOD`, `VERY_GOOD`, exact enum names on the wire (the app's
  `Feeling` names).

**Stores** (suspend interfaces, Exposed on `Dispatchers.IO`, as the alarm stores):

```kotlin
interface UserStore {
    suspend fun register(spec: AccountSpec, passwordHash: String, now: Instant): RegisterResult // Registered(userId) | EmailTaken
    suspend fun credentials(email: String): StoredCredentials?   // userId + passwordHash
    suspend fun email(userId: UserId): String?
    suspend fun delete(userId: UserId): Boolean                  // cascades to sessions + diary
}

interface SessionStore {
    suspend fun create(userId: UserId, now: Instant): SessionToken
    suspend fun authenticate(token: SessionToken, now: Instant): UserId? // null if unknown or expired;
                                                                          // refreshes last_used_at at most once per LAST_USED_RESOLUTION (1 day)
    suspend fun delete(token: SessionToken)
    suspend fun pruneExpired(now: Instant): Int                   // last_used_at older than SESSION_EXPIRY
}

interface DiaryStore {
    suspend fun record(userId: UserId, date: LocalDate, feeling: DiaryFeeling, now: Instant): RecordResult // Recorded(entry) | AlreadyAnswered(entry)
    suspend fun entries(userId: UserId, from: LocalDate, until: LocalDate): List<DiaryEntry>        // sorted by date
    suspend fun entry(userId: UserId, date: LocalDate): DiaryEntry?
}
```

- `SESSION_EXPIRY` = 90 days (exactly 90 days is still valid, as `INACTIVE_DEVICE_RETENTION`).
  An expired session is refused by `authenticate` even before the prune has run.
- `register` relies on the `UNIQUE` constraint (catch the unique violation → `EmailTaken`), so two
  concurrent registrations of one email cannot both succeed.
- `record` is `INSERT … ON CONFLICT (user_id, date) DO NOTHING` followed by a read, so a concurrent
  second answer never overwrites the first.

**Authentication** — Ktor's `Authentication` plugin can be installed only once, and
`configureDeviceAuthentication` installs it today. Restructure so that one install registers both
bearer providers: `DEVICE_AUTH` ("device") and a new `USER_AUTH` ("user") resolving to
`UserPrincipal(userId, token)` (the token is kept so logout can delete exactly that session). Same
rules as the device provider: a token without the issued shape is refused without a lookup; an
unparseable header, another scheme or an unknown/expired token is the same `401` with
`WWW-Authenticate: Bearer`, checked before the body is read. Route tests that install only one
of the two must still work (e.g. `configureAuthentication(devices = …, sessions = …)` with either
nullable, or `Application.authentication { }` to add a provider to an installed plugin — pick
one and use it for both).

**Routes** — `configureAccountRouting(users, sessions, diary, hasher, clock)`, installed in
`Application.module(config)` next to `configureAlarmRouting`:

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/users` | `{email, password, consent}` | `201 {sessionToken}`; `400 {error}` invalid / no consent / malformed; `409 {error}` email taken |
| POST | `/users/session` | `{email, password}` | `201 {sessionToken}`; `401 {error}` wrong email or password (identical for both; `verifyDummy` for an unknown email); `400` malformed |
| DELETE | `/users/me/session` | — | (user auth) `204`; deletes only the calling session |
| DELETE | `/users/me` | `{password}` | (user auth) `204`, cascading; `403 {error}` wrong password (not `401`, so the app does not drop its session) |
| POST | `/users/me/diary` | `{feeling}` | (user auth) `201 {date, feeling}` first answer of today's `SWISS_ZONE` date; `200 {date, feeling}` the **stored** entry if today already has one; `400 {error}` unknown feeling |
| GET | `/users/me/diary?from=YYYY-MM-DD&until=YYYY-MM-DD` | — | (user auth) `200 [{date, feeling}]` sorted, answered days only; `400 {error}` missing, unparseable, `until < from` or longer than 366 days |
| GET | `/users/me/diary/today` | — | (user auth) `200 {date, feeling}` or `204` |

- Email and the session token never appear in a path or query, so they cannot reach call logs.
- **Rate limit:** new `ACCOUNT_LIMIT` in `configureSecurity`, per client address, shared by
  `POST /users` and `POST /users/session`: `RateLimits.accountPerHour`, default 10, from
  `RATE_LIMIT_ACCOUNT_PER_HOUR` (validated like the others in `ServerConfig.load`). `/users/me/…`
  routes get a per-user limiter keyed on `UserPrincipal` inside `authenticate("user")`, mirroring
  `DEVICE_LIMIT` (`RATE_LIMIT_USER_PER_MINUTE`, default 60).
- `ErrorDto` for the `{error}` bodies, same shape as alarm/pollen.

**Scheduler** — `AlarmScheduler` gets a third daily prune step, `pruneExpiredSessions(now)`, with its
own day marker and `try`, on the first tick of each Swiss day, exactly like `pruneInactiveDevices`.
It takes the `SessionStore` as a constructor parameter. Users and diary entries are never pruned.

#### App (`:composeApp`)

**`core/session`** (new; `core/` because Home, Diary, Settings and the account screen use it):

```kotlin
data class Session(val token: String, val email: String)

interface SessionRepository {
    val session: Flow<Session?>                                   // null = signed out
    suspend fun register(email: String, password: String): Result<Unit> // consent is implied by the form; sent as true
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>          // DELETE /users/me/session; clears locally even if the request fails
    suspend fun deleteAccount(password: String): Result<Unit>   // clears locally on success
    suspend fun clear()                         // called by any repository on a 401
}
```

- `SessionRepositoryImpl(api: AccountApiService, store: SessionStorage)`. `SessionStorage` is the
  logic-free DataStore over a **separate file `polleninfo_session`** (`SESSION_DATA_STORE` qualifier
  in `PlatformModule`, both platforms, like `DEVICE_DATA_STORE`), holding token and email. Add
  `datastore/polleninfo_session.preferences_pb` to `data_extraction_rules.xml` and
  `backup_rules.xml`. Untested like the other DataStore classes; consumers use
  `FakeSessionRepository`.
- `AccountApiService(client, baseUrl)` maps its own statuses before `checkSuccess()`: `409` →
  `EmailTakenException`, `401` on login → `InvalidCredentialsException`, `403` on delete →
  `InvalidCredentialsException`, `401` on an authenticated call → `SignedOutException`.
- The account exceptions and their `AppError` mapping live in `core/session` (`toSessionAppError()`,
  as `toAlarmAppError()`); new `AppError` cases: `InvalidCredentials`, `EmailTaken`, `SignedOut`,
  with `error_*` sentences in all four languages and new `ErrorContext`s `LOGIN`, `REGISTER`,
  `SAVE_FEELING`, `DELETE_ACCOUNT` (`AppErrorTextTest` extended). The rate-limit `429` maps to a
  "Too many attempts, try again later" error (new `AppError.TooManyAttempts` or an `ErrorText`
  reason, decided at implementation).

**`core/diary`** (reworked):

- `DiaryRepository` becomes remote and takes the session:

  ```kotlin
  interface DiaryRepository {
      suspend fun record(feeling: Feeling): Result<DiaryEntry>          // POST /users/me/diary (201 or 200 alike)
      suspend fun today(): Result<DiaryEntry?>                          // GET /users/me/diary/today
      suspend fun entries(from: LocalDate, until: LocalDate): Result<List<DiaryEntry>>
  }
  ```

  `DiaryRepositoryImpl(api: DiaryApiService, sessions: SessionRepository)` reads the current token;
  with none it fails with `SignedOutException` without a request; a `401` calls
  `sessions.clear()` and fails with `SignedOutException`.
- New `FeelingPromptPreferences` (`dismissedOn: Flow<LocalDate?>`, `dismiss(date)`), logic-free
  DataStore in the shared `DataStore<Preferences>` (the existing dismissed key can be kept),
  consumers use `FakeFeelingPromptPreferences`.
- **Removed:** `DiaryCodec`, `DiaryCodecTest`, `DataStoreDiaryRepository`, `List<DiaryEntry>.recording`
  (+ its test; immutability is now the server's), the local `entries` key (left orphaned or removed
  once — harmless either way). `Feeling`, `DiaryEntry`, `swissToday` stay.

**`feature/account`** (new, `Screen.Account(mode: AccountMode = LOGIN)`, not a tab):

- `AccountFormState` (pure, `domain/model`): `mode` (`LOGIN` | `REGISTER`), `email`, `password`,
  `confirmation`, `consent`, `passwordVisible`, `confirmationVisible`; `withMode` keeps `email`;
  derived `passwordTooShort`, `passwordsMismatch` (only once `confirmation` is non-empty),
  `isEmailPlausible` (same light check as the server), `canSubmit` (login: email + non-empty
  password; register: email, 8–128, match, consent). Password limits are constants shared in the
  file (`MIN_PASSWORD_LENGTH = 8`, `MAX_PASSWORD_LENGTH = 128`).
- `AccountViewModel(sessionRepository, credentialStore)`: `AccountUiState(form, isSubmitting,
  error: AppError?)`, field changes ignored while submitting, `submit()` → `register` / `login` →
  on success `credentialStore.save(email, password)` then `AccountEvent.Done` (buffered `Channel`);
  on failure `error`, cleared by the next edit or submit. On first open in `LOGIN` mode it asks
  `credentialStore.offer()`; a returned credential fills the form and submits.
- `AccountScreen`: `TopAppBar` with back, email field (`ContentType.EmailAddress` /
  `Username`), password field(s) (`ContentType.Password`, `NewPassword` when registering), each with
  its own eye `IconButton` (announced "Show password" / "Hide password", state "shown"/"hidden"),
  the mismatch hint under the confirmation, the consent checkbox row (whole row toggleable), the
  submit button with progress, the error text, and the mode-switch text button. `onDone` =
  `popBackStack` in `AppNavigation`.
- `core/credentials/CredentialStore` (`suspend fun save(email, password)`, `suspend fun offer():
  SavedCredential?`), bound in `platformModule`. Android: `androidx.credentials` +
  `credentials-play-services-auth`, `CreatePasswordRequest` / `GetCredentialRequest(GetPasswordOption)`;
  every exception (cancel, no provider) is swallowed and logged. Credential Manager needs an
  activity context, so — like the permission requesters — it is a `@Composable expect fun
  rememberCredentialStore()` rather than a Koin single if a plain context is not enough (decide at
  implementation; keep the ViewModel platform-free either way). iOS: no-op actual. Checked by hand.

**Home** — `HomeViewModel(selectedStationRepository, getStationMeasurement, sessionRepository,
diaryRepository, feelingPromptPreferences, clock)`:

- Observes `sessionRepository.session`. Signed out: `showFeelingPrompt` = `dismissedOn != today`.
  Signed in: hidden until `diaryRepository.today()` answered; shown if it is `null` and today is not
  dismissed; a failed `today()` keeps it hidden until the next load/refresh. A session change
  re-checks at once.
- `onFeelingSelected(feeling)`: signed out → `HomeEvent.OpenAccount` (buffered `Channel`, new; the
  screen navigates to `Screen.Account(LOGIN)`), nothing recorded. Signed in → `record(feeling)`;
  success hides the prompt; failure keeps it with `Content.feelingSaveError: AppError?` (was a
  `Boolean`); `SignedOutException` leaves the prompt up (the session flow now says signed out).
- `onFeelingPromptDismissed()` → `feelingPromptPreferences.dismiss(today)`.

**Diary** — `DiaryViewModel(selectedStationRepository, stationRepository, speciesRepository,
stationHistoryRepository, diaryRepository, sessionRepository, clock)`:

- New `DiaryUiState.SignedOut` (the gate: heading, explanation, Log in → `Screen.Account(LOGIN)`,
  Create account → `Screen.Account(REGISTER)`). Signed out → `SignedOut`, nothing fetched.
- Signed in → the existing flow; each history load fetches `history` and
  `diaryRepository.entries(history window)` in parallel (the window is computed client-side the way
  the server does — ending yesterday — or the entries are requested after the history's
  `from`/`until` arrive; prefer parallel with the client-computed window, and filter the entries to
  `history.from..history.until` as today). Either failing → `Error` with Retry. `SignedOutException`
  → `SignedOut`. A session change to `null` → `SignedOut` and the held entries are dropped; to a
  session → load.
- The `entries` collection from a flow, and the reliance on it for live updates, go away;
  `Content.entries` and `hasNoEntries` keep their meaning.

**Settings** — `SettingsViewModel(…, sessionRepository)`: `account: Session?` (email from the stored
session, no request), `showLogoutDialog`, `showDeleteDialog`, `deletePassword`, `isDeleting`,
`deleteError: AppError?`. New "Account" section under "Default station" (`settings_section_account`):
"Logged in as {email}", "Log out" (confirm dialog, `logout()`), "Delete account" (dialog with warning,
password field with eye toggle, `deleteAccount(password)`; wrong password stays in the dialog with
`InvalidCredentials`). Signed out: "Not logged in" + "Log in" row → `Screen.Account(LOGIN)`. The
Impressum gets `settings_account_deletion_note` with the contact address.

**Navigation** — `Screen.Account(mode)` registered in `AppNavigation`, not in
`TopLevelDestination` (bottom bar hidden; `TopLevelDestinationTest` gets the new route in its
"hidden" set if it enumerates them).

**Strings** — new keys under `account_*` (form, gate, consent, password hints, eye buttons),
`diary_gate_*`, `settings_account_*`, `home_feeling_*` (save error wording moves to `AppError`), and
`error_*` for the new `AppError`s — in `values`, `values-de` (Swiss Standard German, "du", no "ß"),
`values-fr` (formal, U+202F), `values-it` (formal). `checkTranslations` must pass; run the
hard-coded-text grep from CLAUDE.md.

**DI** — `SessionRepositoryImpl`, `AccountApiService`, `DiaryApiService`, `DiaryRepositoryImpl`
singles in `dataModule`; `SessionStorage`, `FeelingPromptPreferences` storage and (if a Koin binding)
`CredentialStore` in `platformModule`; `AccountViewModel` in `presentationModule`.

**Docs** — CLAUDE.md sections to update: Architecture, the `core/` table (`core/session`,
`core/credentials`, reworked `core/diary`), Persisted user selections ("Diary answers" rewritten —
answers now leave the device), Error handling (new `AppError`s), Diary, The feeling prompt,
Settings, REST API, Persistence (tables), Configuration (two new variables), Alarm delivery (third
prune), Abuse protection, iOS wrapper configuration (password autofill / Associated Domains),
Testing.

### Automated Testing Decisions

**What makes a good test here:** assert observable behaviour through the module's public surface —
HTTP status and body for routes, returned results and stored rows for stores, `Result` and requests
sent for repositories, emitted UI states and events for ViewModels. No assertions on private
fields or call order inside a class. Pin every boundary from both sides (password 7/8 and 128/129,
email 254/255, session age exactly 90 days / 90 days + 1 ms, range 366/367 days, the Swiss midnight
edge for "today"), as `SpeciesThresholdsTest` does. Backtick names without commas (Kotlin/Native).
Hand-written fakes only.

**Server** (`./gradlew :server:test`):

| Test | Type | Covers | Prior art |
| --- | --- | --- | --- |
| `AccountValidationTest` | unit | normalization, every invalid case and both sides of each limit, consent | `AlarmValidationTest` |
| `PasswordHasherTest` | unit | hash ≠ password, two hashes of one password differ (salt), verify right/wrong, parameters read from the string, malformed hash → false | — |
| `SessionTokenTest` | unit | shape, `parseOrNull`, hash stability, hidden `toString` | `DeviceTokenTest` |
| `ExposedAccountStoresTest` | integration, Postgres | email uniqueness incl. concurrent registration, `credentials`, delete cascades to sessions + diary, session expiry at exactly 90 days, `last_used_at` refreshed at most daily, `pruneExpired`, diary `AlreadyAnswered` keeps the first answer (incl. concurrent), `entries` range + order | `ExposedStoresTest` |
| `AccountRoutesTest` | integration, Postgres | every row of the endpoint table, `401` before body, identical `401` for unknown email and wrong password, `403` wrong password on delete, server-chosen Swiss date with a `MutableClock` across midnight, another user's diary never visible, logout ends only the calling session | `AlarmRoutesTest` |
| `SecurityTest` (extended) | integration | `ACCOUNT_LIMIT` shared by register + login, per-user limit | `SecurityTest` |
| `ServerConfigTest` (extended) | unit | the two new rate-limit variables | `ServerConfigTest` |
| `MigrationTest` (extended) | integration | new tables readable/writable by the app role | `MigrationTest` |
| `AlarmSchedulerTest` (extended) | integration | session prune on a Swiss day's first tick only, a throwing prune does not cost the minute's alarms and is retried | the device-prune cases |
| `AccountFlowLoggingTest` | integration | register → login → answer → logout → delete: no email, password or session token in the captured log | `DeviceFlowLoggingTest` |

**App** (`./gradlew :composeApp:testAndroidHostTest`, plus `compileTestKotlinIosSimulatorArm64`):

| Test | Type | Covers | Prior art |
| --- | --- | --- | --- |
| `SessionRepositoryImplTest` | unit, `MockEngine` | register/login store token + email, `409`/`401`/`403`/`429` mapping, logout clears locally even when the request fails, delete clears only on success, the bearer header | `AlarmRepositoryImplTest` |
| `DiaryRepositoryImplTest` | unit, `MockEngine` | `record` for `201` and `200`, `today` for `200`/`204`, `entries` query, no request without a session, `401` → `clear()` + `SignedOutException` | `StationHistoryRepositoryImplTest` |
| `AccountFormStateTest` | unit | `canSubmit` per mode, length boundaries, mismatch only once confirmation is non-empty, consent, mode switch keeps email, independent visibility toggles | `AlarmFormStateTest` |
| `AccountViewModelTest` | unit, virtual time | `isSubmitting` under a gate, `Done` once, error kept and cleared on edit, credential saved only on success, offered credential fills and submits | `ChangeStationViewModelTest` |
| `HomeViewModelTest` (extended) | unit | signed-out prompt + `OpenAccount` event, signed-in prompt hidden while `today()` loads, shown/hidden by its answer, hidden on failure, save error as `AppError`, session change re-checks, dismissal | existing prompt tests |
| `DiaryViewModelTest` (extended) | unit | `SignedOut` without fetches, parallel load, answers failing → `Error`, `401` → `SignedOut`, sign-out drops entries, sign-in loads | existing |
| `SettingsViewModelTest` (extended) | unit | account row signed in/out, logout dialog, delete dialog with wrong password and success | existing |
| `AppErrorTextTest` (extended) | unit | new `AppError`s and contexts | existing |

New fakes in `commonTest`: `FakeSessionRepository` (settable session, gates, scripted failures),
`FakeDiaryRepository` (rewritten for the remote interface, with `recordGate` / `todayGate`),
`FakeFeelingPromptPreferences`, `FakeCredentialStore`.

**Checked by hand** (as with the other platform pieces): the composables, `SessionStorage` and the
prompt-preferences DataStore, the Android Credential Manager store, backup exclusion, and the iOS
no-op.
