# Update .8 validation — 2026-10-24

Implemented locally on main based on fab8c1b. No commit, push, publishing, GitHub writes, native rebuild, emulator or review agents. The .1–.7 artifacts remain available.

## Artifact

- `0.4.1-android.8`, versionCode 9, R8-minified release signed with the established Android Debug key.
- APK: `/opt/data/projects/Phonon-TTS-v0.4.1-android.8.apk`
- Size: 21,119,320 bytes.
- SHA-256: `0b0362a8ae32eeb0c3b31a3ad195d83f84956a91a414b55a2270a42e68ab65c6`
- Checksum sidecar: `/opt/data/projects/Phonon-TTS-v0.4.1-android.8.apk.sha256`
- Certificate SHA-256: `63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0` (identical to .7).
- APK inspection verified app ID `ai.gradium.phonon.tts`, minSdk 24, targetSdk 36, unchanged three permissions, both ABIs, JNA, licenses, all native libraries and all 13 German embeddings byte-identical to .7. Packaged resource locales are exactly default English, de, es, fr, pt.
- R8 mapping: `android/tts-engine/build/outputs/mapping/release/mapping.txt` (29,386,687 bytes).

## Focused checks

Command: `TMPDIR=/opt/data/profiles/mrs_patmore/cache/scratch android/tts-engine/package-preview.sh`

The script runs `assembleRelease testDebugUnitTest` filtered to the following classes (no full suite; assembleRelease invoked its standard lintVital tasks despite the requested no-lint constraint, an execution deviation):

| Class | Tests | Failures/errors/skipped |
|---|---:|---|
| FocusedTest | 9 | 0/0/0 |
| InventoryTest | 7 | 0/0/0 |
| ProgressTest | 5 | 0/0/0 |
| SettingsStateTest | 5 | 0/0/0 |

26 focused JVM tests passed (the existing 25 plus one targeted activation recovery regression). Coverage includes token encryption/download policy, tempo/pitch and sentence markers, replacement confirmation, inventory legacy backup recovery with sentinel weights, multiple independent directories, safe IDs, cancellation before/during commit, isolated rollback, language union/default routing, regional system locales, explicit voice identities and invalid aliases, and seven pinned catalogs. These tests exercise production helpers; transaction callbacks test filesystem failure handling and do not pretend to perform native loading.

`validate-resources.py`: PASS, 67 string keys × 5 complete locale sets, format placeholder parity, XML parsing, localeConfig and all seven catalog revisions/files/exact byte counts/hash identities against the saved authoritative API metadata fixture. Android resource compilation passed. `verify-update.py`: PASS for the actual final APK. `git diff --check`: PASS. `adb devices -l`: no devices.

## Pre-delivery fixes

Failed installation captures the active entry and stored selection before closing the previous handle. Under the same lifecycle lock, the failure path closes the candidate, recovers the target, restores metadata/selection, and reactivates the previous entry only if its checkpoint is complete and has no pending backup. Same-ID replacement loads the restored old files. Recovery failures are suppressed on the original install error; failed file recovery prevents reactivation. The added pure transaction/lifecycle regression covers same-ID and different-ID failures, one-handle ordering, restored sentinel files, original exception identity, and skipping activation when recovery fails. Native activation itself remains untested on host.

Known supported languages without matching installed data now return LANG_MISSING_DATA even when another language is installed; unknown languages return LANG_NOT_SUPPORTED. VoiceSelect translates the default speaker label in all five resource sets and displays language names using the current resource locale. Stored speaker names and Android voice identities are unchanged. These service/UI changes compiled in both debug and minified release builds; runtime Android behavior is still untested.

## RED → GREEN evidence

- Initial additive API/resource compilation RED: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon8-red.log`; initial focused GREEN: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon8-green.log`.
- Behavioral regression RED: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon8-cancel-red.log`, `InventoryTest.cancellationDuringCommitRestoresPreviousDirectory` failed with AssertionError because cancellation arriving during activation could commit. Production `Inventory.commit` now checks cancellation again inside the reversible swap, and Model closes a newly activated handle before throwing on late cancellation. This test and all 25 others are GREEN in the final packaging run.
- Final build, focused tests, resource checks, artifact/hash and actual APK compatibility validation: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon8-predelivery-final.log`.
- Detailed JUnit results: `android/tts-engine/build/test-results/testDebugUnitTest/TEST-*.xml`.

The earlier packaging verification encountered host tooling issues (apksigner could not find java on PATH, then the badging parser expected sdkVersion rather than minSdkVersion). The verification script was corrected and the final command above completed successfully; these were verification-tool errors, not successful compatibility checks at the time.

## Scope and remaining device validation

All seven original CPU checkpoint models and all five portfolio languages are selectable. Each uses pinned Q8 plus config/tokenizer/default voice; gated small files use Git blob SHA-1 and LFS files SHA-256. The alternative llamacpp/audiocpp conversions and codec-only mimi model are intentionally excluded as incompatible speech-model options for this FFI. No gated weights were fetched and no credentials were used.

Legacy model/backup recovery stays in place, arbitrary legacy imports retain their directory, new imports receive UUID IDs, and all installs have isolated transactions. An uncommitted legacy staging directory is retained without silently promoting it. For imports, config languages determine availability; absent language metadata conservatively defaults to English rather than advertising unsupported French/German/etc. Other imported languages should be declared in config.json. No optional model-removal UI was added.

One native handle is loaded at a time under the same lock used for synthesis and close. Android voice enumeration is metadata-only. Explicit model-qualified voices win, with known old aliases resolved against language defaults. UI follows Android system/app resource language, independently from speech; arbitrary text-language inference is not provided.

Android runtime speech, actual on-device update/restart persistence, visual/accessibility behavior, native model switching and authenticated gated downloads remain untested because no device is connected. Host filesystem tests and unchanged native binaries do not establish those behaviors. CPU ARM64 instruction requirements and reader-dependent Kindle integration remain unchanged.
