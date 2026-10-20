#!/usr/bin/env bash
# Tests de scripts/next-version.sh, lancés par la CI (job Lint).
set -euo pipefail
cd "$(dirname "$0")"

failures=0
expect() {
    local expected="$1" last="$2" bump="$3" log="${4:-}" actual
    actual="$(./next-version.sh "$last" "$bump" <<<"$log")"
    if [[ "$actual" != "$expected" ]]; then
        echo "ÉCHEC : $last $bump [$log] -> $actual, attendu $expected"
        failures=$((failures + 1))
    fi
}

expect 0.1.1 0.1.0 patch
expect 0.2.0 0.1.3 minor
expect 1.0.0 0.9.2 major
expect 0.1.1 0.1.0 auto "fix: keep the calendar visible"
expect 0.1.1 0.1.0 auto "deps: bump androidx group"
expect 0.2.0 0.1.0 auto $'fix: a\nfeat(ui): new clock style'
expect 0.2.0 0.1.0 auto "feat!: drop Android 10"
expect 2.0.0 1.4.2 auto "refactor(prefs)!: rename keys"
expect 2.0.0 1.4.2 auto $'fix: x\n\nBREAKING CHANGE: settings are reset'
expect 1.4.3 1.4.2 auto "docs: README"

if ./next-version.sh 0.1 patch >/dev/null 2>&1; then
    echo "ÉCHEC : une version précédente invalide doit être refusée"
    failures=$((failures + 1))
fi

(( failures == 0 )) && echo "next-version : tous les tests passent"
exit "$failures"
