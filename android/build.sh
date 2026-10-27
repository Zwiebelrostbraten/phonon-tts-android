#!/bin/bash
# Source-only CPU JNI build. Provision dependencies separately; see docs/f-droid.md.
set -euo pipefail
cd "$(dirname "$0")/.."
mode="${1:-}"
case "$mode" in
    '') resolution=(--locked) ;;
    --offline) resolution=(--frozen); export CARGO_NET_OFFLINE=true ;;
    *) echo "Usage: $0 [--offline]" >&2; exit 2 ;;
esac
# An installed toolchain is required even online: this script never runs rustup.
# Resolve the real toolchain binaries first to avoid rustup shims auto-installing offline.
export RUSTUP_AUTO_INSTALL=0
[[ "$(rustc --version)" == 'rustc 1.99.1 '* ]] || { echo 'Rust 1.99.1 required' >&2; exit 1; }
[[ "$(cargo --version)" == 'cargo 1.99.1 '* ]] || { echo 'Cargo 1.99.1 required' >&2; exit 1; }
[[ "$(cargo ndk --version)" == 'cargo-ndk 4.1.2' ]] || { echo 'cargo-ndk 4.1.2 required' >&2; exit 1; }
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to NDK 27.2.12479018}"
grep -Eq '^Pkg.Revision *= *27\.2\.12479018\s*$' "$ANDROID_NDK_HOME/source.properties" || {
    echo 'NDK 27.2.12479018 required' >&2; exit 1;
}
# Preserve the optional older-ARM override; the distributed default needs dotprod/FP16.
ARM64_FEATURES="${ARM64_FEATURES-+dotprod,+fp16}"
COMMON="-C strip=symbols -C link-arg=-Wl,-z,max-page-size=16384 --remap-path-prefix=$PWD=/source"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_RUSTFLAGS="$COMMON -C target-cpu=generic${ARM64_FEATURES:+ -C target-feature=$ARM64_FEATURES}"
# x86-64 baseline (SSE2), matching the existing CPU ABI; no host-native AVX requirement.
export CARGO_TARGET_X86_64_LINUX_ANDROID_RUSTFLAGS="$COMMON -C target-cpu=x86-64"
unset RUSTFLAGS CARGO_ENCODED_RUSTFLAGS
rm -rf android/jniLibs
cargo ndk --platform 24 -t arm64-v8a -t x86_64 -o android/jniLibs build --release -p ptts-ffi "${resolution[@]}"
du -sh android/jniLibs/*/libptts_ffi.so
