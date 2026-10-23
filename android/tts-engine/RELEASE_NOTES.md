# 0.4.1-android.5 (versionCode 6)

Ruhige Settings mit sichtbarer aktueller Modellkopie, Sprachen, lokaler Größe und
Lade-/Fehlerstatus. Stimme und Tempo-Vorschau erscheinen bei bereitem Modell.
Modellauswahl erst auf Wunsch; separater lokaler SAF-Import. Download und Import
bestätigen bei vorhandenem Modell ausdrücklich den Ersatz. Abbrechen startet keine
Installation. Es bleibt jeweils genau ein Modell installiert; Rollback bleibt erhalten.
Systemabhängiges helles/dunkles Theme, scrollbare gruppierte Flächen, große Bedienelemente,
Systemleisten-Abstände und deutsche/englische Beschriftungen. Keine neue UI-Abhängigkeit.
HF-Bedingungen und maskierte, verschlüsselte Tokenverwaltung nur bei gated Auswahl.

Normales Update mit derselben Signatur erhält filesDir/model, Stimmen, Einstellungen
und gespeicherten Token. Nicht deinstallieren, keine App-Daten löschen. Paket-ID,
Signierung, native Bibliotheken, 13 Stimmen und Berechtigungen bleiben unverändert.
Pitch bleibt deaktiviert. Kein Gerät/Emulator angeschlossen: visuelle und Android-
Laufzeitprüfung einschließlich Update-Persistenz bleiben offen.

# 0.4.1-android.4 (versionCode 5)

Die Engine meldet jetzt satzweise Textpositionen über Android rangeStart (API 26+).
Originale UTF-16-Offsets und tatsächlich ausgegebene PCM-Frames nach Tempoanpassung
bestimmen die Marker; keine geschätzten Wortzeiten und kein Timer. Sätze werden
nacheinander synthetisiert, wodurch sich Pausen und Prosodie ändern können.
Wortgenaues Alignment steht nicht zur Verfügung. API 24–25 spielt ohne Positionsmarker.
Ob Kindle die Positionen anzeigt, muss auf dem Handy geprüft werden; keine bestätigte
Kindle-Behebung. Tonhöhenänderungen bleiben deaktiviert.

Update mit derselben Signatur erhält Modelle, Stimmen, Standardstimmen und Token.
Paketname, native Bibliotheken und Berechtigungen bleiben unverändert.

# 0.4.1-android.3 (versionCode 4)

13 echte deutsche Gradium-Stimmen sind jetzt im APK enthalten: Nele, Jette,
Annika, Jule, Lotta, Josefine, Svea, Anton, Mirko, Mats, Henrik, Leon und Mika.
Ein bereits installiertes deutsches oder mehrsprachiges Modell bekommt sie beim
Laden ohne erneuten Modelldownload und ohne Token. Englisch 31m bleibt englisch.
Die Auswahl ist sofort nach dem Laden verfügbar und wird je Sprache als Standard
gespeichert; eine ausdrücklich vom Android-Client angeforderte Stimme hat Vorrang.
Tempo bleibt einstellbar, Tonhöhenänderungen bleiben deaktiviert.

Der HF-Token wird mit „Token speichern“ oder beim Start eines gated Downloads
verschlüsselt gespeichert und automatisch wiederverwendet. Das Passwortfeld bleibt
leer; der Speicherstatus und „Gespeicherten Token löschen“ dienen zur Verwaltung.
AndroidKeyStore AES/GCM schützt die Datei im nicht gesicherten App-Datenbereich
(noBackupFilesDir). Kein Klartext in Einstellungen, Logs, Intents oder APK.
Bei Schlüsselproblemen erneut eingeben; falls nötig explizit den gespeicherten
Token löschen und neu speichern. Fehler löschen nicht stillschweigend die Datei.

Normale Updates mit derselben Signatur erhalten Modelle, Einstellungen und den
ab dieser Version gespeicherten Token. Ein früher nie gespeicherter Token muss
noch einmal eingegeben werden. Deinstallation oder Löschen der App-Daten entfernt
auch den Token. Zum Aktualisieren nicht deinstallieren und keine App-Daten löschen.

Paketname und Signatur bleiben gleich. Keine zusätzlichen Berechtigungen oder
externen Dienste. Die gemeldete Warnung auf dem Handy ist ohne genauen Screenshot
nicht diagnostiziert; dieser Build behauptet keine Behebung oder Malwarefreiheit.
