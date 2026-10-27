# Official F-Droid submission preparation

Phonon TTS Community 1.1.0 (version code 14) is an independent community fork of Gradium Phonon, with application ID `io.github.zwiebelrostbraten.phonontts` and original speech-bubble/equalizer artwork. The retained Kotlin namespace is an implementation detail. The fork maintainer supports inclusion in the official catalog and requests reviewer advice on optional CC-NC models. Preparation and local checks do **not** mean acceptance, listing, a full F-Droid VM build, device validation or verified reproducibility.

## Licensing and review

F-Droid distribution selects **MIT**, exercising one option of the dual license without relicensing the source. Both `LICENSE-MIT` and `LICENSE-APACHE` stay. Code remains **MIT OR Apache-2.0**, with upstream attribution and third-party notices retained. The English `phonon-31m-en` checkpoint and public voice embeddings use Apache-2.0. The six optional 40M checkpoints use **CC-BY-NC-4.0**, require Hugging Face approval and a user-provided read token. No weights or embeddings are bundled. All seven model choices, five UI languages and 102 downloadable public voices remain available, including German and multilingual speech.

The recipe proposes `NonFreeAssets` with an English explanation. These functional speech models may require a different anti-feature classification or affect eligibility: **F-Droid maintainers decide**. Please review the optional download flow, consent, licensing and Hugging Face service use; an anti-feature declaration is not a waiver. The app can provide English speech with freely licensed assets. Accepted [Ramblr metadata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.trevornk.ramblr.yml) declares NonFreeAssets for an optional, consented downloaded model; this supports review of the proposal but does not guarantee acceptance. No token, account or gated download is required to compile the app.

See the official [inclusion policy](https://f-droid.org/docs/Inclusion_Policy/), [anti-features](https://f-droid.org/docs/Anti-Features/) and [submission guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/). Do not claim reproducible builds without independently comparing identical APKs; see [F-Droid reproducibility](https://f-droid.org/docs/Reproducible_Builds/).

## Source build

Use JDK 17, SDK package `platforms;android-37.0`, build-tools 36.0.0, NDK **27.2.12479018**, Rust/Cargo **1.99.1** with both Android targets, and cargo-ndk **4.1.2**. Gradle **9.3.1** is checksum-pinned in the wrapper; AGP stays **9.1.1**, target SDK 36 and minimum API 24. The engine stays `ptts`/`ptts-ffi` **0.4.1**; do not update Cargo.lock to provision dependencies.

Provision explicitly while online (official rustup installer/toolchain must already be available):

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'
rustup toolchain install 1.99.1 --profile minimal \
  --target aarch64-linux-android,x86_64-linux-android
cargo install cargo-ndk@4.1.2 --locked
cargo fetch --locked --target aarch64-linux-android --target x86_64-linux-android
```

Then, in a fresh source checkout/archive with no `target/`, `android/jniLibs/` or Android build directories, reuse only dependency/tool caches:

```sh
export ANDROID_HOME=/path/to/sdk
export JAVA_HOME=/path/to/jdk-17
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
export CARGO_BUILD_JOBS=2
export TMPDIR=/path/to/writable/scratch
mkdir -p "$TMPDIR"
python3 -m unittest android.test_repository android.test_apk
bash android/build.sh --offline
python3 android/tts-engine/validate-resources.py
android/tts-engine/gradlew -p android/tts-engine assembleRelease testDebugUnitTest --console=plain
python3 android/tts-engine/verify-apk.py \
  android/tts-engine/build/outputs/apk/release/phonon-tts-engine-release-unsigned.apk
```

`--offline` uses Cargo `--frozen` and never invokes rustup installation or dependency downloads. Installed shims are configured not to auto-install. A locally named toolchain alias may be selected with `RUSTUP_TOOLCHAIN` only if its rustc/cargo are exactly 1.99.1; the script checks both. Missing targets/dependencies fail. Gradle's first build still needs Maven dependencies and the Gradle distribution; after those are prefetched, add `--offline` to Gradle for an offline Android build. Native compilation and Gradle dependency provisioning are separate operations. The canonical installed SDK is `android-37.0` (`AndroidVersion.ApiLevel=37.0`); offline `checkReleaseAarMetadata` and `processReleaseResources --rerun-tasks` pass with `compileSdk = 37`, `android.newDsl=false`, no `local.properties`, and no `android-37` directory or alias. The existing compatible DSL is retained.

Both arm64-v8a and x86_64 JNI libraries are compiled from repository sources and retain the CPU C ABI. ARM defaults to dotprod/FP16 (optional `ARM64_FEATURES` override for separate local older-CPU builds); x86 uses baseline x86-64/SSE2. ELF LOAD segments and uncompressed APK library entries are checked for 16 KiB compatibility. Source paths are remapped for native builds, but no identical-APK reproducibility result is asserted.

Local preparation checks compiled both ABIs from a clean source archive with frozen native dependencies, built the optimized unsigned APK, and passed resource/ABI/alignment checks, 12 Python tests (including four metadata contracts), 46 JVM tests (1 optional network test skipped) and 132 Rust tests/doctests (1 private-model test ignored). The local fdroidserver source scan found zero fatal findings or warnings. Metadata uses the MIT distribution option; real lint and canonical formatting are checked with official category/anti-feature definitions.

The recipe itself was also executed end to end by fdroidserver 2.4.5 (`fdroid build --test --latest --no-tarball --scan-binary`) against an exact copy of this source tree served from a temporary local Git mirror, with no build or scan skips: it cloned the tag, scanned the source, provisioned Rust, cargo-ndk, the SDK package and Gradle, compiled both JNI ABIs from this repository, assembled the release, scanned the APK for known non-free classes and extra signing blocks, and reported `1 build succeeded`. The resulting unsigned APK passed the identity/version/SDK/ABI/alignment/weights checks. This exercised the recipe, not the official build-server VM image, and the maintainers' own infrastructure remains the authoritative run.

## Signing and identity audit

Release is optimized and **unsigned by default**. It never uses the debug signer. The resulting unsigned APK is build evidence, not directly installable. F-Droid would compile the tagged sources and sign the resulting APK with its own app key; it does not sign or republish the locally supplied APK. Other distributors must sign their own artifacts separately. No private keys or credentials belong in the source tree, metadata or build logs.

There is no data migration or compatibility layer for older installations. Manifest components use full Kotlin class names; `tts_engine.xml` correctly names `ai.gradium.phonon.tts.SettingsActivity` (a class, not an application ID). Runtime TTS selection uses the context's `packageName`; manifest permission placeholders use `${applicationId}`. Instrumentation also uses the target context package. No app-defined provider/account authority or old explicit package intent exists; the merged AndroidX startup provider uses the new application ID in its authority. The Keystore alias is app-UID scoped and does not share keys across these identities. Historical releases, tags, validation records and signatures are unchanged.

## Metadata handoff and manual checklist

The copyable recipe is [fdroid/metadata/io.github.zwiebelrostbraten.phonontts.yml](../fdroid/metadata/io.github.zwiebelrostbraten.phonontts.yml). Standard Fastlane English metadata includes an original 512 px icon and code-14 changelog. Re-render it with `python3 docs/readme/render-icon.py` (CairoSVG). Real screenshots are optional visual polish: capture them from an actual compatible device later; no mock screenshots or emulator claims are included.

The recipe uses official `rustup@1.28.2` srclib provisioning and `RUSTUP_VERSION=1.28.2` to pin the fetched installer, exact Rust/cargo-ndk/NDK, dependency fetch then frozen JNI compilation. It deliberately uses manual `output` and a checksum-verified official Gradle 9.3.1 distribution: this is an explicit-tooling choice, not a required workaround. The separately maintained F-Droid Gradle selector supports wrapper 9.3.1 and AGP 9.1 (and retains wrapper properties when removing executable wrappers). Provisioning is in `build`, which current recipes support. Tool caches live outside the checkout, after source scanning. No scanner exclusions hide app sources or dependencies.

1. Review the source changes, test logs and unsigned APK. Independently review licensing and download/token security. Obtain real device smoke results when possible; none are claimed here.
2. Verify that public source tag **v1.1.0** points to the reviewed commit, with versionName 1.1.0/versionCode 14 and matching Fastlane files. This source tag does not imply an installable GitHub APK or an F-Droid listing.
3. In a separate current fdroiddata checkout, copy the YAML to `metadata/`, retain its official `srclibs/rustup.yml` and config definitions, and run `python -m unittest android.test_fdroid` from the source root using the fdroidserver Python environment, then `fdroid lint -f io.github.zwiebelrostbraten.phonontts` and `fdroid readmeta -W error`.
4. Run `fdroid scanner io.github.zwiebelrostbraten.phonontts --verbose` and `fdroid build --server io.github.zwiebelrostbraten.phonontts:14` on the tagged public source. Review all scanner findings. Use a current supported runner and confirm the exact NDK is installed. Do not substitute a cached JNI library.
5. Run `fdroid rewritemeta` and verify `fdroid lint -f` exits successfully with zero actual errors. `License: MIT` selects a valid source license option; preserve both legal files and notices. Full VM execution is still required; local source building is not a substitute.
6. Open a metadata merge request to official fdroiddata only after manual approval, using the text below and attaching actual results. Ask maintainers to decide optional-model eligibility/anti-features. Do not mark acceptance or reproducibility checkboxes without evidence.

Suggested MR text:

> Add Phonon TTS Community, an independent Android system TTS fork. I maintain this fork and support inclusion. Code is MIT OR Apache-2.0; source release builds are unsigned. The recipe builds both CPU JNI ABIs from this repository with pinned tools. No model weights/embeddings are bundled. English 31M speech/public voices use Apache-2.0; six optional gated 40M checkpoints use CC-BY-NC-4.0. NonFreeAssets is proposed with disclosure; please advise on functional-model eligibility and any additional classification. Seven models and all five languages remain available. Local validation: [insert verified command results and source commit]. F-Droid VM build: [insert actual result, or explicitly not yet run]. Device tests/reproducibility: [only claim measured results]. Metadata uses tag v1.1.0, code 14. F-Droid distribution selects MIT from the source dual-license options.
