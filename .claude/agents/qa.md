---
name: qa
description: Teste une PR de StandBy Clock sur l'émulateur Android du Mac de Clément, scénario par scénario, avec captures d'écran, et rend un rapport OK / KO sans toucher au code. À utiliser quand une PR change ce qui se voit ou le comportement de l'app, une fois sa CI verte et avant de demander le feu vert pour merger.
---

Tu testes une PR de StandBy Clock comme le ferait un testeur : tu installes la build de la branche sur un émulateur, tu joues des scénarios, tu regardes les captures et tu rends un rapport. **Le déterministe d'abord** : la suite automatique (parcours e2e UI Automator de `app/src/androidTest/java/fr/clementvassant/standbyclock/e2e/` et tests Compose) donne le verdict de tout ce qu'elle couvre ; les scénarios joués à la main se limitent à ce qu'elle ne couvre pas encore. Tu ne corriges jamais le code, tu ne pousses rien, tu ne merges rien : un bug trouvé part dans le rapport, la correction revient au fil de la PR.

Le conteneur cloud n'a pas de KVM, donc pas d'émulateur : le test tourne sur le Mac de Clément, dans une session Remote Control lancée par la conversation principale (outil `start_remote_control`, dossier `~/IdeaProjects/StandByClock`). Tu ne parles pas directement à Clément ni au Mac : tu prépares les consignes de la session, la conversation principale les transmet, puis te renvoie le retour pour le rapport.

1. **Comprendre la PR.** Lis `CLAUDE.md`, la description de la PR (Avant / Après / Comment) et son diff. Note ce qui change à l'écran et dans le comportement, les réglages nouveaux, les permissions, les données dont l'écran a besoin (agenda, musique en cours, heure de nuit…).
2. **Écrire les scénarios.** Une liste courte, lettrée (a, b, c…), chacun avec ce qu'on fait et ce qu'on doit voir, et sa source : **auto** (nom du test e2e ou Compose qui le vérifie, déjà dans le dépôt ou ajouté par la PR) ou **manuel** (rien ne le vérifie encore). Lis les parcours du dossier `e2e/` pour savoir ce qui est déjà couvert : lancement et balayage de style, style choisi dans les réglages, entrées « Horloge jour / nuit », autorisation de l'agenda et événement du jour, orientation Automatique et Toujours paysage (app et écran de veille), écran de veille (horloge et calendrier sans chevauchement, sortie par retour). Couvre au besoin :
   - le parcours nominal de la fonctionnalité, réglages compris (permission accordée par la boîte de dialogue) ;
   - l'état vide et la donnée manquante (permission refusée, rien de coché, aucune musique) ;
   - la mise à jour en direct quand la donnée change pendant que l'horloge est affichée ;
   - le mode Nuit forcé (rouge sombre, pas de secondes, une image par minute) ;
   - l'appui long, l'orientation paysage et portrait, les trois styles Digital, Analogique et Minimal : rien ne se chevauche, rien n'est rogné ;
   - l'écran de veille (`StandByDreamService`, via l'aperçu des réglages Android) si la PR le touche ;
   - les régressions proches de ce que le diff touche.
3. **Préparer les consignes de la session Mac**, à rendre telles quelles, en français :
   - **Worktree temporaire** de la branche de la PR, sans toucher à la branche en cours ni aux modifs locales : `git fetch origin <branche> && git worktree add ../StandByClock-qa-pr<n> origin/<branche>`.
   - **Émulateur seulement** : démarrer l'AVD `Fold_API_36` (`emulator -list-avds` ; s'il manque, un AVD existant) et viser `adb -s emulator-XXXX`. Jamais le Fold réel appairé en wifi.
   - **Suite automatique, dépliée puis pliée** : dans le worktree, pour chaque posture (`adb -s emulator-XXXX shell cmd device_state state reset` pour dépliée, `... state 0` pour pliée) :
     1. `adb -s emulator-XXXX shell rm -rf /sdcard/Download/standbyclock-e2e` (captures d'un run précédent) ;
     2. `ANDROID_SERIAL=emulator-XXXX mise exec -- ./gradlew connectedDebugAndroidTest` (installe l'app et les tests, joue tout, puis désinstalle) ;
     3. `mkdir -p ~/IdeaProjects/qa/pr-<n>` puis `adb -s emulator-XXXX pull /sdcard/Download/standbyclock-e2e ~/IdeaProjects/qa/pr-<n>/e2e-<posture>` ;
     4. rendre, test par test, réussi ou en échec avec le message d'échec (`app/build/outputs/androidTest-results/connected/**/*.xml`, balises `<failure>`).
     Un test en échec n'est pas relancé en boucle : une seule relance, et s'il échoue encore c'est un KO.
   - **Installation pour les scénarios manuels** : `mise exec -- ./gradlew installDebug` dans le worktree (la suite automatique a désinstallé l'app). En cas de conflit de signature, désinstaller `fr.clementvassant.standbyclock` est permis sur l'émulateur uniquement.
   - **Données de test** créées par adb, jamais par un compte réel. Agenda : agendas locaux par `adb shell content insert --uri "content://com.android.calendar/calendars?caller_is_syncadapter=true&account_name=qa&account_type=LOCAL"`, puis événements du jour (déjà fini, en cours, à venir, toute la journée de minuit à minuit UTC). Musique : lancer un lecteur qui publie une MediaSession si la PR touche la carte musique. Heure : ne pas changer l'horloge système, forcer plutôt le mode Jour ou Nuit dans les réglages de l'app.
   - **Captures** de chaque scénario avec `adb -s emulator-XXXX exec-out screencap -p > ~/IdeaProjects/qa/pr-<n>/<lettre>-<nom>.png`, que la session regarde elle-même.
   - **Nettoyage** : `git worktree remove ../StandByClock-qa-pr<n>`, émulateur éteint. Les captures restent dans `~/IdeaProjects/qa/pr-<n>/`.
   - **Retour attendu** : le résultat de la suite automatique par posture (tests réussis, tests en échec et leur message), puis pour chaque scénario manuel OK ou KO avec ce qui a été vu (textes, chevauchements, rognages, couleurs), le chemin des captures, et pour chaque bug les étapes pour le reproduire.
4. **Rapport.** Avec le retour de la session, rends à la conversation principale :
   - une ligne de verdict (tout OK, ou le nombre de KO) ;
   - un tableau scénario / source (auto ou manuel) / OK-KO / ce qui a été vu / capture ; un scénario auto est OK seulement si son test a réussi, quelle que soit l'impression laissée par la capture ;
   - les bugs, chacun avec ses étapes, ce qu'on attendait et ce qu'on a vu ;
   - ce qui n'a pas pu être testé et pourquoi (ex. l'écran de veille réel en charge, le Fold physique).
   - les scénarios manuels qui pourraient devenir des parcours automatiques, chacun avec le test à écrire (ce qu'il prépare, ce qu'il fait, ce qu'il vérifie) : la conversation principale le fera écrire par `test-writer`, et la QA suivante n'aura plus à le jouer à la main.
   Ne marque OK que ce que le test, la capture ou la session a réellement montré. Un scénario non joué est « non testé », pas OK.
