# Update .9 validation — 2026-10-24

Narrow installed-model selection follow-up. Full-width, consistently shaped installed rows use 12dp padding and an explicit 12dp gap, with a 24dp radio aligned to the first title line, weighted wrapping text and a single selectable radio item (minimum 48dp touch height). Selected colors follow Material 3. The rest of settings is unchanged apart from removing the obsolete preview default button and its action/resource in all five locales.

Explicit installed-model selection validates identity using the production SelectionPolicy helper, then saves selected identity and all supported-language defaults in one SharedPreferences edit before loading. Other language defaults are preserved; unknown IDs return before writes or loading. Rendering and refresh do not select models. Routing, aliases, system locale handling, storage paths, permissions and TokenStore are unchanged.

## Artifact

- Version: `0.4.1-android.9`, versionCode `10`; existing R8-minified, Android Debug-signed release configuration.
- APK: `/opt/data/projects/Phonon-TTS-v0.4.1-android.9.apk`
- Size: 21,118,836 bytes.
- SHA-256: `6032d4c70373206dea14827c37254fd986f348b3b5f3b97fdc12c7b5a53ef2e4`
- Sidecar: `/opt/data/projects/Phonon-TTS-v0.4.1-android.9.apk.sha256`
- Certificate SHA-256: `63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0`.

## Targeted checks

One RED → GREEN cycle: the new SelectionPolicyTest first ran against an unchanged-state helper stub: 3 tests, 2 expected assertion failures (English and multilingual defaults), unknown-ID no-op passed. After implementation, actual `package-preview.sh` passed `assembleRelease` and the selected JVM classes: FocusedTest 9, ProgressTest 5, SettingsStateTest 5, InventoryTest 7, SelectionPolicyTest 3 = 29 tests, zero failures/errors/skips. The three new checks cover English preserving German, multilingual setting all five defaults, and an unknown installed identity preserving the exact preference snapshot.

`validate-resources.py`: PASS, 66 keys × 5 complete locale sets (67 minus obsolete prefer_language), format parity, localeConfig and all 7 catalogs with exact pinned revisions, byte counts and hashes. Android resource and Compose compilation passed. `verify-update.py`: PASS against preserved `.8` APK: application ID, version 10/.9, signing certificate, permissions, SDK levels, packaged locales, all native libraries and all 13 German embeddings byte-identical. `git diff --check`: PASS.

Logs: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon-ui9/red.log` and `package.log`.

No device/emulator installation or visual testing, broad suite, review, CI, commit, push or publishing performed. Prior APKs preserved. Native runtime behavior and accessibility/layout on a device remain unverified; the host checks compile the layout and exercise the production selection policy.
