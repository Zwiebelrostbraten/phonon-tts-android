#!/usr/bin/env python3
"""Verify the current unsigned release, without keys, installation or a prior APK."""
from pathlib import Path
import hashlib
import os
import re
import struct
import subprocess
import sys
import zipfile


def check_elf(data, machine):
    assert data[:5] == b'\x7fELF\x02', 'Expected ELF64'
    assert struct.unpack_from('<H', data, 18)[0] == machine, 'Wrong ELF architecture'
    offset = struct.unpack_from('<Q', data, 32)[0]
    size, count = struct.unpack_from('<HH', data, 54)
    loads = 0
    for i in range(count):
        at = offset + i * size
        if struct.unpack_from('<I', data, at)[0] == 1:
            loads += 1
            assert struct.unpack_from('<Q', data, at + 48)[0] >= 16384, 'ELF LOAD alignment below 16 KiB'
    assert loads, 'No ELF LOAD segments'


def check_badging(badging):
    assert "package: name='io.github.zwiebelrostbraten.phonontts' versionCode='14' versionName='1.1.0'" in badging
    assert 'application-debuggable' not in badging
    assert "minSdkVersion:'24'" in badging and "targetSdkVersion:'36'" in badging
    locales = set(re.findall(r"'([^']+)'", re.search(r'^locales: (.+)$', badging, re.M).group(1)))
    assert locales == {'--_--', 'de', 'es', 'fr', 'pt'}, locales


def main(apk):
    sdk = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '')))
    build = sdk / 'build-tools/36.0.0'
    badging = subprocess.check_output([str(build / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
    check_badging(badging)
    with zipfile.ZipFile(apk) as archive:
        assert not any(n.upper().startswith('META-INF/') and n.upper().endswith(('.RSA', '.DSA', '.EC', '.SF')) for n in archive.namelist()), 'JAR signature present'
        assert not any(n.endswith(('.safetensors', '.gguf')) for n in archive.namelist()), 'Model/embedding bundled'
        for abi, machine in [('arm64-v8a', 183), ('x86_64', 62)]:
            for lib in ['libptts_ffi.so', 'libjnidispatch.so']:
                check_elf(archive.read(f'lib/{abi}/{lib}'), machine)
        # APK v2/v3 signing blocks must sit immediately before the central directory.
        with apk.open('rb') as source:
            source.seek(archive.start_dir - 16)
            assert source.read(16) != b'APK Sig Block 42', 'APK signing block present'
    result = subprocess.run([str(build / 'apksigner'), 'verify', str(apk)], capture_output=True, text=True)
    assert result.returncode != 0, 'APK is signed'
    assert 'DOES NOT VERIFY' in result.stdout + result.stderr, f'apksigner did not report a verification result: {result.stderr.strip()}'
    subprocess.run([str(build / 'zipalign'), '-c', '-P', '16', '4', str(apk)], check=True)
    print('PASS: unsigned, identity/version, SDK/locales, both CPU ABIs, all ELF LOAD and ZIP alignments, no weights')
    print('APK SHA-256:', hashlib.sha256(apk.read_bytes()).hexdigest())


if __name__ == '__main__':
    main(Path(sys.argv[1]))
