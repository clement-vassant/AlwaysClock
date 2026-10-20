#!/usr/bin/env bash
# Écrit les notes d'une release GitHub, groupées par rubrique, à partir des titres de commits
# Conventional Commits lus sur l'entrée (un par ligne, « type(portée)!: description (#123) »).
#   git log v0.1.0..HEAD --format=%s | scripts/release-notes.sh owner/repo v0.1.0 v0.2.0
# Les types sans effet pour qui installe l'app (ci, docs, test, chore…) n'apparaissent pas.
set -euo pipefail

repo="$1"
previous_tag="$2"
tag="$3"

breaking="" features="" fixes="" dependencies=""
pattern='^([a-z]+)(\([^)]*\))?(!?): (.+)$'
while IFS= read -r subject; do
    [[ "$subject" =~ $pattern ]] || continue
    type="${BASH_REMATCH[1]}" bang="${BASH_REMATCH[3]}" description="${BASH_REMATCH[4]}"
    # Première lettre en majuscule (sans ${x^}, absent du bash 3 de macOS) et « (#12) » transformé en lien vers la PR.
    description="$(tr '[:lower:]' '[:upper:]' <<<"${description:0:1}")${description:1}"
    description="$(sed -E "s|\(#([0-9]+)\)$|([#\1](https://github.com/$repo/pull/\1))|" <<<"$description")"
    line="- $description"$'\n'
    if [[ -n "$bang" ]]; then
        breaking+="$line"
        continue
    fi
    case "$type" in
        feat) features+="$line" ;;
        fix | perf | revert) fixes+="$line" ;;
        deps) dependencies+="$line" ;;
    esac
done

section() {
    if [[ -n "$2" ]]; then printf '## %s\n\n%s\n' "$1" "$2"; fi
}

{
    if [[ -z "$previous_tag" ]]; then
        printf 'Première version publiée.\n\n'
    fi
    section "⚠️ Changements importants" "$breaking"
    section "Nouveautés" "$features"
    section "Corrections" "$fixes"
    section "Dépendances" "$dependencies"
    if [[ -n "$previous_tag" && -z "$breaking$features$fixes$dependencies" ]]; then
        printf 'Améliorations internes, sans changement visible.\n\n'
    fi
    printf '## Installer\n\nTélécharger `alwaysclock-%s.apk` ci-dessous et l'"'"'ouvrir sur le téléphone.\n' "$tag"
    if [[ -n "$previous_tag" ]]; then
        printf '\n[Tous les changements depuis %s](https://github.com/%s/compare/%s...%s)\n' \
            "$previous_tag" "$repo" "$previous_tag" "$tag"
    fi
}
