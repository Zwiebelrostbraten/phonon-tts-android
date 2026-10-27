#!/bin/sh
# Manual-only packaging. Never publish the app-only intermediate APK without the CPU libraries.
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(CDPATH= cd -- "$APP_DIR/../.." && pwd)
ARTIFACT_DIR=${ARTIFACT_DIR:-"$APP_DIR/build/outputs/community"}
mkdir -p "$ARTIFACT_DIR"
for abi in arm64-v8a x86_64; do
    test -s "$REPO_DIR/android/jniLibs/$abi/libptts_ffi.so" || {
        echo "Missing real native CPU library for $abi; finish android/build.sh first." >&2
        exit 1
    }
done
python3 "$APP_DIR/validate-resources.py"
"$APP_DIR/gradlew" -p "$APP_DIR" assembleRelease testDebugUnitTest --tests ai.gradium.phonon.tts.FocusedTest --tests ai.gradium.phonon.tts.ProgressTest --tests ai.gradium.phonon.tts.SettingsStateTest --tests ai.gradium.phonon.tts.ModelDeletionTest --tests ai.gradium.phonon.tts.InventoryTest --tests ai.gradium.phonon.tts.SelectionPolicyTest --tests ai.gradium.phonon.tts.VoiceCatalogPolicyTest --tests ai.gradium.phonon.tts.VoicePackStoreTest --console=plain
python3 - "$APP_DIR" "$ARTIFACT_DIR" <<'PY'
import json, hashlib, pathlib, shutil, struct, sys, zipfile
app, destination = map(pathlib.Path, sys.argv[1:])
apk = app / 'build/outputs/apk/release/phonon-tts-engine-release-unsigned.apk'
with zipfile.ZipFile(apk) as archive:
    for abi, machine in [('arm64-v8a',183),('x86_64',62)]:
        name = f'lib/{abi}/libptts_ffi.so'
        data = archive.read(name)
        assert len(data) > 100000 and data[:5] == b'\x7fELF\x02', f'Invalid native library: {name}'
        assert struct.unpack_from('<H',data,18)[0] == machine, f'Wrong architecture: {name}'
        assert archive.getinfo(f'lib/{abi}/libjnidispatch.so').file_size > 10000
    manifest_bytes = archive.read('assets/voices-manifest.json')
    manifest = json.loads(manifest_bytes)
    assert manifest_bytes == (app / 'src/main/assets/voices-manifest.json').read_bytes()
    assert len(manifest['voices']) == 102
    assert manifest['revision'] == '0ee17205f254b5abc43b266c8a0f215565c352f7'
    assert sum(v['bytes'] for v in manifest['voices']) == 26125872
    assert not any(n.endswith('.safetensors') for n in archive.namelist())
    assert archive.getinfo('assets/licenses/Voices-NOTICE.txt').file_size > 100
    assert archive.getinfo('assets/licenses/German-Voices-NOTICE.txt').file_size > 100
    for name in ['LICENSE-MIT' ,'LICENSE-APACHE','Sonic-LICENSE.txt','NOTICE.txt']:
        assert archive.getinfo(f'assets/licenses/{name}').file_size > 100
out = destination / 'Phonon-TTS-Community-v1.1.0-unsigned.apk'
shutil.copy2(apk,out)
sha = hashlib.sha256(out.read_bytes()).hexdigest()
checksum = out.with_name(out.name + '.sha256')
checksum.write_text(f'{sha}  {out.name}\n')
print('APK bytes:', out.stat().st_size)
print(out)
print(checksum.read_text(),end='')
PY

"${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/data/android/sdk}}/build-tools/36.0.0/zipalign" -c -P 16 4 "$ARTIFACT_DIR/Phonon-TTS-Community-v1.1.0-unsigned.apk"

python3 "$APP_DIR/verify-apk.py" "$ARTIFACT_DIR/Phonon-TTS-Community-v1.1.0-unsigned.apk"
