# Phonon TTS — Android community fork

Community Android system TTS engine based on [Gradium Phonon](https://github.com/gradium-ai/xn-ptts). Current preview: **0.4.1-android.8**, versionCode **9**. The Rust runtime is unchanged.

## Install and use

1. Install `Phonon-TTS-v0.4.1-android.8.apk` over the existing preview. The application ID (`ai.gradium.phonon.tts`) and Android Debug signing certificate are unchanged. Do not uninstall or clear app data: existing checkpoints, voice preferences and encrypted Hugging Face tokens are preserved.
2. Open Phonon TTS. The installed list shows each checkpoint, its languages and local size. Selecting an installed entry loads it for preview without downloading. Select the spoken language/voice and optionally make that model the default for that language. Preview text changes to a sample in the selected spoken language.
3. Choose a catalog model to download, or separately import a local checkpoint folder. Models coexist. Only reinstalling the same catalog identity requires confirmation. Cancellation or failed activation preserves all previous installations. Downloads run as foreground operations with progress and cancellation.
4. Select Phonon TTS in Android speech settings. Available languages are the union of installed checkpoints. A requested language uses its saved preferred model, then a deterministic specialist-before-multilingual fallback. Explicit model-qualified voices take precedence. Unknown voices are not advertised or validated. Older voice names resolve only against known voices in the language default model.
5. The UI follows Android system/app language resources: English, German, French, Spanish and Portuguese, including regional variants. Other UI languages fall back to English. Speech initialization uses the Android locale list and installed languages. This does not detect the language of arbitrary text.

## Portfolio and access

| Catalog checkpoint | Languages | License/access |
|---|---|---|
| [phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en) | en | Apache-2.0, no token |
| [phonon-40m-en](https://huggingface.co/gradium/phonon-40m-en) | en | CC-BY-NC-4.0, gated |
| [phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de) | de | CC-BY-NC-4.0, gated |
| [phonon-40m-fr](https://huggingface.co/gradium/phonon-40m-fr) | fr | CC-BY-NC-4.0, gated |
| [phonon-40m-es](https://huggingface.co/gradium/phonon-40m-es) | es | CC-BY-NC-4.0, gated |
| [phonon-40m-pt](https://huggingface.co/gradium/phonon-40m-pt) | pt | CC-BY-NC-4.0, gated |
| [phonon-40m-multilingual](https://huggingface.co/gradium/phonon-40m-multilingual) | en, de, fr, es, pt | CC-BY-NC-4.0, gated |

All seven original CPU checkpoints use `model.q8.gguf`, with pinned revision, exact size and LFS SHA-256. Config/tokenizer files use their pinned Git blob SHA-1 and exact byte count. `default-voice.safetensors` is included with every catalog download. The 31M download is about 65.1 MB, each 40M download about 75.1 MB. Attribution: Gradium. The gated models require noncommercial use with attribution and your personal acceptance on the corresponding model page. Save your own read token explicitly in the app. TokenStore is unchanged: AndroidKeyStore AES/GCM, atomic ciphertext in noBackupFilesDir, masked empty input after saving, explicit deletion. Tokens go only to huggingface.co, never CDN redirects. No gated weights were fetched for this update.

The llamacpp/audiocpp repositories are alternative conversions requiring other runtimes. `phonon-mimi` is codec-only. They are not extra usable language models in this CPU FFI. Q4 files are not offered or renamed to Q8.

No model weights are bundled. The original 13 Apache-2.0 German embeddings remain byte-identical, pinned to `gradium/phonon-voices@0ee17205f254b5abc43b266c8a0f215565c352f7` (3,329,768 bytes). These appear only for German speech, including the multilingual checkpoint. Other languages have their checkpoint default and any imported voices; German embeddings are never offered as French voices. No additional voice pack is downloaded automatically.

## Storage and lifecycle

The existing `filesDir/model` stays in place as a persistent legacy entry, including arbitrary imported checkpoints. Its `.backup` recovery marker is honored before discovery; no migration, redownload or deletion is needed. Known catalog legacy checkpoints are identified by config content hash and Q8 size. New catalog installations use `filesDir/models/<stable-catalog-id>`. Imports receive separate generated UUID IDs; SAF display names cannot choose installation paths. The importer accepts config.json, tokenizer.json, model.q8.gguf or model.safetensors, default voice and optional voices/embeddings subfolders. For an imported checkpoint, declared config languages control availability. Imports omitting language metadata conservatively use English; provide the checkpoint’s `languages` array for other languages. This avoids advertising French through an unidentified English checkpoint. Uncommitted legacy `.staging` files are retained without automatically promoting an unvalidated candidate. Limits remain 1 GiB total, 512 MiB weights, 16 MiB per voice and 4 MiB per JSON.

Each installation has isolated staging and backup directories. Native activation is inside a reversible swap; failure restores only that identity. There is one loaded native handle at a time. A shared lifecycle lock covers loading, synthesis and closing, so switching waits for speech and never closes a handle in use. Voice enumeration and language checks use file/asset metadata without loading all models. Switching large models can take time and requires enough RAM for one checkpoint. App process death can interrupt installation; restart restores a retained backup.

Android 7+ (API 24), ARM64 or x86_64. The unchanged ARM64 native build requires dotprod and FP16 (ARMv8.2); ARM64 alone is insufficient. Text/audio stay on device. Synthesis remains 24 kHz mono with two Rayon threads, sentence range markers on API 26+, tempo 50–200% and pitch changes disabled. Sentence splitting may affect pauses/prosody. Kindle support depends on the reader using Android TTS.

## Build and verification

Use the existing native libraries; this app update does not rebuild Rust. JDK 17 and SDK 37 are required for the existing AGP 9 setup.

```sh
TMPDIR=/your/scratch ./android/tts-engine/package-preview.sh
```

The script checks 66 string keys and format placeholders in all five locales, localeConfig and all seven pinned catalogs. It runs the focused JVM classes `FocusedTest`, `ProgressTest`, `SettingsStateTest`, and `InventoryTest`, then builds the R8-minified, debug-signed release. Packaging verifies both ELF architectures, JNA, licenses and all 13 embedding hashes. APK and SHA-256 sidecar are copied to the repository parent directory. No publishing occurs.

Host tests cover directory discovery/legacy recovery, cancellation, isolated rollback, routing/preferences/system locales, voice identities and invalid aliases, catalogs, audio markers/tempo, token encryption and download policy. Native activation is not simulated as success by fixtures. No device is available: Android speech, visual behavior, real update persistence and authenticated gated downloads remain untested. See the current section of [FORK_NOTES.md](FORK_NOTES.md) for build evidence.
