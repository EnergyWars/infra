# Infra

Android-App, die per Infrarot-Blaster des Handys systematisch alle
technisch möglichen NEC-Infrarotcodes durchprobiert (Brute-Force-Scan).
Gedacht, um per Ausprobieren herauszufinden, welcher Code ein bestimmtes
Infrarot-Gerät (Fernseher, Klimaanlage, …) steuert.

## Features

### Code-Iteration

- Iteriert alle 65536 möglichen NEC-Codes (Adresse 0–255 × Befehl 0–255,
  jeweils 8 Bit) einmal durch, jeder Code wird als ein NEC-Frame gesendet
  (Standard-NEC-Protokoll, 38 kHz Trägerfrequenz, Header +
  32 Datenbits inkl. invertierter Prüfbytes + Abschluss-Mark)
- Wie eine echte Fernbedienung bei kurzem Tastendruck folgen auf den Frame
  zwei NEC-Repeat-Codes (9 ms Mark, 2,25 ms Space, 560 µs Mark) im
  108-ms-Raster; eine Sendung dauert dadurch rund 228 ms
- Bitreihenfolge: Jedes Byte wird MSB-zuerst gesendet, sodass der
  angezeigte 8-stellige Hex-Wert exakt der Bitfolge auf der Leitung
  entspricht (gleiche Konvention wie LIRC, IRremote und gängige
  Fernbedienungs-Apps, z. B. `20DF10EF` für LG-Power)
- Sendeintervall: 150 ms pro Code
- Aktueller Stand (Index, Adresse, Befehl) wird groß angezeigt, dazu ein
  Fortschrittsbalken mit Positionsangabe „x / 65535"

### Extended NEC

- Switch schaltet zwischen Standard-NEC (65536 Codes) und Extended NEC
  (16.777.216 Codes) um
- Im Extended-Modus ist die Adresse 16 Bit breit und frei wählbar (keine
  Prüfziffer/Inversion nötig); die letzten beiden Bytes (Befehl +
  invertierter Befehl) bleiben wie im Standard-Modus
- Hex-Eingabe bleibt in beiden Modi 8-stellig; im Extended-Modus legen die
  ersten 4 Hex-Stellen die volle 16-Bit-Adresse direkt fest (mehr Freiheit
  bei der Eingabe als im Standard-Modus)
- Wird im Standardmodus ein Hex-Wert übernommen, dessen zweites Byte nicht
  die Inversion des ersten ist (z. B. `020250AF`), schaltet die App
  automatisch in den Extended-Modus, statt den Code stillschweigend
  umzuschreiben
- Jeder Modus merkt sich seine eigene zuletzt erreichte Position getrennt
  (auch über App-Neustarts hinweg) – ein Wechsel zurück springt nicht auf 0
- Umschalten nur möglich, während der Scan pausiert ist

### Play / Pause

- Start-Button beginnt beim aktuellen Index (initial 0) und sendet
  fortlaufend, bis pausiert wird oder alle Codes durch sind
- Pause-Button stoppt sofort; der zuletzt erreichte Index bleibt erhalten
- Der Fortschritt wird laufend persistiert (DataStore), sodass ein
  App-Neustart den Scan nicht auf 0 zurücksetzt

### Startwert & Sprünge

- Eingabefeld „Startwert" (nur Ziffern) plus „Übernehmen"-Button, um direkt
  zu einem beliebigen Index zu springen
- Wert wird auf den gültigen Bereich 0–65535 geclamppt
- Nur nutzbar, während der Scan pausiert ist

### Schrittweise vor/zurück

- Zwei Reihen mit Plus-/Minus-Buttons in den Schrittweiten
  1000 / 500 / 100 / 50 / 25 / 10 / 5 / 2 / 1
- Jeder Klick verschiebt den aktuellen Index um die gewählte Schrittweite,
  geclamppt auf 0–65535
- Nur nutzbar, während der Scan pausiert ist

### Hardware-Prüfung

- Beim Start wird geprüft, ob das Gerät einen Infrarot-Sender besitzt
  (`ConsumerIrManager` / `FEATURE_CONSUMER_IR`)
- Ohne IR-Sender: Hinweis-Banner, Start-Button deaktiviert; die App bleibt
  auf Geräten ohne IR-Blaster trotzdem installierbar
  (`<uses-feature android:required="false">`)

### Abschluss

- Nach dem letzten Code (65535) stoppt der Scan automatisch, ein
  Hinweis-Banner zeigt „Fertig"

### Design & Darstellung

- Material 3 Design System, WaffleHQ-Farbtokens (Sapphire/Aquamarine/
  Amethyst/Emerald/Citrine/Garnet/Graphite), Schrift Geist
- Dark Mode: automatisch nach Systemeinstellung; in den Einstellungen auch
  manuell auf Hell/Dunkel/Systemstandard umstellbar (Zahnrad-Icon auf der
  Hauptseite)
- Vollständige Lokalisierung Deutsch (Erstsprache) und Englisch (Fallback)

### Tests

- Unit-Tests für die NEC-Frame-Erzeugung (Header, Bitreihenfolge,
  invertierte Prüfbytes, Adress-/Befehls-Aufteilung) inkl. Extended-NEC
  (16-Bit-Adresse, Hex-Kodierung, Frame-Aufbau)
- Unit-Tests für die Scan-Logik (Play/Pause, Startwert, Schrittweiten,
  Grenzen, Abschluss, Verhalten ohne IR-Sender, Extended-NEC-Umschaltung
  inkl. getrennter Positions-Erinnerung je Modus)

### Technisches

- Plattform: Android, `minSdk` 26, `targetSdk` 35
- Kotlin, Jetpack Compose Material 3, Hilt, DataStore Preferences,
  Navigation Compose
- Kostenlos, keine Werbung, keine In-App-Käufe, vollständig offline
