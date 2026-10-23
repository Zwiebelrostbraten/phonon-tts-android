#!/bin/sh
# Manual-only packaging. Never publish the app-only intermediate APK without the CPU libraries.
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(CDPATH= cd -- "$APP_DIR/../.." && pwd)
ARTIFACT_DIR=$(dirname -- "$REPO_DIR")
for abi in arm64-v8a x86_64; do
    test -s "$REPO_DIR/android/jniLibs/$abi/libptts_ffi.so" || {
        echo "Missing real native CPU library for $abi; finish android/build.sh first." >&2
        exit 1
    }
done
"$APP_DIR/gradlew" -p "$APP_DIR" assembleDebug testDebugUnitTest --tests ai.gradium.phonon.tts.FocusedTest --console=plain
python3 - "$APP_DIR" "$ARTIFACT_DIR" <<'PY'
import json, hashlib, pathlib, shutil, struct, sys, zipfile
app, destination = map(pathlib.Path, sys.argv[1:])
apk = app / 'build/outputs/apk/debug/phonon-tts-engine-debug.apk'
with zipfile.ZipFile(apk) as archive:
    for abi, machine in [('arm64-v8a',183),('x86_64',62)]:
        name = f'lib/{abi}/libptts_ffi.so'
        data = archive.read(name)
        assert len(data) > 100000 and data[:5] == b'\x7fELF\x02', f'Invalid native library: {name}'
        assert struct.unpack_from('<H',data,18)[0] == machine, f'Wrong architecture: {name}'
        assert archive.getinfo(f'lib/{abi}/libjnidispatch.so').file_size > 10000
    manifest_bytes = archive.read('assets/german-voices/manifest.json')
    manifest = json.loads(manifest_bytes)
    assert manifest_bytes == (app / 'src/main/assets/german-voices/manifest.json').read_bytes()
    assert len(manifest['voices']) == 13
    assert manifest['revision'] == '0ee17205f254b5abc43b266c8a0f215565c352f7'
    for voice in manifest['voices']:
        data = archive.read(f"assets/german-voices/{voice['name']}.safetensors")
        assert len(data) == voice['bytes'] and hashlib.sha256(data).hexdigest() == voice['sha256']
    assert sum(v['bytes'] for v in manifest['voices']) == 3329768
    assert archive.getinfo('assets/licenses/German-Voices-NOTICE.txt').file_size > 100
    for name in ['LICENSE-MIT' ,'LICENSE-APACHE','Sonic-LICENSE.txt','NOTICE.txt']:
        assert archive.getinfo(f'assets/licenses/{name}').file_size > 100
out = destination / 'Phonon-TTS-v0.4.1-android.3-debug.apk'
shutil.copy2(apk,out)
sha = hashlib.sha256(out.read_bytes()).hexdigest()
checksum = out.with_name(out.name + '.sha256')
checksum.write_text(f'{sha}  {out.name}\n')
print(out)
print(checksum.read_text(),end='')
PY
