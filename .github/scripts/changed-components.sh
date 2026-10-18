#!/usr/bin/env bash
# Decides which CI gates a change needs: the single place for the path -> component mapping.
#
# Usage: changed-components.sh <base> <head>
#
# Lists the files changed between the merge base of <base> and <head> and <head>
# (`git diff <base>...<head>`), and prints `server=`, `android=` and `ios=` as `true`/`false`,
# appended to $GITHUB_OUTPUT when it is set, otherwise to stdout. A missing or all-zero base (first
# push, rewritten history) or one git cannot resolve means "everything changed".
#
# Mapping (anything not listed, e.g. *.md, .sdd/, .github/dependabot.yml, iosApp/, runs no gate):
#   server/**, deploy/**, .github/workflows/server.yml          -> server
#   androidApp/**, .github/workflows/android.yml                 -> android (android.yml: also ios,
#                                                                   whose job lives in that workflow)
#   composeApp/**, theme/**                                       -> android, ios
#   gradle/**, root Gradle files, gradlew(.bat), buildSrc/**,
#   .github/actions/**, .github/scripts/**                        -> server, android, ios
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "usage: $0 <base> <head>" >&2
  exit 2
fi

base="${1:-}"
head="${2:-HEAD}"

server=false
android=false
ios=false

all() {
  server=true
  android=true
  ios=true
}

if [[ -z "$base" || "$base" =~ ^0+$ ]]; then
  echo "No usable base, treating every component as changed." >&2
  all
elif ! files="$(git diff --name-only "$base...$head")"; then
  echo "Cannot diff $base...$head, treating every component as changed." >&2
  all
else
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    case "$file" in
      server/* | deploy/* | .github/workflows/server.yml)
        server=true ;;
      androidApp/*)
        android=true ;;
      .github/workflows/android.yml | composeApp/* | theme/*)
        android=true
        ios=true ;;
      gradle/* | build.gradle.kts | settings.gradle.kts | gradle.properties | gradlew | gradlew.bat \
        | buildSrc/* | .github/actions/* | .github/scripts/*)
        all ;;
    esac
  done <<< "$files"
fi

out="${GITHUB_OUTPUT:-/dev/stdout}"
{
  echo "server=$server"
  echo "android=$android"
  echo "ios=$ios"
} >> "$out"
