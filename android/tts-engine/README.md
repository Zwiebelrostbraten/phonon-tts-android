# Phonon TTS — Android community fork

This is the Android system TTS app from [gradium-ai/xn-ptts](https://github.com/gradium-ai/xn-ptts), extended in the [Zwiebelrostbraten community fork](https://github.com/Zwiebelrostbraten/phonon-tts-android). It is not an official Gradium Android release. App version: **0.4.1-android.1**, versionCode **2**. The upstream Rust workspace version is unchanged.

## Install and use

1. Install `Phonon-TTS-v0.4.1-android.1-debug.apk`, allowing installation from your browser/file manager when Android asks. This preview is debug-signed. The app ID remains `ai.gradium.phonon.tts`; an upstream installation signed with another key must be uninstalled first, which removes its app data.
2. Open **Phonon TTS**, choose a model, and tap **Download model / Modell herunterladen**. English needs no account. German and multilingual need personal Hugging Face approval: open the model page, accept the license, and enter your own HF read token **in the app**, never in an issue/chat. No token is saved. The download runs as a foreground operation; its notification reopens the app or cancels it.
3. Wait for installation **and native loading** to succeed. Failed downloads, cancellation and failed activation retain the previous checkpoint. Only one model is installed at a time.
4. Open the system text-to-speech settings using the app button, select **Phonon TTS** as the preferred engine, and choose a language the installed model supports. Apps using Android `TextToSpeech` then use this engine.
5. The preview button initializes this engine, loads its optional voice list and speaks on the first tap once a real installed model is ready. Select a voice matching the sample language when available. Tempo and pitch are independent, 50–200%. Other apps' synthesis requests supply their own tempo and pitch, which the engine honors.

Kindle apps do not consistently expose Android system TTS; installing an engine cannot add Kindle read-aloud support. Use a reader that supports the Android TTS API. Android device/runtime behavior remains untested in this build; the user will test on a phone.

## Models and licenses

No weights are bundled in the APK. Downloads are explicit and go directly to pinned Hugging Face revisions.

| Choice | Model | Download (decimal MB) | License |
|---|---|---:|---|
| English (free) | [gradium/phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en) | 65.1 | Apache-2.0, no account |
| German | [gradium/phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de) | 75.1 | CC-BY-NC-4.0, gated |
| Multilingual | [gradium/phonon-40m-multilingual](https://huggingface.co/gradium/phonon-40m-multilingual) | 75.1 | CC-BY-NC-4.0, gated |

Model attribution: **Gradium**. CC-BY-NC models restrict commercial use and require attribution; accept and follow the model page terms yourself. The English model speaks English only, not German. Installed voices/languages come from the checkpoint.

Each download installs `config.json`, `tokenizer.json`, `model.q8.gguf` and `default-voice.safetensors` together. Sizes and cryptographic digests are pinned in `ModelCatalog.kt`. The freely accessible English JSON files were fetched over TLS and SHA-256 hashed on the build host. Gated JSON contents returned HTTP 401 without approval; their publicly available pinned Git blob SHA-1 identities are used instead of inventing SHA-256 values or bypassing access control. Weights and voice files use the supplied SHA-256 hashes. Fixed TLS hosts, at most five redirects, identity encoding, exact byte counts, streaming hashes, `.part` files and disk limits protect the download. HF tokens are sent only to `https://huggingface.co`, never to CDN redirects, and are held only for the active operation. Background cancellation removes staging; the installed model is kept. App data backup is disabled.

**Import a checkpoint folder** remains available through Android's Storage Access Framework. Choose a subfolder containing the same files (or `model.safetensors`), optionally `voices/` or `embeddings/`. Imports are limited to 1 GiB total, 512 MiB weights, 16 MiB per voice and 4 MiB per JSON file. Imported files have no trusted manifest, so native checkpoint loading validates them before installation. Do not import untrusted checkpoints.

## Requirements and behavior

Android 7+ (API 24), 64-bit ARM or x86_64. The upstream default ARM64 CPU build requires **dotprod and FP16 instructions (ARMv8.2)**; ARM64 alone does not guarantee these features. There is no QNN/NPU dependency. Provide enough RAM to load the checkpoint; validating a replacement briefly keeps both models in memory, and Android can terminate the process under memory pressure. Keep free storage for the new download plus 32 MiB while the old model is retained.

Synthesis is local CPU processing, with two Rayon threads, at 24 kHz mono. Only the explicit downloader uses the network; text/audio are never uploaded. Android owns playback. Per-request Sonic processing changes tempo and pitch independently, flushes its tail, respects stop/output errors and aligns output to 16-bit samples. Both controls at 100% bypass Sonic exactly. Values outside 50–200% are clamped. Model replacement waits for active speech before closing the old native model; a queued request racing a replacement can report a synthesis error and should be retried.

Upstream MIT/Apache-2.0 licenses, Sonic Apache-2.0 license and attribution are available offline in **Licenses / Lizenzen**. Sonic's Java source is pinned to `b93885dcb70aae50c6f76b0fe4e0868f029a077e`; only its package declaration changed.

## Build and focused checks

JDK 17, SDK platform 36, NDK and Rust/cargo-ndk are required. The existing CPU native build supplies the real libraries; do not publish an APK missing them.

```sh
./android/build.sh
cd android/tts-engine
./gradlew assembleDebug testDebugUnitTest
```

For manual preview packaging run `./android/tts-engine/package-preview.sh` from the repository root. It refuses missing native libraries, runs only the build and focused JVM tests, checks ELF architectures/JNA/licenses in the APK, then copies the named APK and SHA-256 sidecar to the parent directory. No CI is enabled.

Output: `build/outputs/apk/debug/phonon-tts-engine-debug.apk`. Verify that it contains **both** `lib/arm64-v8a/libptts_ffi.so` and `lib/x86_64/libptts_ffi.so`, JNA dispatch libraries and `assets/licenses/`. Debug signing is for preview testing; use a maintained private signing key for a production distribution.

Focused JVM checks cover independent waveform tempo/pitch, request percentage mapping, bounded/aligned output, token stripping, redirect policy, complete/hash validation and checkpoint rollback/crash recovery. No emulator or broad test suite is required. These checks do not establish Android runtime quality or native speech correctness. See [FORK_NOTES.md](FORK_NOTES.md) for the actual build results.
