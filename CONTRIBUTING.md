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
| APK debug et release | `assembleDebug assembleRelease` | l'app compile dans les deux variantes ; l'APK debug est téléchargeable dans les artefacts du run |
| `ci-ok` | | vert seulement si tous les autres le sont ; c'est lui que la protection de `main` exige |

Dependabot propose chaque semaine les mises à jour Gradle et chaque mois celles des actions GitHub ; elles passent par la même CI.

## Titres de PR et releases

Les PR sont mergées en squash et leur titre suit [Conventional Commits](https://www.conventionalcommits.org/fr/), vérifié par le workflow « Titre de PR » :

| Préfixe | Effet sur la version (SemVer) | Dans les notes de release |
|---|---|---|
| `feat:` | mineure (0.1.0 → 0.2.0) | Nouveautés |
| `fix:`, `perf:` | correctif (0.1.0 → 0.1.1) | Corrections, Performances |
| `deps:` | correctif | Dépendances (PR Dependabot Gradle) |
| `feat!:` ou pied `BREAKING CHANGE:` | majeure (avant 1.0 : mineure) | mis en avant |
| `docs:`, `test:`, `refactor:`, `ci:`, `build:`, `chore:` | aucune release | masqués |

Le workflow `release.yml` ([release-please](https://github.com/googleapis/release-please)) tient à jour une PR « chore: release X.Y.Z » qui modifie `version.txt` et `CHANGELOG.md`. La CI y est lancée automatiquement. **Merger cette PR publie la release** : tag `vX.Y.Z`, release GitHub avec ses notes et l'APK signé `alwaysclock-vX.Y.Z.apk` (avec son empreinte SHA-256). `versionName` vaut `X.Y.Z` et `versionCode` `X*10000 + Y*100 + Z`, calculés depuis `version.txt`. Pour forcer un numéro, ajouter le pied `Release-As: X.Y.Z` au message du commit de merge.

### Clé de signature

Android n'accepte une mise à jour que si elle est signée avec la même clé que la version installée. Sans clé, la release est signée avec une clé debug jetable (avertissement dans le run) : l'APK s'installe, mais la release suivante ne pourra pas le mettre à jour sans désinstaller. Pour signer les releases, une seule fois :

```bash
keytool -genkeypair -v -keystore alwaysclock-release.jks -alias alwaysclock \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i alwaysclock-release.jks | pbcopy   # macOS ; sous Linux : base64 -w0 alwaysclock-release.jks
```

Puis dans **Settings > Secrets and variables > Actions** du dépôt, créer `RELEASE_KEYSTORE_BASE64` (le contenu copié), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`alwaysclock`) et `RELEASE_KEY_PASSWORD`. Garder le fichier `.jks` et ses mots de passe en lieu sûr hors du dépôt : perdus, plus aucune mise à jour n'est possible.

## Écrire du code lisible

- Des noms qui disent ce qu'ils sont : pas d'abréviations obscures, des constantes nommées plutôt que des nombres magiques.
- Une fonction `@Composable` fait une chose ; au-delà d'une soixantaine de lignes, la découper.
- La logique (calculs de dates, de tailles, de jour/nuit) vit dans des fonctions pures testées en JVM, pas dans l'UI.
- Les commentaires expliquent le *pourquoi* (contrainte Android, choix anti-marquage), pas le *quoi*.
- Le seuil d'un outil ne se contourne pas avec `@Suppress` sans un commentaire qui dit pourquoi.

## Avec Claude Code

`CLAUDE.md` reprend ces règles pour Claude, et `.claude/agents/` définit des agents spécialisés : `code-reviewer` (relecture du diff), `test-writer` (tests), `ci-doctor` (CI rouge) et `compose-ui` (vues Compose).
