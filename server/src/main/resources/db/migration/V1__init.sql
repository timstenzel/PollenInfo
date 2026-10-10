-- The alarm store: registered installs, their alarms and what each threshold alert has notified
-- about. Applied by Flyway as polleninfo_owner; the running server's polleninfo_app login receives
-- SELECT, INSERT, UPDATE and DELETE on these tables through the owner's default privileges
-- (deploy/postgres/init/01-roles.sh).

CREATE TABLE devices (
    id            uuid        PRIMARY KEY,          -- internal; never sent to the app
    token_hash    bytea       NOT NULL UNIQUE,      -- SHA-256 of the device token, never the token
    fcm_token     text,                             -- NULL once FCM reported it unregistered
    created_at    timestamptz NOT NULL,
    last_seen_at  timestamptz NOT NULL              -- refreshed at most once a day
);

CREATE TABLE alarms (
    id            uuid        PRIMARY KEY,
    device_id     uuid        NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    enabled       boolean     NOT NULL,
    station_abbr  varchar(3)  NOT NULL,
    species       text[]      NOT NULL,             -- PollenSpecies names
    min_severity  varchar(16) NOT NULL,
    days          text[]      NOT NULL,             -- DayOfWeek names
    type          varchar(16) NOT NULL,             -- 'daily' | 'threshold'
    at_time       time,                             -- daily reports
    from_time     time,                             -- threshold alerts
    until_time    time,
    created_at    timestamptz NOT NULL              -- strictly increasing per device
);

CREATE INDEX alarms_device_created ON alarms(device_id, created_at);

CREATE TABLE notification_log (
    alarm_id      uuid        NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
    species       varchar(16) NOT NULL,
    local_date    date        NOT NULL,             -- Swiss calendar day
    PRIMARY KEY (alarm_id, species, local_date)
);
