#!/usr/bin/env bash
# Lays out the production stack's secret files, owned by the one uid that reads each, mode 0400.
#
#   sudo deploy/install-secrets.sh [--fcm-key <service-account.json> | --fake-fcm-key] [<dir>]
#
# <dir> defaults to /opt/polleninfo/secrets. Non-Swarm compose bind-mounts a secret file as it is and
# ignores `uid` / `mode`, so ownership has to be right on disk:
#
#   <dir>/postgres/postgres_superuser_password   uid 999   (postgres in the postgres image)
#   <dir>/postgres/db_owner_password             uid 999
#   <dir>/postgres/db_app_password               uid 999
#   <dir>/server/db_owner_password               uid 10001 (the server image, server/Dockerfile)
#   <dir>/server/db_app_password                 uid 10001
#   <dir>/server/fcm_credentials.json            uid 10001
#
# The two database passwords are needed by both containers (Postgres creates the roles with them on
# its first start, the server logs in with them), so each exists twice with the same content.
#
# Idempotent: a password that exists is kept — Postgres only reads them when its volume is created,
# so a new one would lock the server out. Missing ones are generated (openssl rand -base64 32).
# --fcm-key installs (or replaces) the Firebase service-account key; --fake-fcm-key writes a
# syntactically valid key with a throw-away RSA key, for trying the stack locally — the server
# starts with it, and every push fails and is logged.
set -euo pipefail

POSTGRES_UID=999
SERVER_UID=10001

fcm_key=""
fake_fcm_key=false
dir=/opt/polleninfo/secrets
while [[ $# -gt 0 ]]; do
    case "$1" in
        --fcm-key) fcm_key="${2:?--fcm-key needs a file}"; shift 2 ;;
        --fake-fcm-key) fake_fcm_key=true; shift ;;
        -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
        -*) echo "Unknown option $1" >&2; exit 2 ;;
        *) dir="$1"; shift ;;
    esac
done

if [[ $EUID -ne 0 ]]; then
    echo "Run as root (sudo): the files must be owned by the containers' uids." >&2
    exit 1
fi

install -d -m 0755 -o 0 -g 0 "$dir"
install -d -m 0755 -o 0 -g 0 "$dir/postgres" "$dir/server"

# Writes stdin to $1, owned by uid $2, mode 0400, without the content ever being world-readable.
write_secret() {
    local target="$1" uid="$2" tmp
    tmp="$(mktemp "$(dirname "$target")/.tmp.XXXXXX")"
    cat >"$tmp"
    chown "$uid:$uid" "$tmp"
    chmod 0400 "$tmp"
    mv -f "$tmp" "$target"
}

# Prints the existing password of $1 (from either copy), or generates one.
password() {
    local name="$1"
    for copy in "$dir/postgres/$name" "$dir/server/$name"; do
        if [[ -s "$copy" ]]; then cat "$copy"; return; fi
    done
    openssl rand -base64 32
}

superuser="$(password postgres_superuser_password)"
owner="$(password db_owner_password)"
app="$(password db_app_password)"

printf '%s\n' "$superuser" | write_secret "$dir/postgres/postgres_superuser_password" "$POSTGRES_UID"
printf '%s\n' "$owner" | write_secret "$dir/postgres/db_owner_password" "$POSTGRES_UID"
printf '%s\n' "$app" | write_secret "$dir/postgres/db_app_password" "$POSTGRES_UID"
printf '%s\n' "$owner" | write_secret "$dir/server/db_owner_password" "$SERVER_UID"
printf '%s\n' "$app" | write_secret "$dir/server/db_app_password" "$SERVER_UID"

fcm_target="$dir/server/fcm_credentials.json"
if [[ -n "$fcm_key" ]]; then
    write_secret "$fcm_target" "$SERVER_UID" <"$fcm_key"
elif $fake_fcm_key; then
    rsa_key="$(openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null)"
    # JSON string escaping of the PEM: newlines become \n.
    pem_json="$(printf '%s\n' "$rsa_key" | awk 'BEGIN { ORS = "" } { print $0 "\\n" }')"
    write_secret "$fcm_target" "$SERVER_UID" <<JSON
{
  "type": "service_account",
  "project_id": "polleninfo-local",
  "private_key_id": "local",
  "private_key": "$pem_json",
  "client_email": "local@polleninfo-local.iam.gserviceaccount.com",
  "client_id": "0",
  "token_uri": "https://oauth2.googleapis.com/token"
}
JSON
elif [[ -e "$fcm_target" ]]; then
    # Re-assert ownership and mode of the key already in place.
    chown "$SERVER_UID:$SERVER_UID" "$fcm_target"
    chmod 0400 "$fcm_target"
else
    echo "Note: no FCM key installed yet; the server will not start without $fcm_target." >&2
    echo "      Re-run with --fcm-key <service-account.json>." >&2
fi

echo "Secrets in $dir:"
ls -ln "$dir/postgres" "$dir/server"
