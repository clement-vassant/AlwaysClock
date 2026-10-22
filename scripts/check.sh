#!/usr/bin/env bash
# Mêmes vérifications que la CI, à lancer avant de pousser.
#   scripts/check.sh          lint + tests unitaires + captures de référence + APK
#   scripts/check.sh --all    idem + tests d'interface et parcours e2e (émulateur ou téléphone branché)
set -euo pipefail
cd "$(dirname "$0")/.."

gradle() {
    if command -v mise >/dev/null; then mise exec -- ./gradlew "$@"; else ./gradlew "$@"; fi
}

# Les images de référence sont rendues sous Linux, comme sur la CI : ailleurs (macOS), le rendu des polices diffère
# un peu, donc on produit seulement le rapport de comparaison (app/build/outputs/roborazzi) sans échouer.
if [[ "$(uname -s)" == "Linux" ]]; then screenshots=verifyRoborazziDebug; else screenshots=compareRoborazziDebug; fi

tasks=(ktlintCheck detekt lintDebug testDebugUnitTest "$screenshots" assembleDebug)
if [[ "${1:-}" == "--all" ]]; then
    tasks+=(connectedDebugAndroidTest)
fi
gradle "${tasks[@]}"
