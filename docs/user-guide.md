# Phonon TTS user guide

[App overview and download](../README.md) · [Android development](android-development.md)

Phonon TTS is a community-maintained Android system text-to-speech engine based on Gradium Phonon. This guide covers installation, model access, voices and everyday use.

## Install and set up

Requires **Android 7+ (API 24)** and ARM64 with **ARMv8.2 dotprod and FP16**, or x86_64. A 64-bit phone alone does not establish compatibility. Speech synthesis runs on the CPU.

1. Download the APK from the [latest stable release](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/latest) and open it. If prompted, allow installation from the browser or file manager you used.
2. Open Phonon TTS and download a model. **phonon-31m-en** provides English without an account; the 40M models require Hugging Face approval and a saved read token (see [Models](#models)). No model weights are bundled in the APK.
3. Select an installed model, spoken language and voice, then play the sample. Additional voice packs are optional downloads.
4. In Android settings, search for **Text-to-speech output**, choose **Phonon TTS** as the preferred engine and play the system sample. Setting names vary by device. Your reader must support Android system TTS.

## Updates and installation warnings

Install an update over the existing app to retain models, voices, preferences and the saved token. Do not uninstall or clear app data to resolve an update problem without first considering the data loss.

Stable 1.0.0 retains the previews’ established **Android Debug signing certificate** for update compatibility. Its release build is minified and nondebuggable. A build made elsewhere may use a different signing key and cannot update the published installation in place. If installation fails, verify the download source and signing compatibility while preserving existing app data.

Permission to install from a browser or file manager differs from a harmful-app warning. If Android displays a security warning, stop and verify the download and release information; do not disable Play Protect.

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

## FAQ and troubleshooting

| Symptom | Check |
|---|---|
| A voice is missing | Download the pack for that spoken language; only available voices are advertised. |
| Wrong model for a language | Select the intended model again. Multilingual selection changes all five defaults. Check the reader’s language and explicit voice too. |
| Model loading takes time | Loading and switching need CPU time and enough RAM for one checkpoint. No phone speed guarantee is implied. |
| An update will not install | Verify its source and signing compatibility. A build made elsewhere may have a different key. Keep existing app data while investigating. |
| Reader highlighting is absent | The reader must use API 26+ sentence range callbacks; word-precise highlighting is unavailable. |

Kindle behavior depends on the book, app, device and available Assistive Reader functionality. Support is not guaranteed; this app does not bypass DRM.

## Reading apps and playback

Tempo can be adjusted from **50–200%**. Pitch adjustment is disabled, including Android pitch requests. Material 3 light and dark themes follow Android settings, as does the interface language. The interface supports English, German, French, Spanish and Portuguese; other locales fall back to English.

On Android 8+ (API 26), Phonon supplies sentence range callbacks for readers that use them. Word-level timing and automatic detection of the text’s language are unavailable. A reader’s explicit language or voice choice can affect model routing.

## Credits and licenses

Phonon and its models are by **Gradium**. This community-maintained Android app is based on [gradium-ai/xn-ptts](https://github.com/gradium-ai/xn-ptts) and is not an official Gradium app. Code is available under [MIT](../LICENSE-MIT) **or** [Apache-2.0](../LICENSE-APACHE). Model terms are listed in [Models](#models); public voices are Apache-2.0.

See the [bundled notices](../android/tts-engine/src/main/assets/licenses/NOTICE.txt), [voice attribution](../android/tts-engine/src/main/assets/licenses/Voices-NOTICE.txt) and [Sonic license](../android/tts-engine/src/main/assets/licenses/Sonic-LICENSE.txt). The [preserved upstream README](../README.upstream.md) covers other runtime integrations.
