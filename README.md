<img src="docs/readme/app-icon.svg" width="48" height="48" alt="Phonon TTS app icon">

# Phonon TTS for Android

**[Deutsch — vollständige Anleitung](README.de.md)** · Android 7+ · Five languages · Community preview

An Android system text-to-speech engine based on [Gradium Phonon](https://github.com/gradium-ai/xn-ptts). Install models once and synthesize speech locally on the CPU, including from apps that use Android’s TextToSpeech service. This community fork is not an official Gradium app.

**[Download APK](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/download/android-v0.4.1-11/Phonon-TTS-v0.4.1-android.11.apk)** · [Release notes](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/tag/android-v0.4.1-11) · [Install](#install) · [Models](#models) · [Voices](#voices) · [Privacy](#privacy) · [Development](docs/android-development.md)

Current preview: **0.4.1-android.11** (versionCode 12). Releases are marked as prereleases; use the links above or [all releases](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases).

## What the app supports

- Seven original Q8 checkpoints installed side by side; English, German, French, Spanish and Portuguese speech, depending on the model.
- Optional public voice packs, voice selection, sample playback and tempo from 50–200%. Pitch adjustment is disabled, including Android pitch requests.
- Material 3 light/dark settings in all five languages, following Android system/app language settings; other UI languages fall back to English.
- Sentence-level reader positions on Android 8+ (API 26), when the reader uses them. No word-level timing or automatic detection of the text’s language.

## Install

1. Download the APK above. Android 7+ (API 24) and ARM64 or x86_64 are required. The ARM64 build needs **dotprod and FP16 (ARMv8.2)**; a 64-bit phone alone is insufficient. This APK uses CPU inference.
2. Open the APK and, if prompted, allow installation from the browser or file manager you used. This source permission differs from a harmful-app warning: if a security warning appears, stop and verify the download and release information; do not disable Play Protect.
3. Install over the existing preview to retain models, voices, preferences and the saved token. Do not uninstall or clear app data. Published previews use the same debug signing key, including this minified release build.
4. Open Phonon TTS and download a model. Start with **phonon-31m-en** for English without an account. For gated models, follow the access steps below.
5. Select the installed model, spoken language and voice; try the sample. Voice packs are optional, separate downloads.
6. In Android settings, search for **Text-to-speech output**, select **Phonon TTS** as the preferred engine and play the system sample. Setting names vary by device. Your reader must support Android system TTS.

## Models

| Checkpoint | Speech languages | License / access |
|---|---|---|
| [phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en) | English | Apache-2.0 · no account |
| [phonon-40m-en](https://huggingface.co/gradium/phonon-40m-en) | English | CC-BY-NC-4.0 · gated |
| [phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de) | German | CC-BY-NC-4.0 · gated |
| [phonon-40m-fr](https://huggingface.co/gradium/phonon-40m-fr) | French | CC-BY-NC-4.0 · gated |
| [phonon-40m-es](https://huggingface.co/gradium/phonon-40m-es) | Spanish | CC-BY-NC-4.0 · gated |
| [phonon-40m-pt](https://huggingface.co/gradium/phonon-40m-pt) | Portuguese | CC-BY-NC-4.0 · gated |
| [phonon-40m-multilingual](https://huggingface.co/gradium/phonon-40m-multilingual) | All five | CC-BY-NC-4.0 · gated |

For each gated model, request access and personally accept its terms on the linked Hugging Face page, then wait for approval. Create a Hugging Face **read** token and explicitly save it in the app. Approval for one checkpoint does not grant access to the others. The 40M models are restricted to noncommercial use with attribution; code licensing does not override model terms.

Models include a default voice and download approximately 65 MB (31M) or 75 MB (40M). No model weights are in the APK. Selecting a model sets it as the default for **every language it supports**: multilingual changes all five defaults, while unrelated language defaults stay intact. Selection does not translate text.

Delete an installed model with its delete icon and confirm the named entry. Other models, shared voice packs and the saved token remain. Local checkpoint import is also available; declared checkpoint languages determine availability.

## Voices

Optional [public Gradium voices](https://huggingface.co/gradium/phonon-voices) are Apache-2.0 and require no token: **37 English, 13 German, 15 French, 25 Spanish and 12 Portuguese — 102 total**.

Use the download action beside the selected voice to fetch that language’s pack once. Packs are shared across matching models; activation may also keep checkpoint-local copies. The APK contains names, hashes and attribution only, **no voice embeddings**, including German. The default voice arrives with the model. Existing German voice files and preferences are preserved by updates.

## Privacy

Speech text and generated audio are processed on device. Model and voice downloads connect to Hugging Face and allowed CDNs; synthesis works offline after installation. The app has no additional telemetry code.

A saved Hugging Face token is masked and encrypted using AndroidKeyStore AES/GCM, with atomic storage in `noBackupFilesDir`. It is sent only to Hugging Face, never CDN redirects. Empty input does not erase it; use the explicit delete action. Never include tokens or private reading text in reports.

## Troubleshooting

| Symptom | Check |
|---|---|
| A voice is missing | Download the pack for that spoken language; only available voices are advertised. |
| Wrong model for a language | Select the intended model again. Multilingual selection changes all five defaults. Check the reader’s language and explicit voice too. |
| Model loading takes time | Loading and switching need CPU time and enough RAM for one checkpoint. No phone speed guarantee is implied. |
| An update will not install | Verify its source and signing compatibility. A build made elsewhere may have a different key. Keep existing app data while investigating. |
| Reader highlighting is absent | The reader must use API 26+ sentence range callbacks; word-precise highlighting is unavailable. |

Kindle behavior depends on the book, app, device and available Assistive Reader functionality. Support is not guaranteed; this app does not bypass DRM.

## Development and project scope

See [build instructions and smoke checks](docs/android-development.md), [contributing](CONTRIBUTING.md), [security reporting](SECURITY.md) and the [latest recorded validation](android/tts-engine/UPDATE11_VALIDATION.md). Host checks do not establish Android device behavior; native lifecycle, deletion UI and reader integration still need device confirmation.

- `android/tts-engine/` — Android app, Compose settings, system TTS service and host tests.
- `android/PhononTTS.kt` — shared Kotlin native wrapper; `android/build.sh` builds CPU libraries.
- `ptts/` and `ptts-ffi/` — upstream Rust runtime and C interface.
- [Historical upstream README](README.upstream.md) — CLI, Python, Wasm and other runtime integrations, separate from this APK.

## Credits and licenses

Phonon and its models are by **Gradium**; this repository maintains a community Android fork of [gradium-ai/xn-ptts](https://github.com/gradium-ai/xn-ptts). Code is available under [MIT](LICENSE-MIT) **or** [Apache-2.0](LICENSE-APACHE). Models have the separate terms listed above; public voice packs are Apache-2.0. See the [bundled notices](android/tts-engine/src/main/assets/licenses/NOTICE.txt), [voice attribution](android/tts-engine/src/main/assets/licenses/Voices-NOTICE.txt) and [Sonic license](android/tts-engine/src/main/assets/licenses/Sonic-LICENSE.txt).
