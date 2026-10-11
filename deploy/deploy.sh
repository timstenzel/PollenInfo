#!/usr/bin/env bash
# Deploys a published server image to production, or rolls back to an earlier one
# (deploy/README.md, "Deploying and rolling back").
#
#   deploy/deploy.sh <tag>
#
# <tag> is a commit SHA that .github/workflows/server.yml published to GHCR. Run from a clean
# checkout on your own machine. Over SSH it:
#
#   1. copies the stack files of this checkout — compose.yaml, caddy/, postgres/,
#      install-secrets.sh, .env.example — to /opt/polleninfo/deploy,
#   2. sets SERVER_TAG=<tag> in /opt/polleninfo/deploy/.env (created from .env.example once),
#   3. runs `docker compose pull server && docker compose up -d --wait`,
#
# then waits for https://api.polleninfo.ch/health. It prints the tag it replaced: a rollback is the
# same command with that tag.
#
#   POLLENINFO_SSH   the SSH login (default tim@api.polleninfo.ch)
#   POLLENINFO_URL   the address checked afterwards (default https://api.polleninfo.ch)
set -euo pipefail

die() { echo "deploy: $*" >&2; exit 1; }

[[ $# -eq 1 ]] || die "usage: deploy/deploy.sh <tag>"
tag="$1"
[[ "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "'$tag' is not a valid image tag"

ssh_target="${POLLENINFO_SSH:-tim@api.polleninfo.ch}"
url="${POLLENINFO_URL:-https://api.polleninfo.ch}"
remote=/opt/polleninfo/deploy
here="$(cd "$(dirname "$0")" && pwd)"
files=(compose.yaml caddy postgres install-secrets.sh .env.example)

# What runs in production must be what is committed.
if [[ -n "$(git -C "$here" status --porcelain -- "${files[@]}")" ]]; then
    die "uncommitted changes in deploy/; commit or stash them first"
fi

pack() {
    if tar --version 2>/dev/null | grep -q bsdtar; then
        # macOS: keep its extended attributes out of the archive.
        COPYFILE_DISABLE=1 tar -C "$here" --no-mac-metadata -cf - "${files[@]}"
    else
        tar -C "$here" -cf - "${files[@]}"
    fi
}

echo "Copying the stack files to $ssh_target:$remote"
# shellcheck disable=SC2029 # $remote is a fixed path, meant to expand here.
pack | ssh "$ssh_target" "tar -C $remote --warning=no-unknown-keyword -xf -"

# shellcheck disable=SC2087 # $remote is meant to expand here; everything else is quoted.
ssh "$ssh_target" bash -s -- "$tag" <<REMOTE
set -euo pipefail
cd $remote
tag="\$1"
[[ -f .env ]] || cp .env.example .env
previous="\$(sed -n 's/^SERVER_TAG=//p' .env)"
if grep -q '^SERVER_TAG=' .env; then
    sed -i "s/^SERVER_TAG=.*/SERVER_TAG=\$tag/" .env
else
    echo "SERVER_TAG=\$tag" >>.env
fi
docker compose pull server
docker compose up -d --wait
echo "Running \$tag (was: \${previous:-none})."
REMOTE

echo "Waiting for $url/health"
for _ in $(seq 1 30); do
    if [[ "$(curl -fsS --max-time 5 "$url/health" 2>/dev/null)" == OK ]]; then
        echo "OK — $tag is live."
        exit 0
    fi
    sleep 2
done
die "$url/health did not answer OK within a minute; see: ssh $ssh_target 'cd $remote && docker compose logs server'"
