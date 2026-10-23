# Android fork implementation notes

Base: upstream `6fc8b65f7e71146517098aa428c8cfa135b4fcb5` in the existing community fork. No new engine or renamed upstream ownership claim. No commits, pushes, GitHub API calls, emulator, CI runs or broad test suite performed for this task. Rust sources, Cargo files, native build script and shared Kotlin wrapper were left unchanged. The native CPU build is complete; both real ABI libraries are packaged and verified.

Implemented in the existing `android/tts-engine` app:

- German resources with English fallback, three fixed model choices with sizes/licenses, explicit foreground downloads, progress/cancel, gated-model page/read-token entry, SAF import, explicit-engine Android `TextToSpeech` preview and offline license viewer.
- Revision/size/digest manifest; finite allowlisted HTTPS redirects with no CDN authorization, exact-length streaming verification, synced `.part` files and disk limits. No token storage, intent extras, logging or APK credentials. Tokens are cleared from the input on use/backgrounding; the active service job retains a token only until it completes.
- Native staging validation before replacing the old checkpoint; serialized installs, backup/swap/rollback, crash recovery, speech lock coordination and success only after native load. Imports preserve SAF read permission for the foreground operation and release it afterward.
- Per-request Sonic tempo with fixed original pitch at fixed 24 kHz mono, 50–200% clamp, exact 100% passthrough, bounded/aligned output, stop/output status handling and flushed tail. Upstream Sonic source commit `b93885dcb70aae50c6f76b0fe4e0868f029a077e`, package-only change, original header/license retained.
- Android versionName `0.4.1-android.2`, versionCode `3`; only arm64-v8a/x86_64 advertised. Existing app ID retained, with signature/uninstall caveat. Community attribution and original licenses included offline.
- Manual `package-preview.sh` builds and checks native ELF architectures, JNA and license assets before producing a named preview APK/checksum. It refuses to distribute app-only APKs missing the real CPU libraries. No workflow is enabled.

Validation performed on the host (2026-10-23):

- Existing AGP 8.13.0, Kotlin 2.2.20, Gradle 8.13, SDK 36, JDK 17 build succeeded without downgrades.
- `assembleDebug testDebugUnitTest` passed. Seven focused JVM tests cover waveform duration/frequency independence, request percentage mapping, output alignment/stop, TLS redirect/token policy, hash/length rejection and transaction rollback/crash recovery. No Android native speech/runtime test is implied.
- English config/tokenizer fetched over HF TLS: SHA-256 `b5d0f88353638370d94035b7d73d7d6577c0554d78f8d7ecfd40490aae39ae85` / `691a00068fa7b989f2f3e5ae5daa2d1a5bb319f2d5360deafcfb19a8bb6e7936`.
- Gated JSON content returned HTTP 401 without personal access approval. The manifest uses publicly visible Git blob SHA-1 identities from HF's API at the exact pinned revisions instead of fabricated SHA-256 values. Weights/default voices use the supplied SHA-256 values. No gate was bypassed and no private token was requested in chat. German JSON SHA-256 acquisition remains dependent on authorized access; authenticated app downloads are nevertheless checked against pinned Git content identities and exact sizes.

Known runtime limits: Android phone testing is pending. No host native speech smoke was run as part of this app-only work. CPU ARM64 requires upstream default dotprod/FP16 support; ARM64 alone is insufficient. Native staging validation can briefly double model memory, and Android may kill the process under memory pressure; the previous checkpoint is retained. Background execution remains subject to Android foreground-service limits and force-stop. Model swaps can cause a queued synthesis request holding an old handle to fail safely; callers may retry. A debug key is suitable for preview testing, not a long-term release identity. Kindle support depends on the reader app's use of Android TTS.

Final native-library/APK verification completed; see the final build record below.

## Final build record

Final preview APK: `/opt/data/projects/Phonon-TTS-v0.4.1-android.1-debug.apk`
Size: 17,737,907 bytes.
SHA-256: `7425609e69f0080f99a139ebc95f19eb0154de47f04528bcfa912d6ed9faa63c`
Sidecar: `/opt/data/projects/Phonon-TTS-v0.4.1-android.1-debug.apk.sha256`

`package-preview.sh` completed successfully with the real concurrent-build libraries for both ABIs. APK contents verified:

- `lib/arm64-v8a/libjnidispatch.so` (176,520 bytes)
- `lib/arm64-v8a/libptts_ffi.so` (7,573,888 bytes)
- `lib/x86_64/libjnidispatch.so` (126,912 bytes)
- `lib/x86_64/libptts_ffi.so` (8,734,168 bytes)
- `assets/licenses/LICENSE-APACHE` (11,357 bytes)
- `assets/licenses/LICENSE-MIT` (1,023 bytes)
- `assets/licenses/NOTICE.txt` (844 bytes)
- `assets/licenses/Sonic-LICENSE.txt` (11,358 bytes)

The CPU libraries passed ELF magic/class/architecture checks (AArch64 = 183, x86_64 = 62). APK signature verified by build-tools 35.0.0 `apksigner` (v2, Android Debug certificate); `zipalign -c -P 16 4` passed. This verifies packaging/signing/alignment, not Android runtime behavior.

Final `assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL; 7 focused JVM tests, 0 failures, 0 errors, 0 skipped. No missing-native-library blocker remains. No Android device/emulator or host native speech synthesis was run. Source remains uncommitted/unpushed for the parent publishing task.


Pre-publication blocker fixes (2026-10-23):

- Added only the exact trusted `us.aws.cdn.hf.co` host observed in the parent's HTTP 302 HEAD response for pinned phonon31m weights (64,512,352 bytes, linked SHA-256 prefix `a7d9aad2`). Regression coverage accepts HTTPS/default or explicit port 443 and rejects HTTP, alternate ports, userinfo, fragments, sibling hosts and suffix spoofing. The HF token remains limited to `huggingface.co`; the CDN receives no Authorization header. Finite redirect, byte and digest policies remain intact.
- First preview tap now resumes its captured text/rate request after successful initialization, posted to the main handler after assigning `preview` and populating optional voices. Ready installed-model checks run before initialization and again before speaking; models without optional voices use the default. Stop, cancellation and activity pause/destroy clear pending speech. Service honors caller tempo; pitch requests are now ignored.
- Rebuilt using `package-preview.sh`: `BUILD SUCCESSFUL in 3s`; `45 actionable tasks: 8 executed, 37 up-to-date`. JUnit XML reports 7 tests, 0 failures, 0 errors, 0 skipped. Signature verification and 16 KiB zip alignment passed. Only the focused app unit tests/build were run; no emulator, lint or full suite.
- Build log: `/opt/data/profiles/mrs_patmore/scratch/phonon-preview/package-preview.log`; signature/alignment logs are in the same profile scratch directory. APK/checksum above supersede the earlier preview. Android preview runtime testing remains pending.

## Pitch removal (0.4.1-android.2)

Removed the preview pitch slider; tempo remains 50–200%. Preview resets pitch to 1f. Service never reads SynthesisRequest.pitch, and SpeechAudio fixes effectivePitch to 1f for all inputs. Android system pitch controls cannot be removed by the engine; their requests are explicitly ignored. No voice downloader added; weights, native libraries, models, tokens and installed state are unchanged. The .1 artifact above is retained as the previous build.

Regression checks compare PCM byte-for-byte across extreme pitch requests at 50%, 100% and 200% tempo, and check duration and original 220 Hz frequency. The new regression failed before the fix (RED).

GREEN: package-preview.sh completed assembleDebug and all 7 focused JVM tests (0 failures/errors/skips). New APK: `/opt/data/projects/Phonon-TTS-v0.4.1-android.2-debug.apk`; SHA-256 `7bbf716e737832a7db3f9b989b4d1b725e4a006ff93c00f092c069e94ceea05f` (matching `.sha256` sidecar). apksigner verified both APKs; certificate SHA-256 unchanged: `63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0`. All 4 packaged native libraries are byte-identical to .1. Logs: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon-pitch`. No emulator, review, full suite, CI, commit, push or GitHub API.
