# Infra – context.md

Aktueller Stand (keine Historie). Bei Widersprüchen hat `FEATURES.md`
Vorrang.

## Tech-Stack

- Kotlin, AGP 8.13.2, JVM 17, `compileSdk`/`targetSdk` 35, `minSdk` 26
- Jetpack Compose Material 3, Single-Activity, Compose Navigation
- Hilt (`InfraApp`, DI-Module in `di/`)
- DataStore Preferences (Theme-Mode + Scan-Fortschritt), kein Room –
  die App hat keine relationalen Daten
- Schrift Geist (`res/font/`), WaffleHQ-Farbtokens in `ui/theme/`

## Paket

`com.wafflehq.infra`

## Dateistruktur

```
app/src/main/java/com/wafflehq/infra/
  InfraApp.kt, MainActivity.kt
  ir/NecCodec.kt              – reine NEC-Frame-Erzeugung (Header, 32 Bit, Trailing-Mark)
  ir/IrTransmitter.kt         – Interface (hasEmitter, transmit)
  ir/ConsumerIrTransmitter.kt – Android-Implementierung über ConsumerIrManager
  di/IrModule.kt              – bindet IrTransmitter -> ConsumerIrTransmitter
  data/settings/              – SettingsRepository, ThemeMode (DataStore "settings")
  data/scan/ScanStateRepository.kt – persistiert aktuellen Scan-Index (DataStore "scan_state")
  ui/theme/                   – Color.kt, Theme.kt, Type.kt, AppTokens.kt, AppShapes.kt
  ui/navigation/AppNavHost.kt – Routen home/settings
  ui/home/HomeScreen.kt       – Scan-UI (Anzeige, Play/Pause, Startwert, Schrittweiten)
  ui/home/HomeViewModel.kt    – Scan-Logik (Loop, Persistenz, Clamping)
  ui/settings/                – SettingsScreen/-ViewModel (Theme-Auswahl)
app/src/test/java/com/wafflehq/infra/
  ir/NecCodecTest.kt
  ui/home/HomeViewModelTest.kt
```

## Implementierungsstatus

| Feature | Status | Dateien |
|---|---|---|
| NEC-Code-Iteration (65536 Codes) + Senden | fertig | `ir/NecCodec.kt`, `ir/IrTransmitter.kt`, `ir/ConsumerIrTransmitter.kt` |
| Play/Pause + Persistenz | fertig | `ui/home/HomeViewModel.kt`, `data/scan/ScanStateRepository.kt` |
| Startwert-Eingabe | fertig | `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt` |
| Schrittweiten ±1000…±1 | fertig | `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt` |
| Hardware-Prüfung / kein IR-Sender | fertig | `ir/ConsumerIrTransmitter.kt`, `ui/home/HomeScreen.kt` |
| Abschluss-Banner | fertig | `ui/home/HomeScreen.kt` |
| Theme Hell/Dunkel/System | fertig | `ui/settings/*`, `data/settings/*` |
| Lokalisierung DE/EN | fertig | `res/values/strings.xml`, `res/values-de/strings.xml` |
| Launcher-Icon | ausstehend – Platzhalter aus dem Basisprojekt, noch zu ersetzen | `res/mipmap-anydpi-v26/*`, `res/drawable/ic_launcher_*.xml` |
| Tests NecCodec + HomeViewModel | fertig | `app/src/test/java/com/wafflehq/infra/**` |

## Hinweise

- `verify-theme.sh` + `theme-hashes.sha256` prüfen, dass Farb-Tokens nur in
  `ui/theme/Color.kt`/`Theme.kt` vorkommen (kein Hex-Wert sonst im Code).
- Sendeintervall 150 ms/Code ist fest codiert (`TRANSMIT_INTERVAL_MS` in
  `HomeViewModel.kt`), nicht einstellbar.
- Scan-Fortschritt wird beim Laufen nur alle 10 Codes persistiert
  (Performance), beim Pausieren/Abschluss aber immer sofort.
