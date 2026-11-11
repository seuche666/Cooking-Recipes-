# Rezeptbuch – Android-Rezept-App

Native Android-App für eigene Rezepte. Kotlin, Jetpack Compose (Material 3), MVVM,
Room, Hilt, Navigation Compose. Mindest-SDK 26 (Android 8.0), Target-SDK 35.

## Funktionen

| Bereich | Umfang |
|---|---|
| **Start** | Überblick und Schnellzugriffe: Suche, Kategorien als farbige Kreise, „Zuletzt gekocht“ und „Neu hinzugefügt“ |
| **Rezepte** | Alle Rezepte mit Suche nach Name **und Zutaten** („Kartoffeln“), Umschalter „Alle Rezepte“ / „Kategorien“ (tippen oder wischen), Wischen zum Löschen mit „Rückgängig“ |
| **Kategorien** | Beliebig viele pro Rezept, selbst anlegen, umbenennen und löschen, jeweils mit Symbol und Farbe; Anzahl der Rezepte je Kategorie |
| **Rezeptverwaltung** | Anlegen, Bearbeiten, Löschen; Titel, Foto (Galerie oder Kamera), Kategorien, Grundportionen, Zutaten (Menge · Einheit · Name), Schritte mit optionalem Timer, Kochzeit |
| **Notizen** | Beliebig viele datierte Notizen pro Rezept, direkt auf der Detailseite bearbeitbar |
| **Portionsrechner** | Plus/Minus auf der Detailseite, Mengen werden live umgerechnet und sinnvoll gerundet; gespeicherte Daten bleiben unverändert |
| **Kochmodus** | Vollbild, ein Schritt pro Seite, große Schrift, Bildschirm bleibt an, Timer im Hintergrund mit Alarm, Zutaten-Panel |
| **Optionen** | Design „Hell“, „Dunkel“ oder „Wie System“ (Standard, wirkt sofort), Backup-Export und -Import (ZIP) |

### Design „Klar & Hell“

Heller, leicht bläulich-grauer Hintergrund, Schrift **Plus Jakarta Sans** (eingebettet, SIL Open Font
License, siehe `licenses/`), weiße Karten mit runden Ecken und weichem Schatten, Kategorien als farbige
Symbol-Kreise, untere Navigationsleiste mit blauem Akzent. Animationen: weicher Übergang beim Öffnen
von Unterseiten, Wischen zwischen den Reitern, leichtes Nachgeben beim Antippen, Wischen zum Löschen.
Im dunklen Design: gleicher Aufbau, dunkler Hintergrund, dunkelgraue Karten, helle Schrift und eigens
abgestimmte Kategorie-Farben.

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

Jede Release-APK muss mit demselben Schlüssel signiert sein, damit sich neue Versionen als Update
installieren lassen. Dafür braucht es **nur ein einziges Secret**, nichts muss kopiert werden:

| Secret | Inhalt |
|---|---|
| `SIGNING_PASSWORD` | Ein langes, zufälliges Passwort (mindestens 16 Zeichen, z. B. aus dem Passwort-Manager) |

Anlegen unter `Settings → Secrets and variables → Actions → New repository secret`.

So funktioniert es: `.github/scripts/derive-keystore.py` leitet bei jedem Build aus dem Passwort
denselben privaten Schlüssel ab (scrypt → EC P-256). Dazu erzeugt es ein Zertifikat, das Byte für
Byte gleich bleibt (feste Felder, deterministische ECDSA-Signatur nach RFC 6979). Der Schlüssel wird
nirgends gespeichert. Der SHA-256-Fingerabdruck des Zertifikats steht in der Zusammenfassung jedes
Release-Laufs.

- **Das Passwort ist der Schlüssel.** Bewahre es sicher auf (Passwort-Manager) und ändere das Secret
  **nie**. Ein anderes Passwort ergibt einen anderen Schlüssel, dann lassen sich installierte Versionen
  nicht mehr aktualisieren, nur deinstallieren und neu installieren.
- Weil der Code öffentlich ist, schützt allein das Passwort den Schlüssel. Deshalb muss es lang und
  zufällig sein. Die langsame Ableitung (scrypt) erschwert das Durchprobieren zusätzlich.
- Die Konstanten in `derive-keystore.py` und die festgelegte `cryptography`-Version im Workflow dürfen
  nicht geändert werden, sonst ändert sich der Schlüssel.
- Bei einem Merge auf `main` **ohne** das Secret bricht der Workflow ab. Es wird nie eine unsignierte
  App veröffentlicht. Pull Requests bauen ohne Secret eine unsignierte Test-APK.
- Frühere Secrets wie `SIGNING_KEYSTORE_BASE64` werden nicht mehr verwendet und können gelöscht werden.

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
./gradlew testDebugUnitTest      # Unit-Tests (Portionsumrechnung, DB-Migration, Backup-Format)
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
Außerdem versteht der Build `SIGNING_KEYSTORE_PATH`, `SIGNING_PASSWORD` und `SIGNING_KEY_ALIAS`. Diese
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
│   ├── model/              Recipe, Category, Ingredient, IngredientUnit, CookingStep, RecipeNote, ThemeMode …
│   ├── repository/         Interfaces: RecipeRepository, CategoryRepository, SettingsRepository, BackupRepository
│   └── portion/            PortionCalculator (Umrechnung + Rundung), TimerInput, Zeitformatierung
├── data/
│   ├── local/              Room: AppDatabase, Entities, RecipeDao, CategoryDao, Migrations, Mapper
│   ├── repository/         RecipeRepositoryImpl, CategoryRepositoryImpl
│   ├── photo/              PhotoStorage (Fotos in filesDir/photos, Kamera-URIs via FileProvider)
│   ├── backup/             ZIP-Export/-Import, Backup-Modelle, Format-Migrationen
│   ├── settings/           DataStore (Theme, Seed-Flag)
│   ├── seed/               Beispielrezepte
│   └── remote/             ApiKeyProvider (BuildConfig-Key)
├── service/timer/          CookingTimerManager, Foreground-Service, Benachrichtigungen
└── ui/
    ├── navigation/         typsichere Routen + NavHost
    ├── home/ recipes/ category/ detail/ edit/ cooking/ settings/   Screens + ViewModels
    ├── components/         wiederverwendbare Composables
    └── theme/              Farben (hell/dunkel), Schrift, Formen, Kategorie-Farben
```

### Datenmodell (Room)

```
recipes      id PK · uuid (unique) · title · photo_file_name? · base_servings · cook_time_minutes? · created_at · updated_at
ingredients  id PK · recipe_id FK→recipes (CASCADE) · position · amount? · unit (Enum-Name) · name
steps        id PK · recipe_id FK→recipes (CASCADE) · position · text · timer_seconds?
notes        id PK · recipe_id FK→recipes (CASCADE) · text · created_at · updated_at
categories   id PK · uuid (unique) · name · icon (Enum-Name) · color (Enum-Name) · position · created_at   (seit v2)
recipe_categories  recipe_id FK (CASCADE) · category_id FK (CASCADE) · PK(beide)                     (seit v2)
recipes.last_cooked_at                                                                               (seit v2)
```

- **Datenbank-Version 2.** `Migrations.MIGRATION_1_2` ergänzt nur Tabellen und eine Spalte, legt die
  vier Standard-Kategorien an und lässt alle Rezepte, Notizen und Fotos unverändert. `MigrationTest`
  (Robolectric) legt eine echte Version-1-Datenbank an und prüft die Migration gegen das neue Schema.
  `fallbackToDestructiveMigration` wird bewusst nie verwendet.

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
backup.json        { "formatVersion": 2, "appVersion": "…", "exportedAt": "…", "categories": [ … ], "recipes": [ … ] }
images/<datei>.jpg  Rezeptfotos, referenziert über "photo": "images/<datei>.jpg"
```

- **Versionierung:** `formatVersion` wird bei inkompatiblen Änderungen erhöht. `BackupMigrations`
  hebt ältere Backups schrittweise auf das aktuelle Format. Backups aus *neueren* App-Versionen
  werden mit einer klaren Meldung abgelehnt. Neue Felder bekommen immer Default-Werte, unbekannte Felder
  werden ignoriert.
- **Format 2** ergänzt `categories` (uuid, name, icon, color) sowie pro Rezept `categories` (UUIDs) und
  `lastCookedAt`. Sicherungen im Format 1 lassen sich weiter laden (`BackupFormatTest`). Kategorien
  werden beim Import über UUID oder Namen wiedererkannt, damit nichts doppelt entsteht.
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
