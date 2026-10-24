# Stable 1.0.0 validation

Prepared 2026-10-24 from clean ec26d05 (last app behavior commit 94fad6d), promoting 0.4.1-android.11 / code 12 to **1.0.0 / code 13**. Package `ai.gradium.phonon.tts`; runtime Cargo **0.4.1 unchanged**. Community fork, not an official Gradium stable release.

## Scope and handset evidence

Product changes are only Gradle version fields and the About version in EN/DE/FR/ES/PT. All Kotlin/Java/Rust runtime code, model/voice assets, storage, token, audio, dependencies, permissions, SDK levels and tests remain unchanged. The signer configuration is unchanged; only its misleading comment was clarified. Packaging/verification scripts and current documentation were updated. No native build, live voice download sweep, CI, emulator or phone actions were performed.

The user confirmed all requested handset checks on **.11**, saying “Ja ist getestet”: model/pack downloads, offline playback, model/language switching, deletion preserving other models and restart persistence. This is user-reported evidence on the preceding app behavior, not an agent handset test of 1.0.0. Device model and Android version were not supplied. No Android device was attached to the build environment; no agent-run handset validation was performed. These checks do not establish blanket platform compatibility.

## Build and host checks

`package-preview.sh` (historical manual filename retained) ran actual `assembleRelease`, R8 and lintVital successfully, then signature/update and 16 KiB zipalign checks. Its test task initially reused valid results; one fresh execution of the same eight selected classes with task option `--rerun` then succeeded. No expanded test loop. Exact XML counts:

```
{'tests': 47, 'failures': 0, 'errors': 0, 'skipped': 1}
Passed: 46
```

Resource checks retain 74 keys × five locales, seven pinned catalogs and 102 voice metadata entries, with no embedded model/voice safetensors. aapt2 resource output confirms compiled 1.0.0 About text in all five languages. aapt2 badging and manifest xmltree show no debuggable flag; release uses R8 minification. Permissions, declared features and min/target SDK match .11. All packaged asset names/bytes (including both voice manifests) and every .so match .11 exactly. Source audit confirms runtime code unchanged; compiled DEX need not be byte-identical after version/resource changes and R8.

Exact package output tail:

```
BUILD SUCCESSFUL in 1m 27s
74 actionable tasks: 31 executed, 43 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.3.1/userguide/configuration_cache_enabling.html
APK bytes: 18055385
/opt/data/projects/Phonon-TTS-v1.0.0.apk
bdfe618dc63d893684d0671eb5627f96af728aa79f2aa8c1f10bd13506ad5d95  Phonon-TTS-v1.0.0.apk
PASS: application ID, explicit stable version and increasing code, certificate, permissions, SDK levels; all native libraries and all packaged assets byte-identical; zero embedding binaries
Certificate SHA-256: 63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0
Size: 18055385
APK SHA-256: bdfe618dc63d893684d0671eb5627f96af728aa79f2aa8c1f10bd13506ad5d95
```

Exact native byte comparison hashes:

```
41e9a793c43a0f4fddb19e33f346bace464f30f888ba7b9eaf96294ea115bfb6 lib/arm64-v8a/libandroidx.graphics.path.so
abc26e994517bcaa3309acdb0a27373864086c7569c89d3087b8626fada9ef06 lib/arm64-v8a/libjnidispatch.so
cfce164dce61038367f05f2a67b25d91f741e5c5838dc1bd572f6ad53a6f2c2b lib/arm64-v8a/libptts_ffi.so
4e56c996f13670e70082658de7880c4020eabf4f25e43387f88ed78a713fc9f0 lib/x86_64/libandroidx.graphics.path.so
3809247e9b804a05ed8377b87d545cf5b5e97f960f75e725bc8497b40260e417 lib/x86_64/libjnidispatch.so
e550c761ee6d2e4ffc20e778d099b6e0f7a23a62c0e0e33859c2c170db6b7ea1 lib/x86_64/libptts_ffi.so
PASS: every packaged .so byte-identical to .11; all assets compared by verify-update.py
```

## Signing and handoff

The established **Android Debug** certificate is retained by user choice, SHA-256 `63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0`. A nondebuggable release build, semantic version and stable GitHub tag do not rename its certificate CN. No key was generated, switched, exposed or copied into Git. A protected local backup of the established key was verified separately, outside Git. In-place update metadata is compatible; no uninstall or data clearing is needed.

Artifact: `/opt/data/projects/Phonon-TTS-v1.0.0.apk` and `.apk.sha256` sidecar. Parent will commit/publish stable tag `v1.0.0` with `--latest` and without prerelease. No commit, push, tag or GitHub write was performed here. Historical APKs/tags and validation records remain intact. No local packaging blockers.

Logs and tool dumps are under `/opt/data/profiles/mrs_patmore/cache/scratch/`: `phonon-stable-package.log`, `phonon-stable-tests.log`, `phonon-stable-test-counts.log`, `phonon-stable-source-audit.log`, `phonon-stable-inspect.log`, `phonon-stable-manifest.log`, `phonon-stable-resources.log`, `phonon-stable-zipalign.log`, `phonon-stable-compatibility.log`, `phonon-stable-native-hashes.log`. zipalign verbose output ends `Verification successful`.

Changed tracked files (plus this new validation record):

```
README.de.md
README.md
android/tts-engine/CHANGELOG.md
android/tts-engine/README.md
android/tts-engine/RELEASE_NOTES.md
android/tts-engine/build.gradle.kts
android/tts-engine/package-preview.sh
android/tts-engine/src/main/res/values-de/strings.xml
android/tts-engine/src/main/res/values-es/strings.xml
android/tts-engine/src/main/res/values-fr/strings.xml
android/tts-engine/src/main/res/values-pt/strings.xml
android/tts-engine/src/main/res/values/strings.xml
android/tts-engine/validate-resources.py
android/tts-engine/verify-update.py
docs/android-development.md
```
