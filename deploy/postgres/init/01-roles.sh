#!/usr/bin/env bash
# Runs once, when the Postgres data volume is created (docker-entrypoint-initdb.d), as the superuser.
# Shared by development (compose.dev.yaml), production (compose.yaml) and the server's
# Testcontainers tests, so every environment has the same database and the same two roles:
#
#   polleninfo_owner  owns the database and schema `public`; Flyway migrates as this role.
#   polleninfo_app    the running server's login: SELECT, INSERT, UPDATE, DELETE on the owner's
#                     tables and nothing else — it cannot create, alter or drop a table.
#
# The passwords are read from the files DB_OWNER_PASSWORD_FILE and DB_APP_PASSWORD_FILE name.
set -euo pipefail

owner_password="$(<"${DB_OWNER_PASSWORD_FILE:?DB_OWNER_PASSWORD_FILE is not set}")"
app_password="$(<"${DB_APP_PASSWORD_FILE:?DB_APP_PASSWORD_FILE is not set}")"

# psql substitutes :'var' as a quoted literal in script input, so no password is ever spliced into
# SQL text by the shell.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v owner_password="$owner_password" -v app_password="$app_password" <<'SQL'
CREATE ROLE polleninfo_owner LOGIN PASSWORD :'owner_password';
CREATE ROLE polleninfo_app LOGIN PASSWORD :'app_password';
CREATE DATABASE polleninfo OWNER polleninfo_owner;

\connect polleninfo

ALTER SCHEMA public OWNER TO polleninfo_owner;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO polleninfo_app;

ALTER DEFAULT PRIVILEGES FOR ROLE polleninfo_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO polleninfo_app;
ALTER DEFAULT PRIVILEGES FOR ROLE polleninfo_owner IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO polleninfo_app;
SQL
