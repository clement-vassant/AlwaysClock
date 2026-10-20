# StandBy Clock (Android)

Horloges plein écran en paysage, façon mode StandBy de l'iPhone, pensées pour un écran OLED (Galaxy Z Fold).

## Pourquoi pas le vrai Always On Display ?

Samsung ne laisse aucune app tierce dessiner sur l'AOD. L'app fonctionne donc de deux façons :

- **Écran de veille Android** (automatique) : se lance tout seul quand le téléphone charge.
- **App plein écran** (manuelle) : ouvrir l'app, le téléphone tourne en paysage et l'écran reste allumé.

Dans les deux cas : fond noir pur (pixels OLED éteints), pas de blanc pur, décalage de quelques pixels chaque minute contre le marquage, et un mode nuit rouge.

## Utilisation

- **Balayer à gauche/droite** : changer de style (Digital, Analogique, Minimal). Le choix est mémorisé.
- **Toucher l'écran** : basculer le mode nuit (rouge sombre, luminosité minimale dans l'app).
- **Appui long** : afficher ou masquer le calendrier du mois à droite de l'horloge (affiché par défaut).
- **Double toucher** : ouvrir les réglages (dans l'app ; depuis l'écran de veille, bouton ⚙ des réglages Android).

## Jour, nuit et programmation

Dans les **réglages** (double toucher, ou appui long sur l'icône > *Réglages StandBy*) :
- **Auto** : nuit rouge sur une plage horaire (22:00 → 07:00 par défaut), jour le reste du temps.
- **Jour** ou **Nuit** : toujours ce mode.

L'app ajoute deux entrées en plus de *StandBy Clock* : **Horloge jour** et **Horloge nuit**, qui forcent le mode pour ce lancement. Elles servent à **Modes et routines** de Samsung :
1. Paramètres > Modes et routines > Routines > **+**.
2. *Si* : par exemple **Heure** (22:30, tous les jours) et/ou **En charge**, ou le début d'un mode (Sommeil...).
3. *Alors* : **Applications > Ouvrir une app** > *Horloge nuit* (ou *Horloge jour*, ou *StandBy Clock* pour le mode Auto).

Un toucher sur l'horloge inverse jour/nuit jusqu'à la prochaine bascule programmée. Les deux entrées supplémentaires peuvent être masquées de l'écran d'accueil (Paramètres de l'écran d'accueil > Masquer des applications) ; elles devraient rester proposées dans les routines (non vérifié).

## Protection de l'écran

Samsung conseille d'éviter les images fixes affichées longtemps, de baisser la luminosité et de préférer les fonds sombres. L'app applique :

- **Fond noir pur** : les pixels OLED du fond restent éteints.
- **Orbite anti-marquage** : tout le contenu se déplace chaque minute sur une spirale lente jusqu'à ±20 dp, sans jamais rester deux minutes au même endroit.
- **Inversion des côtés chaque heure** : heures paires, horloge à gauche et calendrier à droite ; heures impaires, l'inverse.
- **Moins de pixels allumés** : pas de blanc pur (gris clair `#BDBDBD` le jour), jour du calendrier entouré d'un anneau fin plutôt que d'un disque plein, mode nuit rouge sombre.
- **Luminosité plafonnée** : 35 % le jour, minimale la nuit (dans l'app) ; l'écran de veille demande un écran atténué.
- **Arrêt automatique** : réglage *Éteindre après* (1 h, 2 h, 4 h, 8 h, Jamais ; 2 h par défaut). Le délai écoulé, l'horloge se ferme et l'écran peut s'éteindre normalement.

## Compiler l'APK

### Avec Android Studio (le plus simple)
1. Ouvrir le dossier du projet dans Android Studio (Ladybug ou plus récent).
2. Laisser la synchronisation Gradle se terminer (accepter les mises à jour proposées si besoin).
3. Menu **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. L'APK est dans `app/build/outputs/apk/debug/app-debug.apk`.

### En ligne de commande
Prérequis : le SDK Android (`ANDROID_HOME` défini, ou un fichier `local.properties` avec `sdk.dir=...`) et Java 17, géré par [mise](https://mise.jdx.dev) via `mise.toml` (Gradle 8.14 refuse les JDK trop récents). Installer mise, puis :
```bash
mise install
mise exec -- ./gradlew assembleDebug
```
Dans un shell où mise est activé, `./gradlew assembleDebug` suffit.

### Sans rien installer (GitHub)
Pousser ce dossier dans un repo GitHub : le workflow `.github/workflows/build.yml` compile l'APK et le met en téléchargement dans l'onglet **Actions**.

## Installer sur le téléphone

- **Par câble** : activer les options développeur (Paramètres > À propos du téléphone > Informations sur le logiciel > taper 7 fois sur "Numéro de version"), activer le débogage USB, puis `adb install app-debug.apk` (ou bouton ▶ Run d'Android Studio).
- **Sans câble** : copier l'APK sur le téléphone, l'ouvrir depuis Mes fichiers et autoriser l'installation d'apps inconnues pour Mes fichiers.

## Activer le mode automatique en charge

Paramètres > Affichage > **Écran de veille** (sur certaines versions de One UI : Paramètres > Affichage > Écran de veille / *Screen saver*) :
1. Choisir **StandBy Clock**.
2. Quand démarrer : **Pendant la charge**.

Astuce : le bouton ⚙ de l'écran de veille ouvre les réglages de l'app.

## Structure

```
app/src/main/java/io/shodo/standbyclock/
  MainActivity.kt          activité plein écran, paysage, écran allumé
  StandByDreamService.kt   écran de veille Android (charge)
  SettingsActivity.kt      réglages (style, calendrier, jour/nuit, plage de nuit)
  ClockPrefs.kt            préférences partagées + calcul jour/nuit
  ui/StandByScreen.kt      pager des styles, anti-marquage, mode nuit, calendrier
  ui/Clocks.kt             les styles d'horloge et le calendrier du mois
```

Pour ajouter un style : une valeur dans `ClockStyle`, un `@Composable` dans `Clocks.kt`, une ligne dans le `when` de `StandByScreen.kt`.
