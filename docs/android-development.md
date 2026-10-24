# Android development

The [English app guide](../README.md) and [German app guide](../README.de.md) cover installation and use. This guide concerns the community CPU system-TTS app, not the upstream QNN integration.

## Toolchain

The checked-in configuration uses **JDK 17**, **Gradle 9.3.1** (wrapper), **Android Gradle Plugin 9.1.1**, Kotlin/Compose plugin **2.2.20**, compile SDK **37**, target SDK **36** and minimum API **24**. Packaging calls SDK build-tools **36.0.0** for zipalign. Set `ANDROID_HOME` to your SDK directory and install those SDK components. No CI run is required; packaging is manual.

Native builds need Rust, cargo-ndk and an Android NDK. `android/build.sh` uses the NDK found through `ANDROID_NDK_HOME`/cargo-ndk; it does not pin an NDK version. The local SDK currently provides NDK **27.2.12479018**. The script explicitly sets 16 KiB ELF page alignment and ARM64 dotprod/FP16 features. The published CPU build therefore requires ARMv8.2 features. x86_64 is packaged too; this is not evidence of an emulator test.

## Build and package

Run from the repository root. Replace paths with your SDK, NDK and writable scratch directory. Keep temporary files outside the checkout.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
export TMPDIR=/path/to/scratch
mkdir -p "$TMPDIR"
cargo install cargo-ndk
./android/build.sh
./android/tts-engine/package-preview.sh
```

`android/build.sh` rebuilds `ptts-ffi` for arm64-v8a and x86_64 and replaces `android/jniLibs/`. These generated libraries are not tracked in Git; a fresh checkout needs real native builds, not placeholder libraries. Existing validated libraries can be reused for app-only work.

`package-preview.sh` validates resources, builds the minified release, runs selected JVM tests, checks packaged native architectures/JNA, voice metadata and notices, and checks APK alignment. Its current outputs are **`Phonon-TTS-v1.0.0.apk` and its `.sha256` sidecar in the repository’s parent directory**. The script contains explicit versioned output names; changing documentation does not change the app version. The historical manual script name is retained for stable releases. When the .11 APK exists there, it also verifies update compatibility. Nothing is uploaded by the script.

The release build currently uses Gradle’s **debug signing configuration**. Stable 1.0.0 retains the established local Android Debug certificate used by previews, by user choice. A minified, nondebuggable release build and a stable GitHub tag do not change the signer certificate name. The private key and its local backups must never enter Git. A build elsewhere normally receives a different certificate and cannot update an installed published preview in place. Preserve the original installation and data when investigating signature mismatches. Do not commit or distribute private signing keys, credentials or local SDK configuration.

R8 rules in [proguard-rules.pro](../android/tts-engine/proguard-rules.pro) preserve JNA reflection. Do not remove them when changing packaging.

## Focused checks

For resource validation and the same selected host tests without packaging:

```sh
python3 android/tts-engine/validate-resources.py
./android/tts-engine/gradlew -p android/tts-engine testDebugUnitTest \
  --tests ai.gradium.phonon.tts.FocusedTest \
  --tests ai.gradium.phonon.tts.ProgressTest \
  --tests ai.gradium.phonon.tts.SettingsStateTest \
  --tests ai.gradium.phonon.tts.ModelDeletionTest \
  --tests ai.gradium.phonon.tts.InventoryTest \
  --tests ai.gradium.phonon.tts.SelectionPolicyTest \
  --tests ai.gradium.phonon.tts.VoiceCatalogPolicyTest \
  --tests ai.gradium.phonon.tts.VoicePackStoreTest --console=plain
```

With `TMPDIR` exported as above, test temporary files use that scratch directory. The optional public-voice network test is enabled explicitly:

```sh
PHONON_LIVE_VOICES=1 ./android/tts-engine/gradlew -p android/tts-engine \
  testDebugUnitTest --tests ai.gradium.phonon.tts.VoiceCatalogPolicyTest --rerun-tasks --console=plain
```

This makes public network requests; no gated-model token is needed. See [1.0.0 validation](../android/tts-engine/RELEASE_1.0.0_VALIDATION.md) and the historical [Update 11 validation](../android/tts-engine/UPDATE11_VALIDATION.md) for recorded evidence and limitations. Host tests and downloaded voice validation do not establish Android runtime behavior.

## Device smoke checks

Use harmless sample text and record the device, CPU, Android version, app version, model and voice. Check:

- Update over the preceding preview with the same certificate; confirm retained models, saved token and voice preferences.
- Download the public English model, select the system TTS engine and play app/system samples offline after installation.
- Download an optional voice pack, change language/model and confirm routing. Selecting multilingual should update all five defaults.
- Test cancellation, model switch during speech, deletion confirmation/cancellation and remaining model/voice availability. Native handle lifecycle and deletion UI need device confirmation.
- Check light/dark UI, all five locales, accessibility and tempo. Pitch requests should have no effect.
- In a compatible reader, test API 26+ sentence-position callbacks; do not interpret them as word alignment or promise Kindle compatibility.

## Source map

Paths below are relative to `android/tts-engine/src/main/java/ai/gradium/phonon/tts/`:

| Area | Files |
|---|---|
| Android TTS service and audio | `PhononService.kt`, `SentenceSpeech.kt`, `SpeechAudio.kt` |
| Compose settings | `SettingsScreen.kt`, `SettingsActivity.kt` |
| Model catalog and lifecycle | `ModelCatalog.kt`, `Inventory.kt`, `CheckpointSwap.kt`, `SelectionPolicy.kt` |
| Optional public voices | `VoicePacks.kt`, `VoicePackStore.kt`, `VoiceCatalogPolicy.kt` |
| Credentials and network policy | `TokenStore.kt`, `SecureDownload.kt` |

The shared native wrapper is [android/PhononTTS.kt](../android/PhononTTS.kt); CPU library integration is described in [android/README.md](../android/README.md). The latter documents the upstream embedding API, separate from the installable app.
