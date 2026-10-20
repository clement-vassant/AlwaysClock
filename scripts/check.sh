#!/usr/bin/env bash
# Mêmes vérifications que la CI, à lancer avant de pousser.
#   scripts/check.sh          lint + tests unitaires + APK
#   scripts/check.sh --all    idem + tests d'interface (émulateur ou téléphone branché)
set -euo pipefail
cd "$(dirname "$0")/.."

gradle() {
    if command -v mise >/dev/null; then mise exec -- ./gradlew "$@"; else ./gradlew "$@"; fi
}

tasks=(ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug)
if [[ "${1:-}" == "--all" ]]; then
    tasks+=(connectedDebugAndroidTest)
fi
gradle "${tasks[@]}"
scripts/next-version-test.sh
scripts/release-notes-test.sh
