<img src="docs/readme/app-icon.svg" width="48" height="48" alt="Phonon TTS App-Symbol">

# Phonon TTS für Android

**[English](README.md)** · Android 7+ · Fünf Sprachen · Community-Vorabversion

Eine Android-Systemsprachausgabe auf Basis von [Gradium Phonon](https://github.com/gradium-ai/xn-ptts). Nach dem einmaligen Modelldownload entstehen Sprache und Audio lokal auf der CPU. Auch Apps, die Androids TextToSpeech-Dienst verwenden, können die Engine nutzen. Dieser Community-Fork ist keine offizielle Gradium-App.

**[APK herunterladen](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/download/android-v0.4.1-11/Phonon-TTS-v0.4.1-android.11.apk)** · [Versionshinweise](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/tag/android-v0.4.1-11) · [Installation](#installation) · [Modelle](#modelle) · [Stimmen](#stimmen) · [Datenschutz](#datenschutz) · [Entwicklung](docs/android-development.md)

Aktuelle Vorabversion: **0.4.1-android.11** (versionCode 12). Die Veröffentlichungen sind als Prerelease markiert. Verwende die direkten Links oben oder [alle Veröffentlichungen](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases).

## Funktionen

- Sieben originale Q8-Modelle parallel installieren; je nach Modell Englisch, Deutsch, Französisch, Spanisch und Portugiesisch sprechen lassen.
- Optionale öffentliche Stimmpakete, Stimmauswahl, Hörprobe und Tempo von 50–200 %. Tonhöhenänderungen sind deaktiviert; Android-Pitch-Anfragen werden ignoriert.
- Material-3-Oberfläche mit hellem und dunklem Design in allen fünf Sprachen. Sie folgt Androids System-/App-Sprache; andere Oberflächensprachen fallen auf Englisch zurück.
- Satzpositionen ab Android 8 (API 26), sofern die Lese-App diese auswertet. Keine wortgenauen Zeitmarken und keine automatische Erkennung der Textsprache.

## Installation

1. Lade die APK oben herunter. Erforderlich sind Android 7+ (API 24) und ARM64 oder x86_64. Die ARM64-Version benötigt **dotprod und FP16 (ARMv8.2)**; 64 Bit allein reichen nicht. Die Berechnung läuft auf der CPU.
2. Öffne die APK. Erlaube bei Bedarf die Installation durch den verwendeten Browser oder Dateimanager. Diese Quellenberechtigung ist etwas anderes als eine Warnung vor einer schädlichen App: Bei einer Sicherheitswarnung abbrechen und Herkunft sowie Versionsinformationen prüfen; Play Protect nicht deaktivieren.
3. Installiere über die vorhandene Vorabversion. Modelle, Stimmen, Einstellungen und gespeicherter Token bleiben dabei erhalten. Nicht deinstallieren und keine App-Daten löschen. Die veröffentlichten Vorabversionen verwenden denselben Debug-Signaturschlüssel, auch dieser mit R8 verkleinerte Release-Build.
4. Öffne Phonon TTS und lade ein Modell herunter. **phonon-31m-en** ermöglicht Englisch ohne Konto. Für die zugangsbeschränkten Modelle gelten die Schritte im nächsten Abschnitt.
5. Wähle installiertes Modell, gesprochene Sprache und Stimme und spiele die Hörprobe ab. Zusätzliche Stimmpakete sind optional und werden separat geladen.
6. Suche in den Android-Einstellungen nach **Text-in-Sprache-Ausgabe**, wähle **Phonon TTS** als bevorzugte Engine und teste die Systemhörprobe. Bezeichnungen unterscheiden sich je nach Gerät. Die Lese-App muss Android-System-TTS unterstützen.

## Modelle

| Modell | Gesprochene Sprachen | Lizenz / Zugang |
|---|---|---|
| [phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en) | Englisch | Apache-2.0 · ohne Konto |
| [phonon-40m-en](https://huggingface.co/gradium/phonon-40m-en) | Englisch | CC-BY-NC-4.0 · Freigabe erforderlich |
| [phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de) | Deutsch | CC-BY-NC-4.0 · Freigabe erforderlich |
| [phonon-40m-fr](https://huggingface.co/gradium/phonon-40m-fr) | Französisch | CC-BY-NC-4.0 · Freigabe erforderlich |
| [phonon-40m-es](https://huggingface.co/gradium/phonon-40m-es) | Spanisch | CC-BY-NC-4.0 · Freigabe erforderlich |
| [phonon-40m-pt](https://huggingface.co/gradium/phonon-40m-pt) | Portugiesisch | CC-BY-NC-4.0 · Freigabe erforderlich |
| [phonon-40m-multilingual](https://huggingface.co/gradium/phonon-40m-multilingual) | Alle fünf | CC-BY-NC-4.0 · Freigabe erforderlich |

Beantrage für jedes zugangsbeschränkte Modell auf der verlinkten Hugging-Face-Seite persönlich den Zugang, akzeptiere die Bedingungen und warte auf die Freigabe. Erstelle einen Hugging-Face-Token mit **Leserechten** und speichere ihn ausdrücklich in der App. Eine Freigabe gilt nicht automatisch für andere Modelle. Die 40M-Modelle erlauben nichtkommerzielle Nutzung mit Namensnennung; die Code-Lizenz ersetzt diese Bedingungen nicht.

Ein Download umfasst ungefähr 65 MB (31M) oder 75 MB (40M) und enthält eine Standardstimme. Die APK enthält keine Modellgewichte. Die Auswahl setzt das Modell als Standard für **alle von ihm unterstützten Sprachen**. Das mehrsprachige Modell ändert somit alle fünf Sprachstandards; andere Sprachstandards bleiben bei einsprachigen Modellen erhalten. Die Auswahl übersetzt keinen Text.

Ein Modell lässt sich über sein Löschsymbol nach Bestätigung des angezeigten Eintrags entfernen. Andere Modelle, gemeinsame Stimmpakete und Token bleiben erhalten. Lokale Checkpoint-Ordner lassen sich ebenfalls importieren; ihre deklarierten Sprachen bestimmen die Verfügbarkeit.

## Stimmen

Die optionalen [öffentlichen Gradium-Stimmen](https://huggingface.co/gradium/phonon-voices) stehen unter Apache-2.0 und benötigen keinen Token: **37 englische, 13 deutsche, 15 französische, 25 spanische und 12 portugiesische — insgesamt 102**.

Lade über die Aktion neben der ausgewählten Stimme einmalig das Paket ihrer Sprache herunter. Passende Modelle verwenden das gemeinsame Paket; bei der Aktivierung können zusätzlich modelllokale Kopien entstehen. Die APK enthält nur Namen, Prüfsummen und Lizenzhinweise, **keine Stimmeinbettungen**, auch keine deutschen. Die Standardstimme kommt mit dem Modell. Bestehende deutsche Stimmdateien und Einstellungen bleiben bei Updates erhalten.

## Datenschutz

Text und erzeugtes Audio werden auf dem Gerät verarbeitet. Nur Modell- und Stimmdownloads verbinden sich mit Hugging Face und erlaubten CDNs; nach der Installation funktioniert die Sprachsynthese offline. Die App enthält keinen zusätzlichen Telemetrie-Code.

Der gespeicherte Hugging-Face-Token wird maskiert angezeigt und mit AndroidKeyStore AES/GCM verschlüsselt atomar in `noBackupFilesDir` abgelegt. Er geht nur an Hugging Face, nicht an weitergeleitete CDN-Adressen. Ein leeres Eingabefeld löscht ihn nicht; dafür gibt es eine eigene Löschaktion. Keine Tokens oder privaten Lesetexte in Fehlerberichte aufnehmen.

## Hilfe bei Problemen

| Problem | Prüfen |
|---|---|
| Eine Stimme fehlt | Das Paket der gesprochenen Sprache herunterladen. Nur verfügbare Stimmen werden angeboten. |
| Falsches Sprachmodell | Gewünschtes Modell erneut auswählen. Die mehrsprachige Auswahl ändert alle fünf Standards. Auch Sprache und explizite Stimme der Lese-App prüfen. |
| Modell lädt langsam | Laden und Wechsel benötigen CPU-Zeit und genug RAM für ein Modell. Es gibt keine Geschwindigkeitsgarantie für bestimmte Telefone. |
| Update lässt sich nicht installieren | Herkunft und kompatible Signatur prüfen. Anderswo erstellte Builds können einen anderen Schlüssel verwenden. Vorhandene App-Daten während der Klärung behalten. |
| Keine Lesemarkierung | Die Lese-App muss Satzbereich-Rückmeldungen ab API 26 auswerten. Wortgenaue Markierungen sind nicht verfügbar. |

Kindle-Unterstützung hängt vom Buch, der App, dem Gerät und der verfügbaren Assistive-Reader-Funktion ab und ist nicht garantiert. Die App umgeht keinen DRM-Schutz.

## Entwicklung und Projektumfang

[Build-Anleitung und Gerätetests](docs/android-development.md), [Beiträge](CONTRIBUTING.md), [Sicherheitsmeldungen](SECURITY.md) und [letzte dokumentierte Validierung](android/tts-engine/UPDATE11_VALIDATION.md) sind auf Englisch verfügbar. Host-Prüfungen bestätigen kein Android-Geräteverhalten: Native Lebenszyklen, Löschdialoge und Reader-Integration müssen noch auf Geräten bestätigt werden.

- `android/tts-engine/` — App, Compose-Einstellungen, System-TTS-Dienst und Host-Tests.
- `android/PhononTTS.kt` — gemeinsamer nativer Kotlin-Wrapper; `android/build.sh` baut die CPU-Bibliotheken.
- `ptts/` und `ptts-ffi/` — ursprüngliche Rust-Laufzeit und C-Schnittstelle.
- [Historische Upstream-README](README.upstream.md) — CLI, Python, Wasm und weitere Laufzeitintegrationen, getrennt von dieser APK.

## Herkunft und Lizenzen

Phonon und die Modelle stammen von **Gradium**. Dieses Repository pflegt einen Android-Community-Fork von [gradium-ai/xn-ptts](https://github.com/gradium-ai/xn-ptts). Code: [MIT](LICENSE-MIT) **oder** [Apache-2.0](LICENSE-APACHE). Für Modelle gelten die oben aufgeführten separaten Bedingungen; öffentliche Stimmpakete stehen unter Apache-2.0. Weitere Angaben: [gebündelte Hinweise](android/tts-engine/src/main/assets/licenses/NOTICE.txt), [Stimmenzuordnung](android/tts-engine/src/main/assets/licenses/Voices-NOTICE.txt) und [Sonic-Lizenz](android/tts-engine/src/main/assets/licenses/Sonic-LICENSE.txt).
