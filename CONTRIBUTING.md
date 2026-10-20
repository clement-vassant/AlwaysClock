# Contribuer

## Règles

- **Jamais de push direct sur `main`.** Chaque changement passe par une branche et une pull request dédiée, un sujet par PR.
- **Une PR n'est mergeable que si la CI est verte** (job `ci-ok`) et que la branche est à jour avec `main`.
- **Chaque changement arrive avec ses tests** : test unitaire JVM pour la logique, test Compose pour ce qui se voit à l'écran.

## Avant de pousser

```bash
mise install           # Java 17, une seule fois
scripts/check.sh       # ktlint, detekt, Android Lint, tests unitaires, APK
scripts/check.sh --all # idem + tests d'interface (émulateur ou téléphone branché)
```

`mise exec -- ./gradlew ktlintFormat` corrige automatiquement la mise en forme.

## Ce que vérifie la CI

| Job | Commande | Rôle |
|---|---|---|
| Lint | `ktlintCheck detekt lintDebug` | style Kotlin (ktlint + règles Compose), code smells (detekt + règles Compose), erreurs Android (Lint, avertissements bloquants) |
| Tests unitaires | `testDebugUnitTest` | logique pure : plage de nuit, calendrier, anti-marquage, musique… |
| Tests d'interface | `connectedDebugAndroidTest` | écran réel sur un émulateur API 34, à heure fixe |
| APK debug | `assembleDebug` | l'app compile ; l'APK est téléchargeable dans les artefacts du run |
| `ci-ok` | | vert seulement si tous les autres le sont ; c'est lui que la protection de `main` exige |

Dependabot propose chaque semaine les mises à jour Gradle et chaque mois celles des actions GitHub ; elles passent par la même CI.

## Écrire du code lisible

- Des noms qui disent ce qu'ils sont : pas d'abréviations obscures, des constantes nommées plutôt que des nombres magiques.
- Une fonction `@Composable` fait une chose ; au-delà d'une soixantaine de lignes, la découper.
- La logique (calculs de dates, de tailles, de jour/nuit) vit dans des fonctions pures testées en JVM, pas dans l'UI.
- Les commentaires expliquent le *pourquoi* (contrainte Android, choix anti-marquage), pas le *quoi*.
- Le seuil d'un outil ne se contourne pas avec `@Suppress` sans un commentaire qui dit pourquoi.

## Avec Claude Code

`CLAUDE.md` reprend ces règles pour Claude, et `.claude/agents/` définit des agents spécialisés : `code-reviewer` (relecture du diff), `test-writer` (tests), `ci-doctor` (CI rouge) et `compose-ui` (vues Compose).
