# Secret files (placeholders)

The layout `deploy/install-secrets.sh` creates under `/opt/polleninfo/secrets` (or `SECRETS_DIR`).
Every file here is a placeholder; the real ones exist only on the server (or, for a local trial, in
the git-ignored `deploy/local-secrets/`), each owned by the uid of the container that reads it, mode
`0400`:

| File | Owner | Read by |
| --- | --- | --- |
| `postgres/postgres_superuser_password` | 999 | Postgres (`POSTGRES_PASSWORD_FILE`) |
| `postgres/db_owner_password` | 999 | Postgres's role script, on the volume's first start |
| `postgres/db_app_password` | 999 | Postgres's role script, on the volume's first start |
| `server/db_owner_password` | 10001 | The server (Flyway migrates as the owner) |
| `server/db_app_password` | 10001 | The server (its connection pool) |
| `server/fcm_credentials.json` | 10001 | The server (`FCM_CREDENTIALS`) |
