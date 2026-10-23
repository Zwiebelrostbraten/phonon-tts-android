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
