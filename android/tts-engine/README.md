# Phonon TTS for Android

This directory contains the community Android system TextToSpeechService app (`ai.gradium.phonon.tts`), based on Gradium Phonon. It uses local CPU inference; upstream QNN and other platform integrations are separate.

- **[Install and use](../../docs/user-guide.md)**
- [Models and access requirements](../../docs/user-guide.md#models)
- [Optional voice packs](../../docs/user-guide.md#voices)
- [Privacy and token storage](../../docs/user-guide.md#privacy)
- [Build, focused tests and device smoke checks](../../docs/android-development.md)
- [Stable 1.0.0 validation](RELEASE_1.0.0_VALIDATION.md)

The user guide is the current user reference. Existing [release notes](RELEASE_NOTES.md) and [fork notes](FORK_NOTES.md) retain historical implementation and validation details; old descriptions of bundled German voice embeddings do not describe the current APK. All public voice embeddings now require an explicit optional download.

Current app release: **1.0.0 / versionCode 13**, runtime Cargo **0.4.1** unchanged. [Download](https://github.com/Zwiebelrostbraten/phonon-tts-android/releases/download/v1.0.0/Phonon-TTS-v1.0.0.apk). This is a stable community-fork release, not an official Gradium release.
