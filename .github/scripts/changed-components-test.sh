#!/usr/bin/env bash
# Self-test of changed-components.sh, the path -> component mapping that decides which CI gates run.
# A mistake there would silently skip a gate, so every workflow runs this first.
#
# Usage: changed-components-test.sh
#
# Builds a throwaway git repository in a temporary directory. For each case it commits the listed
# paths on a branch from a base commit, runs changed-components.sh <base> HEAD with GITHUB_OUTPUT
# unset and compares its output with the expected components. Stops at the first mismatch with the
# expected and actual output. Plain bash 3.2 (macOS's /bin/bash): no associative arrays, no mapfile.
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/changed-components.sh"
repo="$(mktemp -d)"
trap 'rm -rf "$repo"' EXIT
unset GITHUB_OUTPUT

# A fixed throwaway identity, no signing and no hooks, so the developer's git setup cannot interfere.
git_() {
  git -C "$repo" \
    -c user.name='changed-components test' \
    -c user.email='test@example.invalid' \
    -c commit.gpgsign=false \
    -c core.hooksPath=/dev/null \
    "$@"
}

git_ init --quiet --initial-branch=base
echo base > "$repo/base.txt"
git_ add base.txt
git_ commit --quiet --message base
base_sha="$(git_ rev-parse HEAD)"

# Prints what changed-components.sh prints for the given components (comma-separated, or "none").
expected_output() {
  local server=false android=false ios=false
  case ",$1," in *,server,*) server=true ;; esac
  case ",$1," in *,android,*) android=true ;; esac
  case ",$1," in *,ios,*) ios=true ;; esac
  printf 'server=%s\nandroid=%s\nios=%s\n' "$server" "$android" "$ios"
}

cases=0

# check <name> <base> <expected components> -- runs the script against the current HEAD.
check() {
  local name="$1" base="$2" expected actual
  expected="$(expected_output "$3")"
  actual="$(cd "$repo" && "$script" "$base" HEAD 2>/dev/null)"
  cases=$((cases + 1))
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL: $name" >&2
    diff <(echo "$expected") <(echo "$actual") | sed -n 's/^< /  expected: /p; s/^> /  actual:   /p' >&2 || true
    exit 1
  fi
  echo "ok: $name"
}

# changed <expected components> <path>... -- commits the paths on a fresh branch from the base.
changed() {
  local expected="$1" path
  shift
  git_ checkout --quiet -B case "$base_sha"
  for path in "$@"; do
    mkdir -p "$repo/$(dirname "$path")"
    echo "change" >> "$repo/$path"
    git_ add -- "$path"
  done
  git_ commit --quiet --message case
  check "$*" "$base_sha" "$expected"
}

# One case per mapping row, at least one path each.
changed server server/src/main/kotlin/Main.kt
changed server deploy/compose.yaml
changed server .github/workflows/server.yml
changed android androidApp/build.gradle.kts
changed android .github/workflows/android.yml
changed ios iosApp/iosApp/iOSApp.swift
changed ios iosApp/iosApp.xcodeproj/project.pbxproj
changed ios .github/workflows/ios.yml
changed android,ios composeApp/src/commonMain/kotlin/App.kt
changed android,ios theme/build.gradle.kts
changed server,android,ios gradle/libs.versions.toml
changed server,android,ios build.gradle.kts
changed server,android,ios settings.gradle.kts
changed server,android,ios gradle.properties
changed server,android,ios gradlew
changed server,android,ios gradlew.bat
changed server,android,ios buildSrc/src/main/kotlin/Plugin.kt
changed server,android,ios .github/actions/setup-jvm/action.yml
changed server,android,ios .github/scripts/changed-components.sh

# Documentation and unmapped files run no gate.
changed none README.md
changed none .sdd/x/requirements.md
changed none .github/dependabot.yml
changed none CLAUDE.md deploy.md docs/ios.md

# A mixed change runs the union of its components.
changed server,android server/src/main/kotlin/Main.kt androidApp/src/main/AndroidManifest.xml
changed server,ios README.md deploy/README.md iosApp/Config/Base.xcconfig

# No usable base: everything changed.
check "all-zero base" 0000000000000000000000000000000000000000 server,android,ios
check "empty base" "" server,android,ios
check "unresolvable base" 1234567890abcdef1234567890abcdef12345678 server,android,ios

echo "All $cases cases passed."
