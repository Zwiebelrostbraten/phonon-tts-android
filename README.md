<p align="center">
  <img src="docs/readme/app-icon.svg" width="96" height="96" alt="Phonon TTS app icon">
</p>
<h1 align="center">Phonon TTS</h1>
<p align="center">Offline text-to-speech for Android.</p>

<p align="center">
  <a href="https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/latest"><img src="https://img.shields.io/github/v/release/Zwiebelrostbraten/phonon-tts-android?label=stable" alt="Latest stable release"></a>
  <a href="#getting-started"><img src="https://img.shields.io/badge/Android-7%2B-3DDC84" alt="Android 7 and later"></a>
  <a href="#license"><img src="https://img.shields.io/badge/code-MIT%20OR%20Apache--2.0-blue" alt="Code license: MIT or Apache-2.0"></a>
</p>

<p align="center">
  <a href="https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/latest"><strong>Download APK</strong></a> ·
  <a href="#getting-started">Getting started</a> ·
  <a href="docs/user-guide.md">User guide</a> ·
  <a href="docs/android-development.md">Development</a>
</p>

Phonon TTS brings [Gradium Phonon](https://github.com/gradium-ai/xn-ptts) to Android as a system text-to-speech engine. Use it with reading and accessibility apps that support Android’s TextToSpeech service. After downloading a model and any optional voices, speech is generated entirely on your device, without an internet connection.

## Features

- Speak English, German, French, Spanish and Portuguese with seven models you can install side by side.
- Choose from 102 downloadable public voices, preview speech and adjust playback speed from 50–200%.
- Set a default model for each language and remove models individually.
- Use a Material 3 interface with light and dark themes that follows your Android language settings.
- Generate speech locally on the CPU; speech text and audio stay on your device.

## Getting started

**Requirements:** Android 7+ (API 24), with ARM64 supporting **ARMv8.2 dotprod and FP16**, or x86_64. Not all 64-bit phones are compatible. This app uses CPU inference.

1. [Download the latest APK](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/latest) and install it on your device.
2. Open Phonon TTS and download a model. Start with **phonon-31m-en** for free English speech without an account.
3. Select your model, language and voice, then listen to the sample. Download additional voices if desired.
4. In Android’s **Text-to-speech output** settings, select **Phonon TTS** as your preferred engine.

The initial model download is required (approximately 65–75 MB). The English 31M model uses Apache-2.0; the six 40M models require Hugging Face approval and a read token and are licensed **CC-BY-NC-4.0 for noncommercial use**. See [model access and setup](docs/user-guide.md#models) before downloading them.

## Documentation and development

The [user guide](docs/user-guide.md) covers models, voices, local imports, updates, privacy and troubleshooting. For source builds and device checks, see [Android development](docs/android-development.md). Contributions are welcome: read [contributing](CONTRIBUTING.md) and use [security reporting](SECURITY.md) for vulnerabilities.

## License

This community-maintained app is based on [Gradium’s Phonon](https://github.com/gradium-ai/xn-ptts). Code is licensed under [MIT](LICENSE-MIT) **or** [Apache-2.0](LICENSE-APACHE). Models have separate terms; public voices are Apache-2.0. See [credits and third-party notices](docs/user-guide.md#credits-and-licenses) and the [upstream runtime documentation](https://github.com/gradium-ai/xn-ptts).
