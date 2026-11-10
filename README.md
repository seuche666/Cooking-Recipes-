# Rezeptbuch – Android-Rezept-App

Native Android-App für eigene Rezepte. Kotlin, Jetpack Compose (Material 3), MVVM,
Room, Hilt, Navigation Compose. Mindest-SDK 26 (Android 8.0), Target-SDK 35.

## Funktionen

| Bereich | Umfang |
|---|---|
| **Rezepte** | Anlegen, Bearbeiten, Löschen; Titel, Foto (Galerie oder Kamera), Grundportionen, Zutaten (Menge · Einheit · Name), Schritte mit optionalem Timer, Kochzeit; Liste mit Titelsuche |
| **Notizen** | Beliebig viele datierte Notizen pro Rezept, direkt auf der Detailseite anlegen, bearbeiten, löschen |
| **Portionsrechner** | Plus/Minus auf der Detailseite, Mengen werden live umgerechnet und sinnvoll gerundet; gespeicherte Daten bleiben unverändert |
| **Kochmodus** | Vollbild, ein Schritt pro Seite, große Schrift, Wischen oder Buttons, Fortschrittsbalken, Bildschirm bleibt an, Timer laufen im Hintergrund weiter und melden sich per Benachrichtigung mit Alarmton, ausklappbares Zutaten-Panel mit gewählter Portionszahl |
| **Einstellungen** | Hell-/Dunkel-/Systemmodus, Backup-Export und -Import (ZIP) über das Storage Access Framework |
| **Seed-Daten** | 3 Beispielrezepte beim ersten Start (Carbonara, Pfannkuchen, Linsensuppe) |

## App installieren (ohne eigenen Computer)

Gebaut wird alles über **GitHub Actions** (`.github/workflows/android.yml`), Android Studio ist nicht nötig:

- **Pull Request:** Die Tests laufen, dann wird eine Test-APK gebaut. Sie liegt im Workflow-Lauf unter
  **Actions → Lauf auswählen → Artifacts** als `Rezeptbuch-1.0.X` (ZIP mit der APK darin).
  Ohne Signier-Secrets ist diese APK unsigniert und nur zur Kontrolle gedacht.
- **Merge auf `main`:** Die APK wird signiert und als **Release** veröffentlicht:
  1. Im Repository rechts auf **Releases** tippen, oder `https://github.com/<besitzer>/<repo>/releases/latest` öffnen.
  2. Unter **Assets** die Datei `Rezeptbuch-1.0.X.apk` herunterladen.
  3. Die Datei öffnen. Beim ersten Mal fragt Android, ob der Browser „unbekannte Apps installieren“ darf:
     erlauben, zurück und **Installieren** tippen.
  4. Updates werden genauso installiert. Die Rezepte bleiben dabei erhalten.

Die Versionsnummer ist `1.0.<Laufnummer>`. Deshalb lässt sich jede neue APK als Update installieren.

### Signierung über GitHub-Secrets

Jede Release-APK wird mit demselben **geheimen** Schlüssel signiert, damit sich neue Versionen als Update
installieren lassen. Der Schlüssel liegt **nicht** im Repository, sondern in GitHub-Secrets
(`Settings → Secrets and variables → Actions → New repository secret`):

| Secret | Inhalt |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Die Keystore-Datei (`.jks`) als Base64-Text |
| `SIGNING_PASSWORD` | Passwort des Keystores (gleichzeitig Passwort des Schlüssels) |
| `SIGNING_KEY_ALIAS` | optional. Ohne Angabe wird der erste Alias im Keystore verwendet. |

- Du kannst denselben Keystore wie in einem anderen Projekt verwenden (gleiche Werte eintragen).

**Neuen Schlüssel ohne Computer erzeugen** (Workflow `.github/workflows/signing-key.yml`):

1. Ein langes, zufälliges Passwort erzeugen (mindestens 16 Zeichen, z. B. im Passwort-Manager) und als
   Secret `SIGNING_PASSWORD` anlegen.
2. **Actions → Signierschlüssel erzeugen → Run workflow** starten.
3. Im fertigen Lauf das Artifact `signierschluessel` herunterladen und entpacken. Den gesamten Inhalt
   von `SIGNING_KEYSTORE_BASE64.txt` als Secret `SIGNING_KEYSTORE_BASE64` anlegen.
4. `rezeptbuch-release.jks` zusammen mit dem Passwort sicher aufbewahren und das Artifact im
   Workflow-Lauf löschen. Es wird ohnehin nach einem Tag automatisch entfernt.
5. Den fehlgeschlagenen Lauf von **Android-App bauen** auf `main` mit **Re-run all jobs** neu starten.
   Danach erscheint das Release.

Der Keystore ist mit dem Passwort verschlüsselt. Ohne das Passwort ist die Datei nutzlos, auch wenn
jemand das Artifact herunterlädt. Ist `SIGNING_KEYSTORE_BASE64` schon gesetzt, bricht der Workflow ab,
damit kein vorhandener Schlüssel versehentlich ersetzt wird.
- Bei einem Merge auf `main` **ohne** diese Secrets bricht der Workflow mit einer Fehlermeldung ab.
  Es wird nie eine unsignierte App veröffentlicht.
- **Keystore und Passwort sicher aufbewahren** (z. B. im Passwort-Manager). Gehen sie verloren,
  lässt sich die App nicht mehr aktualisieren, sondern nur deinstallieren und neu installieren.
  Vorher eine Datensicherung exportieren!

## Setup in Android Studio

1. **Android Studio** Ladybug (2024.2) oder neuer installieren. Es bringt ein passendes JDK 17+ mit.
2. Repository klonen und in Android Studio über **File → Open** den Projektordner öffnen.
3. `local.properties` anlegen (siehe nächster Abschnitt). Android Studio trägt `sdk.dir` beim ersten
   Öffnen meist selbst ein. Fehlt die Datei, läuft der Build trotzdem: Dann gelten die Platzhalter aus
   `secrets.defaults.properties`.
4. Gradle-Sync abwarten. Fehlende SDK-Plattformen (API 35) schlägt Android Studio automatisch zur Installation vor.
5. Run-Konfiguration **app** auf einem Emulator oder Gerät (Android 8.0+) starten.

Kommandozeile:

```bash
./gradlew assembleDebug          # Debug-APK
./gradlew testDebugUnitTest      # Unit-Tests (Portionsumrechnung)
./gradlew assembleRelease        # Release-Build mit R8 (Signierung siehe unten)
```

## `local.properties` – Schlüssel und Signierung

**Keine API-Keys, Passwörter oder Keystores im Quellcode oder im Repository.** Alle Geheimnisse stehen
in `local.properties` (von Git ignoriert) oder in Umgebungsvariablen. Eine Vorlage liegt in
[`local.properties.example`](local.properties.example).

| Eintrag | Pflicht | Zweck |
|---|---|---|
| `sdk.dir` | ja (setzt Android Studio) | Pfad zum Android SDK |
| `RECIPE_API_KEY` | nein | API-Schlüssel; das **Secrets Gradle Plugin** stellt ihn als `BuildConfig.RECIPE_API_KEY` bereit (Zugriff über `ApiKeyProvider`). Ohne Eintrag gilt der Platzhalter aus `secrets.defaults.properties`. |
| `RELEASE_STORE_FILE` | nur für signierte Releases | Absoluter Pfad zur Keystore-Datei (`.jks`), die **außerhalb** des Repos liegen sollte |
| `RELEASE_STORE_PASSWORD` | nur für signierte Releases | Passwort des Keystores |
| `RELEASE_KEY_ALIAS` | nur für signierte Releases | Alias des Schlüssels |
| `RELEASE_KEY_PASSWORD` | nur für signierte Releases | Passwort des Schlüssels |

Beispiel:

```properties
sdk.dir=/Users/ich/Library/Android/sdk
RECIPE_API_KEY=abc123
RELEASE_STORE_FILE=/Users/ich/keys/rezeptbuch.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=rezeptbuch
RELEASE_KEY_PASSWORD=…
```

Die vier `RELEASE_*`-Werte können alternativ als **Umgebungsvariablen** gleichen Namens gesetzt werden.
Außerdem versteht der Build `SIGNING_KEYSTORE_PATH`, `SIGNING_PASSWORD` und `SIGNING_KEY_ALIAS`; diese
Variablen setzt der GitHub-Workflow. Ist kein Keystore konfiguriert, entsteht ein unsignierter Release-Build.

Keystore erzeugen (einmalig):

```bash
keytool -genkeypair -v -keystore ~/keys/rezeptbuch.jks -alias rezeptbuch \
        -keyalg RSA -keysize 4096 -validity 10000
```

`.gitignore` schließt u. a. `local.properties`, `*.jks`, `*.keystore`, `*.p12` und
`google-services.json` aus. Release-Builds laufen mit **R8** (`isMinifyEnabled`, `isShrinkResources`);
Regeln stehen in `app/proguard-rules.pro`.

## Architektur

MVVM mit unidirektionalem Datenfluss: Compose-Screen → ViewModel (`StateFlow`) → Repository-Interface
(domain) → Implementierung (data) → Room / Dateisystem. Abhängigkeiten werden per Hilt injiziert.

```
de.rezeptbuch.app
├── RezeptbuchApp.kt, MainActivity.kt
├── di/                     Hilt-Module (Room, DataStore, Repository-Bindings, App-Scope)
├── domain/                 reines Kotlin, keine Android-UI-Abhängigkeiten
│   ├── model/              Recipe, Ingredient, IngredientUnit, CookingStep, RecipeNote, ThemeMode …
│   ├── repository/         Interfaces: RecipeRepository, SettingsRepository, BackupRepository
│   └── portion/            PortionCalculator (Umrechnung + Rundung), TimerInput, Zeitformatierung
├── data/
│   ├── local/              Room: AppDatabase, Entities, RecipeDao, Converter, Mapper
│   ├── repository/         RecipeRepositoryImpl
│   ├── photo/              PhotoStorage (Fotos in filesDir/photos, Kamera-URIs via FileProvider)
│   ├── backup/             ZIP-Export/-Import, Backup-Modelle, Format-Migrationen
│   ├── settings/           DataStore (Theme, Seed-Flag)
│   ├── seed/               Beispielrezepte
│   └── remote/             ApiKeyProvider (BuildConfig-Key)
├── service/timer/          CookingTimerManager, Foreground-Service, Benachrichtigungen
└── ui/
    ├── navigation/         typsichere Routen + NavHost
    ├── list/ detail/ edit/ cooking/ settings/   Screens + ViewModels
    ├── components/         wiederverwendbare Composables
    └── theme/              Material-3-Farbschemata (hell/dunkel)
```

### Datenmodell (Room)

```
recipes      id PK · uuid (unique) · title · photo_file_name? · base_servings · cook_time_minutes? · created_at · updated_at
ingredients  id PK · recipe_id FK→recipes (CASCADE) · position · amount? · unit (Enum-Name) · name
steps        id PK · recipe_id FK→recipes (CASCADE) · position · text · timer_seconds?
notes        id PK · recipe_id FK→recipes (CASCADE) · text · created_at · updated_at
```

- `uuid` identifiziert ein Rezept geräteübergreifend und wird beim Backup-Import zum Zusammenführen genutzt.
- `amount = null` bedeutet „ohne Mengenangabe“ (z. B. „Pfeffer“).
- `IngredientUnit` ist ein Enum (g, kg, ml, l, TL, EL, Tasse, Stück, Prise, Zehe, Bund, Dose, Pck., Scheibe, ohne).
  Jede Einheit trägt eine Rundungsregel.
- Fotos liegen als Datei in `filesDir/photos`; die Datenbank speichert nur den Dateinamen.

### Rundung im Portionsrechner

| Regel | Einheiten | Verhalten | Beispiele |
|---|---|---|---|
| `WHOLE` | g, ml | ganze Zahl; unter 1 eine Nachkommastelle | 166,7 g → `167 g`, 0,5 g → `0,5 g` |
| `DECIMAL` | kg, l, TL, EL, Tasse | unter 10 eine Nachkommastelle, sonst ganzzahlig | 1,5 EL → `1,5 EL`, 12,5 EL → `13 EL` |
| `COUNTABLE` | Stück, Prise, Zehe, Bund, Dose, Pck., Scheibe, ohne Einheit | ganze Zahl, Zwischenwerte als Spanne, mindestens 1 | 1,5 Eier → `1–2 Eier`, 3,0 → `3` |

Die Umrechnung ist reine Anzeige-Logik (`PortionCalculator`). Die gewählte Portionszahl wird nie gespeichert.

## Backup-Format

`rezepte-backup-JJJJ-MM-TT.zip`:

```
backup.json        { "formatVersion": 1, "appVersion": "1.0.0", "exportedAt": "…", "recipes": [ … ] }
images/<datei>.jpg  Rezeptfotos, referenziert über "photo": "images/<datei>.jpg"
```

- **Versionierung:** `formatVersion` wird bei inkompatiblen Änderungen erhöht. `BackupMigrations`
  hebt ältere Backups schrittweise auf das aktuelle Format. Backups aus *neueren* App-Versionen
  werden mit einer klaren Meldung abgelehnt. Neue Felder bekommen immer Default-Werte, unbekannte Felder
  werden ignoriert.
- **Import „Ersetzen“:** Alle Rezepte, Notizen und Fotos werden gelöscht und durch das Backup ersetzt.
- **Import „Zusammenführen“:** Vorhandenes bleibt erhalten. Rezepte mit gleicher `uuid` werden nur
  überschrieben, wenn die Backup-Fassung neuer ist (`updatedAt`).
- Der Import läuft in einer Datenbank-Transaktion. Bei einem Fehler bleibt der alte Stand erhalten.
  ZIP-Einträge werden nie als Dateipfade verwendet (Schutz vor „Zip Slip“).

## Berechtigungen

| Berechtigung | Wofür |
|---|---|
| `POST_NOTIFICATIONS` | Timer-Benachrichtigungen (Android 13+, wird beim ersten Timer-Start angefragt) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Timer laufen im Hintergrund weiter |
| `WAKE_LOCK` | CPU bleibt wach, solange ein Timer läuft |

Die Kamera wird über die System-Kamera-App (`TakePicture`) genutzt, dafür ist keine Kamera-Berechtigung
nötig. Für die Galerie dient der System-Photo-Picker, für Backups das Storage Access Framework.
