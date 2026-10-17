-- Personal accounts: a user logs in with an email address and a password and holds sessions, one
-- per logged-in phone, and their diary. Nothing here is pruned for inactivity except expired
-- sessions. Applied by Flyway as polleninfo_owner after V1; the polleninfo_app login receives
-- SELECT, INSERT, UPDATE and DELETE on these tables through the owner's default privileges
-- (deploy/postgres/init/01-roles.sh), as on V1's.

CREATE TABLE users (
    id            uuid        PRIMARY KEY,          -- internal; never sent to the app
    email         text        NOT NULL UNIQUE,      -- trimmed and lowercased
    password_hash text        NOT NULL,             -- Argon2id, PHC string with its parameters
    consented_at  timestamptz NOT NULL,             -- when the user agreed to their answers being stored
    created_at    timestamptz NOT NULL
);

CREATE TABLE sessions (
    token_hash    bytea       PRIMARY KEY,          -- SHA-256 of the session token, never the token
    user_id       uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    timestamptz NOT NULL,
    last_used_at  timestamptz NOT NULL              -- refreshed at most once a day
);

CREATE INDEX sessions_user ON sessions(user_id);

-- The diary: at most one answer per user and Swiss calendar day, chosen by the server. The first
-- answer of a day is final; a second one is never written (INSERT … ON CONFLICT DO NOTHING).
CREATE TABLE diary_entries (
    user_id       uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date          date        NOT NULL,             -- Swiss calendar day
    feeling       varchar(16) NOT NULL,             -- VERY_BAD | BAD | GOOD | VERY_GOOD
    created_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, date)
);
