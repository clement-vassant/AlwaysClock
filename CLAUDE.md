# StandBy Clock : consignes pour Claude

App Android (Kotlin, Jetpack Compose) qui affiche une grande horloge en paysage pendant la charge, façon StandBy.
Package `io.shodo.standbyclock`, minSdk 29, Java 17 via `mise.toml`. Réponses et textes visibles en français.

## Règles de travail

- **Jamais de push sur `main`.** Une branche et une pull request par sujet ; la PR n'est mergeable que si le job `ci-ok` est vert.
- **Pas de changement sans test.** Logique : test unitaire JVM dans `app/src/test`. Ce qui se voit : test Compose dans `app/src/androidTest`.
- **Avant de pousser :** `scripts/check.sh` (ktlint, detekt, Android Lint, tests unitaires, APK). `mise exec -- ./gradlew ktlintFormat` corrige la mise en forme.
- **Ne jamais affaiblir un garde-fou pour passer la CI** : pas de test désactivé, pas de `@Suppress`, `tools:ignore` ni règle désactivée sans commentaire qui explique pourquoi c'est voulu.
- Messages de commit et titres de PR en anglais, au format [Conventional Commits](https://www.conventionalcommits.org/fr/) (`feat:`, `fix:`, `docs:`, `ci:`…) : le bouton de release en déduit la prochaine version. Description de PR au format « Avant / Après / Comment » du modèle `.github/pull_request_template.md`.
- La version n'est écrite nulle part dans le dépôt : elle vient du dernier tag `vX.Y.Z` (voir CONTRIBUTING.md).

## Code attendu

- Noms explicites, constantes nommées (`TimeUnits.kt`) plutôt que nombres magiques.
- La logique (dates, jour/nuit, tailles, anti-marquage) vit dans des fonctions pures testables, pas dans les `@Composable`.
- Un `@Composable` fait une chose ; on découpe quand il grossit. Les nœuds testés ont un `testTag` défini dans `ui/TestTags.kt`.
- L'app est générique Android : aucun texte visible ne parle d'un modèle de téléphone (vérifié par `VisibleTextsTest`).
- Commentaires en français, pour le *pourquoi* (contrainte Android, économie d'énergie, anti-marquage OLED).

## Contraintes du produit

- L'écran toujours allumé du fabricant est inaccessible aux apps : on passe par l'écran de veille (`StandByDreamService`) et une activité plein écran.
- Anti-marquage OLED et économie d'énergie sont des exigences, pas des options : orbite, inversion des côtés chaque heure, rouge sombre la nuit, une image par minute la nuit.

## Agents spécialisés (`.claude/agents/`)

Les utiliser plutôt que tout faire dans la conversation principale :

| Agent | Quand |
|---|---|
| `code-reviewer` | avant d'ouvrir ou de finaliser une PR : relit le diff contre ces règles |
| `test-writer` | dès qu'un comportement change : écrit ou complète les tests unitaires et Compose |
| `ci-doctor` | quand un job de la CI échoue : trouve la cause et propose le correctif minimal |
| `compose-ui` | pour une nouvelle vue ou un nouveau style d'horloge : Compose, anti-marquage, nuit |
| `releaser` | quand une release est demandée : lance le workflow Release sur `main`, suit l'exécution, rend la version et l'APK |
