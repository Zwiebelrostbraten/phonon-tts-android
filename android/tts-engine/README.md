# Phonon TTS — Android community fork

This is the Android system TTS app from [gradium-ai/xn-ptts](https://github.com/gradium-ai/xn-ptts), extended in the [Zwiebelrostbraten community fork](https://github.com/Zwiebelrostbraten/phonon-tts-android). It is not an official Gradium Android release. App version: **0.4.1-android.4**, versionCode **5**. The upstream Rust workspace version is unchanged.

## Install and use

1. Install `Phonon-TTS-v0.4.1-android.4-debug.apk`, allowing installation from your browser/file manager when Android asks. This preview is debug-signed. The app ID remains `ai.gradium.phonon.tts`; an upstream installation signed with another key must be uninstalled first, which removes its app data.
2. Open **Phonon TTS**, choose a model, and tap **Download model / Modell herunterladen**. English needs no account. German and multilingual need personal Hugging Face approval: open the model page, accept the license, and enter your own HF read token **in the app**, never in an issue/chat. The token is encrypted with AndroidKeyStore AES/GCM and persisted in `noBackupFilesDir` when you tap **Token speichern** or start a gated download with a new token. A saved token is reused; entering another token optionally replaces it. The input stays empty after saving: an empty field does not mean deletion. Use **Gespeicherten Token löschen** to delete the saved token. A token never saved by earlier versions must be entered once again. Same-signature updates preserve it; uninstalling or clearing app data removes it. The download runs as a foreground operation; its notification reopens the app or cancels it.
3. Wait for installation **and native loading** to succeed. Failed downloads, cancellation and failed activation retain the previous checkpoint. Only one model is installed at a time.
4. Open the system text-to-speech settings using the app button, select **Phonon TTS** as the preferred engine, and choose a language the installed model supports. Apps using Android `TextToSpeech` then use this engine.
5. The preview button initializes this engine, loads its available voice list and speaks on the first tap once a real installed model is ready. Select a voice matching the sample language when available. The choice persists per language; explicit client voice requests take precedence. Models without optional voices use their default voice. Tempo is adjustable from 50–200%; original voice pitch is preserved. All Android pitch requests are ignored. The engine cannot remove the pitch slider from Android system settings.

Kindle apps do not consistently expose Android system TTS; installing an engine cannot add Kindle read-aloud support. Use a reader that supports the Android TTS API. Android device/runtime behavior remains untested in this build; the user will test on a phone.

## Models and licenses

No model weights are bundled in the APK. The APK includes 13 public Apache-2.0 German voice embeddings (3,329,768 bytes) from `gradium/phonon-voices`, pinned to revision `0ee17205f254b5abc43b266c8a0f215565c352f7`. Existing German and multilingual checkpoints receive them on next model/process load without a model download or token. English remains English. Imported/default voices are preserved; the loaded checkpoint determines the available voice list. Downloads are explicit and go directly to pinned Hugging Face revisions.

| Choice | Model | Download (decimal MB) | License |
|---|---|---:|---|
| English (free) | [gradium/phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en) | 65.1 | Apache-2.0, no account |
| German | [gradium/phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de) | 75.1 | CC-BY-NC-4.0, gated |
| Multilingual | [gradium/phonon-40m-multilingual](https://huggingface.co/gradium/phonon-40m-multilingual) | 75.1 | CC-BY-NC-4.0, gated |

Model attribution: **Gradium**. CC-BY-NC models restrict commercial use and require attribution; accept and follow the model page terms yourself. The English model speaks English only, not German. Available languages come from the checkpoint; bundled German voices are added only to compatible checkpoints.

Each download installs `config.json`, `tokenizer.json`, `model.q8.gguf` and `default-voice.safetensors` together. Sizes and cryptographic digests are pinned in `ModelCatalog.kt`. The freely accessible English JSON files were fetched over TLS and SHA-256 hashed on the build host. Gated JSON contents returned HTTP 401 without approval; their publicly available pinned Git blob SHA-1 identities are used instead of inventing SHA-256 values or bypassing access control. Weights and voice files use the supplied SHA-256 hashes. Fixed TLS hosts, at most five redirects, identity encoding, exact byte counts, streaming hashes, `.part` files and disk limits protect the download. HF tokens are sent only to `https://huggingface.co`, never to CDN redirects, and are stored encrypted for reuse as described above. Backgrounding, model changes and downloads clear the input text without deleting the saved token. Background cancellation removes staging; the installed model is kept. App data backup is disabled.

**Import a checkpoint folder** remains available through Android's Storage Access Framework. Choose a subfolder containing the same files (or `model.safetensors`), optionally `voices/` or `embeddings/`. Imports are limited to 1 GiB total, 512 MiB weights, 16 MiB per voice and 4 MiB per JSON file. Imported files have no trusted manifest, so native checkpoint loading validates them before installation. Do not import untrusted checkpoints.

## Requirements and behavior

Android 7+ (API 24), 64-bit ARM or x86_64. The upstream default ARM64 CPU build requires **dotprod and FP16 instructions (ARMv8.2)**; ARM64 alone does not guarantee these features. There is no QNN/NPU dependency. Provide enough RAM to load the checkpoint; validating a replacement briefly keeps both models in memory, and Android can terminate the process under memory pressure. Keep free storage for the new download plus 32 MiB while the old model is retained.

Synthesis is local CPU processing, with two Rayon threads, at 24 kHz mono. Only the explicit downloader uses the network; text/audio are never uploaded. Android owns playback. Per-request Sonic processing changes tempo while preserving voice pitch, flushes its tail, respects stop/output errors and aligns output to 16-bit samples. Tempo at 100% bypasses Sonic exactly. Values outside 50–200% are clamped. Model replacement waits for active speech before closing the old native model; a queued request racing a replacement can report a synthesis error and should be retried.

Upstream MIT/Apache-2.0 licenses, Sonic Apache-2.0 license and attribution are available offline in **Licenses / Lizenzen**. Sonic's Java source is pinned to `b93885dcb70aae50c6f76b0fe4e0868f029a077e`; only its package declaration changed.

## Build and focused checks

JDK 17, SDK platform 36, NDK and Rust/cargo-ndk are required. The existing CPU native build supplies the real libraries; do not publish an APK missing them.

```sh
./android/build.sh
cd android/tts-engine
./package-preview.sh
```

For manual preview packaging run `./android/tts-engine/package-preview.sh` from the repository root. It refuses missing native libraries, runs only the build and focused JVM tests, checks ELF architectures/JNA/licenses and all 13 bundled embedding sizes/digests in the APK, then copies the named APK and SHA-256 sidecar to the parent directory. No CI is enabled.

Output: `build/outputs/apk/debug/phonon-tts-engine-debug.apk`. Verify that it contains **both** `lib/arm64-v8a/libptts_ffi.so` and `lib/x86_64/libptts_ffi.so`, JNA dispatch libraries and `assets/licenses/`. Debug signing is for preview testing; use a maintained private signing key for a production distribution.

Focused JVM checks cover ignored pitch requests and independent waveform tempo, request percentage mapping, bounded/aligned output, token stripping, redirect policy, complete/hash validation and checkpoint rollback/crash recovery, plus AES/GCM integrity and storage replacement failure handling (9 focused JVM tests). These host checks do not verify AndroidKeyStore or update persistence on a device. No emulator or broad test suite is required. These checks do not establish Android runtime quality or native speech correctness. See [FORK_NOTES.md](FORK_NOTES.md) for the actual build results.

Version .4 supplies sentence-level playback positions via Android API 26+ rangeStart.
Markers use original UTF-16 text offsets and actual 24 kHz PCM frames after tempo
processing. Each sentence is synthesized separately, which can change pauses/prosody.
No word alignment is available; exact word highlighting is not claimed. API 24–25
still plays speech without range notifications. Kindle behavior awaits phone testing.
