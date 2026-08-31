# Infra

Android-App, die per Infrarot-Blaster des Handys systematisch alle
technisch möglichen NEC-Infrarotcodes durchprobiert (Brute-Force-Scan).
Gedacht, um per Ausprobieren herauszufinden, welcher Code ein bestimmtes
Infrarot-Gerät (Fernseher, Klimaanlage, …) steuert.

## Features

### Code-Iteration

- Iteriert alle 65536 möglichen NEC-Codes (Adresse 0–255 × Befehl 0–255,
  jeweils 8 Bit) einmal durch, jeder Code wird genau als ein NEC-Frame
  gesendet (Standard-NEC-Protokoll, 38 kHz Trägerfrequenz, Header +
  32 Datenbits inkl. invertierter Prüfbytes + Abschluss-Mark)
- Sendeintervall: 150 ms pro Code
- Aktueller Stand (Index, Adresse, Befehl) wird groß angezeigt, dazu ein
  Fortschrittsbalken mit Positionsangabe „x / 65535"

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
  invertierte Prüfbytes, Adress-/Befehls-Aufteilung)
- Unit-Tests für die Scan-Logik (Play/Pause, Startwert, Schrittweiten,
  Grenzen, Abschluss, Verhalten ohne IR-Sender)

### Technisches

- Plattform: Android, `minSdk` 26, `targetSdk` 35
- Kotlin, Jetpack Compose Material 3, Hilt, DataStore Preferences,
  Navigation Compose
- Kostenlos, keine Werbung, keine In-App-Käufe, vollständig offline
