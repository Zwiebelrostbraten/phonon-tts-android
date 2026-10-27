# Android development

The [user guide](user-guide.md) covers installation and use. This guide covers the community CPU system-TTS app.

## Toolchain

The checked-in configuration uses **JDK 17**, **Gradle 9.3.1** (wrapper), **Android Gradle Plugin 9.1.1**, Kotlin/Compose plugin **2.2.20**, compile SDK **37**, target SDK **36** and minimum API **24**. Packaging calls SDK build-tools **36.0.0** for zipalign. Set `ANDROID_HOME` to your SDK directory and install those SDK components. No CI run is required; packaging is manual.

Native builds require **Rust/Cargo 1.99.1**, **cargo-ndk 4.1.2** and **NDK 27.2.12479018**. The script verifies these exact versions, targets API 24, and sets 16 KiB ELF alignment, ARM64 dotprod/FP16 and x86-64 baseline SSE2. The default ARM64 build requires those CPU features. See [F-Droid preparation](f-droid.md) for provisioning, frozen source builds and metadata handoff.

## Build and package

Run from the repository root. Replace paths with your SDK, NDK and writable scratch directory. Keep temporary files outside the checkout.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
export TMPDIR=/path/to/scratch
mkdir -p "$TMPDIR"
rustup toolchain install 1.99.1 --profile minimal --target aarch64-linux-android,x86_64-linux-android
cargo install cargo-ndk@4.1.2 --locked
cargo fetch --locked --target aarch64-linux-android --target x86_64-linux-android
./android/build.sh --offline
./android/tts-engine/package-preview.sh
```

`android/build.sh` rebuilds `ptts-ffi` for arm64-v8a and x86_64 and replaces `android/jniLibs/`. These generated libraries are not tracked in Git; a fresh checkout needs real native builds, not placeholder libraries. Existing validated libraries can be reused for app-only work.

`package-preview.sh` validates resources, builds an optimized **unsigned** release, runs the JVM tests and checks native architectures, notices, voice metadata, absence of weights, ELF and ZIP alignment. It writes `Phonon-TTS-Community-v1.1.0-unsigned.apk` and a SHA-256 sidecar under `android/tts-engine/build/outputs/community/` (override with `ARTIFACT_DIR`). Nothing is uploaded. `verify-apk.py` checks the current unsigned release independently; `verify-update.py` is only an optional comparison of two distributor-signed APKs, with no fixed historical signer.

Release builds have no signing configuration. F-Droid signs its own compiled APK if accepted; the local unsigned APK is not installable until signed. No signing secrets are needed for building or checking it. Historical artifacts and certificates are unchanged; this new application identity deliberately has no migration or update compatibility layer.

R8 rules in [proguard-rules.pro](../android/tts-engine/proguard-rules.pro) preserve JNA reflection. Do not remove them when changing packaging.

## Focused checks

For resource validation and the same selected host tests without packaging:

```sh
python3 android/test_repository.py
cargo test -p ptts --features hf
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

This makes public network requests; no gated-model token is needed. See the historical [1.0.0 validation](https://github.com/Zwiebelrostbraten/phonon-tts-android/blob/v1.0.0/android/tts-engine/RELEASE_1.0.0_VALIDATION.md) for recorded evidence and limitations. Host tests and downloaded voice validation do not establish Android runtime behavior.

## Device smoke checks

Use harmless sample text and record the device, CPU, Android version, app version, model and voice. Check:

- Install a distributor-signed build under the new application identity; verify the settings entry opens this app. No old data migration is expected.
- Download the public English model, select the system TTS engine and play app/system samples offline after installation.
- Download an optional voice pack, change language/model and confirm routing. Selecting multilingual should update all five defaults.
- Test cancellation, model switch during speech, deletion confirmation/cancellation and remaining model/voice availability. Native handle lifecycle and deletion UI need device confirmation.
- Check light/dark UI, all five locales, accessibility and tempo. Pitch requests should have no effect.
- In a compatible reader, test API 26+ sentence-position callbacks; do not interpret them as word alignment or promise Kindle compatibility.

## Repository scope

The Cargo workspace contains only `ptts` (shared inference core and regression fixtures) and `ptts-ffi` (CPU C/JNA interface). Native version remains 0.4.1; the app is 1.1.0, code 14. Shared generic engine code and optional library features remain to avoid changing inference behavior; Android enables only tokenization (`hf`) and the CPU backend. Other platform frontends, desktop commands and upstream automation live in Git history and the [upstream project](https://github.com/gradium-ai/xn-ptts).

## Source map

Paths below are relative to `android/tts-engine/src/main/java/ai/gradium/phonon/tts/`:

| Area | Files |
|---|---|
| Android TTS service and audio | `PhononService.kt`, `SentenceSpeech.kt`, `SpeechAudio.kt` |
| Compose settings | `SettingsScreen.kt`, `SettingsActivity.kt` |
| Model catalog and lifecycle | `ModelCatalog.kt`, `Inventory.kt`, `CheckpointSwap.kt`, `SelectionPolicy.kt` |
| Optional public voices | `VoicePacks.kt`, `VoicePackStore.kt`, `VoiceCatalogPolicy.kt` |
| Credentials and network policy | `TokenStore.kt`, `SecureDownload.kt` |

The shared native wrapper is [android/PhononTTS.kt](../android/PhononTTS.kt); CPU library integration is described in [android/README.md](../android/README.md). The wrapper is compiled into the installable app.
