# Shared CPU runtime

This is the Phonon 0.4.1 inference library used by the Android app through
[`ptts-ffi`](../ptts-ffi/README.md). See [Android development](../docs/android-development.md)
for builds and tests, and [Gradium’s upstream project](https://github.com/gradium-ai/xn-ptts)
for the original runtime documentation. This fork is community maintained.

Shared engine modules and optional backend/audio/Hub features remain to preserve the
runtime’s types, feature gates and regression tests without refactoring inference.
The Android FFI enables only `hf` for tokenization and uses CPU inference; downloads
and model selection belong to the app. Desktop commands and platform frontends are
not part of this source tree.

Code is MIT OR Apache-2.0; model and voice terms are separate.
