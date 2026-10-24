#!/usr/bin/env python3
"""Compare a packaged update with the previous release, without installing either APK."""
from pathlib import Path
import hashlib
import os
import re
import subprocess
import sys
import zipfile

new, previous = map(Path, sys.argv[1:])
sdk = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '/opt/data/android/sdk')))
if os.environ.get('JAVA_HOME'):
    os.environ['PATH'] = str(Path(os.environ['JAVA_HOME']) / 'bin') + os.pathsep + os.environ.get('PATH', '')
build = sorted((sdk / 'build-tools').iterdir())[-1]
def run(tool, *args):
    return subprocess.check_output([str(build / tool), *map(str, args)], text=True)
def cert(apk):
    report = run('apksigner', 'verify', '--print-certs', apk)
    return re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', report).group(1)
def manifest(apk):
    report = run('aapt2', 'dump', 'badging', apk)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", report).groups()
    permissions = sorted(re.findall(r"uses-permission: name='([^']+)'", report))
    return package, permissions, report
assert cert(new) == cert(previous), 'Certificate changed'
new_info, new_permissions, badging = manifest(new)
old_info, old_permissions, old_badging = manifest(previous)
assert new_info == ('ai.gradium.phonon.tts', '13', '1.0.0'), new_info
assert cert(new) == '63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0'
assert 'application-debuggable' not in badging, 'Debuggable release'
assert int(new_info[1]) > int(old_info[1]), (new_info, old_info)
assert new_info[0] == old_info[0]
assert new_permissions == old_permissions, (new_permissions, old_permissions)
assert re.findall(r'^uses-feature[^\n]*', badging, re.M) == re.findall(r'^uses-feature[^\n]*', old_badging, re.M), 'Changed features'
assert set(re.findall(r"'([^']+)'", re.search(r"^locales: (.+)$", badging, re.M).group(1))) == {"--_--", "de", "es", "fr", "pt"}, "Unexpected packaged locales"
assert "minSdkVersion:'24'" in badging and "targetSdkVersion:'36'" in badging
with zipfile.ZipFile(new) as current, zipfile.ZipFile(previous) as old:
    names = [name for name in old.namelist() if name.endswith('.so') or name.startswith('assets/')]
    assert not any(n.endswith('.safetensors') for n in current.namelist())
    assert {n for n in current.namelist() if n.endswith('.so') or n.startswith('assets/')} == set(names), 'Changed native/asset inventory'
    for name in names:
        assert current.read(name) == old.read(name), f'Changed native/voice asset: {name}'
print('PASS: application ID, explicit stable version and increasing code, certificate, permissions, SDK levels; all native libraries and all packaged assets byte-identical; zero embedding binaries')
print('Certificate SHA-256:', cert(new))
print('Size:', new.stat().st_size)
print('APK SHA-256:', hashlib.sha256(new.read_bytes()).hexdigest())
