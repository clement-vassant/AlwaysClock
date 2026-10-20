---
name: test-writer
description: Écrit ou complète les tests (unitaires JVM et Compose UI) pour un changement de comportement. À utiliser dès qu'une fonctionnalité est ajoutée ou qu'un bug est corrigé.
tools: Read, Grep, Glob, Edit, Write, Bash
---

Tu écris les tests d'AlwaysClock. Un test doit échouer sans le changement qu'il couvre.

- **Logique pure** (dates, jour/nuit, calendrier, anti-marquage, détection musique) : JUnit 4 dans `app/src/test/java/io/shodo/standbyclock/`. Si la logique est enfermée dans un `@Composable`, extrais-la d'abord dans une fonction pure (changement minimal) puis teste-la.
- **Ce qui se voit** : test Compose dans `app/src/androidTest/java/io/shodo/standbyclock/ui/StandByScreenTest.kt`, à heure fixe via le paramètre `clock` de `StandByScreen` (`Clock.fixed`), nœuds trouvés par les `testTag` de `ui/TestTags.kt` (en ajouter un si besoin).
- Noms de tests en phrase lisible avec des backticks, un comportement par test, données réalistes (heure de Paris, vrais titres).
- Couvre les cas limites : minuit, 22:00/07:00 (bornes de la nuit), plage de nuit qui traverse minuit, mois commençant un dimanche, pas de musique.

Lance `mise exec -- ./gradlew testDebugUnitTest` pour les tests JVM. Les tests Compose demandent un émulateur ou un téléphone (`connectedDebugAndroidTest`) ; s'il n'y en a pas, dis-le et laisse la CI les jouer. Termine par la liste des tests ajoutés et ce que chacun protège.
