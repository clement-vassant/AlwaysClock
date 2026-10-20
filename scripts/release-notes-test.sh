#!/usr/bin/env bash
# Tests de scripts/release-notes.sh, lancés par la CI (job Lint).
set -euo pipefail
cd "$(dirname "$0")"

failures=0
check() {
    local description="$1" expected="$2" notes="$3"
    if [[ "$notes" != *"$expected"* ]]; then
        echo "ÉCHEC : $description : « $expected » absent de :"$'\n'"$notes"
        failures=$((failures + 1))
    fi
}
refute() {
    local description="$1" unexpected="$2" notes="$3"
    if [[ "$notes" == *"$unexpected"* ]]; then
        echo "ÉCHEC : $description : « $unexpected » ne devrait pas apparaître"
        failures=$((failures + 1))
    fi
}

notes="$(./release-notes.sh o/r v0.1.0 v0.2.0 <<'LOG'
feat(ui): add an analog clock style (#30)
fix: keep the calendar visible at midnight (#31)
deps: bump the androidx group (#32)
ci: bump actions/cache from 4 to 6 (#33)
docs: README
feat!: drop Android 10 (#34)
Merge branch 'main'
LOG
)"
check "nouveauté avec lien" "## Nouveautés"$'\n\n'"- Add an analog clock style ([#30](https://github.com/o/r/pull/30))" "$notes"
check "correction" "- Keep the calendar visible at midnight ([#31](https://github.com/o/r/pull/31))" "$notes"
check "dépendances" "## Dépendances" "$notes"
check "changement important" "## ⚠️ Changements importants"$'\n\n'"- Drop Android 10" "$notes"
check "APK" "alwaysclock-v0.2.0.apk" "$notes"
check "comparaison" "https://github.com/o/r/compare/v0.1.0...v0.2.0" "$notes"
refute "ci masqué" "actions/cache" "$notes"
refute "docs masqué" "README" "$notes"
refute "commit non conventionnel ignoré" "Merge branch" "$notes"

refute "changement important listé une seule fois" "## Nouveautés"$'\n\n'"- Add an analog clock style ([#30](https://github.com/o/r/pull/30))"$'\n'"- Drop" "$notes"

notes="$(./release-notes.sh o/r v0.1.0 v0.1.1 <<<"ci: release workflow")"
check "rien de visible" "Améliorations internes" "$notes"

notes="$(./release-notes.sh o/r "" v0.1.0 <<<"ci: release workflow")"
check "première release" "Première version publiée." "$notes"
refute "pas de comparaison pour la première release" "compare" "$notes"

(( failures == 0 )) && echo "release-notes : tous les tests passent"
exit "$failures"
