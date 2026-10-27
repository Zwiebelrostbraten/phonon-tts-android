# Android CPU integration

The installable system TTS app is in [`tts-engine/`](tts-engine/README.md).
See the [user guide](../docs/user-guide.md) for use and
[Android development](../docs/android-development.md) for toolchains and builds.

[`build.sh`](build.sh) builds the retained `ptts-ffi` CPU runtime for arm64-v8a
and x86_64 into generated `jniLibs/`. ARM64 uses dotprod/FP16; both libraries use
16 KiB ELF alignment. A clean source build must run this script before Gradle.

[`PhononTTS.kt`](PhononTTS.kt) is the JNA wrapper compiled directly into the app.
The [C header](../ptts-ffi/include/ptts.h) and [ABI reference](../ptts-ffi/README.md)
describe native ownership and callbacks. The app’s R8 rules preserve JNA reflection,
and its ABI filters restrict packaging to the two compiled architectures.
