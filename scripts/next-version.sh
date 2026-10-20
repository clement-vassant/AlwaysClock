#!/usr/bin/env bash
# Calcule la prochaine version SemVer à partir de la dernière (X.Y.Z) et du type de saut demandé.
#   scripts/next-version.sh 0.3.1 minor       -> 0.4.0
#   scripts/next-version.sh 0.3.1 auto < log  -> déduit des messages Conventional Commits lus sur l'entrée :
#     un « ! » ou « BREAKING CHANGE » donne une majeure (une mineure avant 1.0), un « feat » une mineure, sinon un correctif.
set -euo pipefail

last="$1"
bump="$2"
if [[ ! "$last" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Version précédente invalide : $last" >&2
    exit 1
fi
major="${BASH_REMATCH[1]}" minor="${BASH_REMATCH[2]}" patch="${BASH_REMATCH[3]}"

if [[ "$bump" == "auto" ]]; then
    log="$(cat)"
    if grep -qE '^[a-z]+(\([^)]*\))?!:|^BREAKING[ -]CHANGE:' <<<"$log"; then
        # Avant 1.0, SemVer autorise les changements cassants dans une mineure.
        if (( major == 0 )); then bump=minor; else bump=major; fi
    elif grep -qE '^feat(\([^)]*\))?:' <<<"$log"; then
        bump=minor
    else
        bump=patch
    fi
fi

case "$bump" in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "$major.$((minor + 1)).0" ;;
    patch) echo "$major.$minor.$((patch + 1))" ;;
    *) echo "Saut inconnu : $bump (attendu : auto, patch, minor, major)" >&2; exit 1 ;;
esac
