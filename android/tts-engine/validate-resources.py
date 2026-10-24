#!/usr/bin/env python3
"""Focused static release checks: locale parity and authoritative pinned CPU catalog."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

app = Path(__file__).resolve().parent
res = app / 'src/main/res'
locales = ['en', 'de', 'fr', 'es', 'pt']
sets = {}
for lang in locales:
    path = res / ('values' if lang == 'en' else f'values-{lang}') / 'strings.xml'
    elements = ET.parse(path).getroot().findall('string')
    strings = {e.attrib['name']: ''.join(e.itertext()) for e in elements}
    assert len(strings) == len(elements), f'Duplicate key in {lang}'
    sets[lang] = strings
base = sets['en']
assert len(base) == 74 and 'prefer_language' not in base
pattern = r'%(?:\d+\$)?[\d.]*[a-zA-Z%]'
for lang, strings in sets.items():
    assert strings.keys() == base.keys(), f'Key mismatch: {lang}'
    for key, text in strings.items():
        assert sorted(re.findall(pattern, text)) == sorted(re.findall(pattern, base[key])), (lang, key)
        assert text.strip(), (lang, key, 'empty translation')
assert sorted(p.name for p in res.glob('values-*/strings.xml')) == ['strings.xml'] * 4
config = ET.parse(res / 'xml/locales_config.xml').getroot()
assert [e.attrib['{http://schemas.android.com/apk/res/android}name'] for e in config] == locales
source = (app / 'src/main/java/ai/gradium/phonon/tts/ModelCatalog.kt').read_text()
pins = json.loads((app / 'src/test/resources/catalog-pins.json').read_text())
assert source.count('        DownloadModel(') == len(pins) == 7
for model in pins:
    assert f'"{model["id"]}"' in source
    assert f'"gradium/{model["id"]}"' in source
    assert f'"{model["sha"]}"' in source
    for f in model['files']:
        line = f'ModelFile("{f["name"]}", {f["size"]}, "{f["hash"]}", {str(f["gitBlob"]).lower()})'
        assert line in source, (model['id'], f['name'])
print(f'PASS: {len(base)} strings × 5 locales; format parity; localeConfig; 7 pinned Q8 catalogs with exact sizes and hashes')

# All public assets are pinned, hash-verified, unique, and represented exactly once.
import hashlib
from collections import Counter
assets = app / 'src/main/assets'
manifest = json.loads((assets / 'voices-manifest.json').read_text())
voices = manifest['voices']
assert manifest['revision'] == '0ee17205f254b5abc43b266c8a0f215565c352f7'
assert Counter(v['language'] for v in voices) == dict(en=37, es=25, de=13, fr=15, pt=12)
assert len({v['name'] for v in voices}) == len({v['asset'] for v in voices}) == 102
assert sum(v['bytes'] for v in voices) == 26125872
for voice in voices:
    assert re.fullmatch(r'voices/[A-Za-z0-9_-]+\.safetensors', voice['filename'])
    assert re.fullmatch(r'[a-f0-9]{64}', voice['sha256'])
legacy = json.loads((assets / 'german-voices/manifest.json').read_text())
for voice in legacy['voices']:
    current = next(v for v in voices if v['name'] == voice['name'])
    assert current['sha256'] == voice['sha256'] and current['bytes'] == voice['bytes']
assert not list(assets.rglob('*.safetensors'))
ui = (app / 'src/main/java/ai/gradium/phonon/tts/SettingsScreen.kt').read_text()
assert ui.count('ModelChoiceRow(') == 3  # two callers, one shared implementation
row = ui[ui.index('private fun ModelChoiceRow'):]
assert 'verticalAlignment = Alignment.CenterVertically' in row
assert 'Alignment.Top' not in row and 'onClick = null' in row
assert ui.count('.selectableGroup()') == 2
print('PASS: 102 pinned public voice metadata entries, no embedding assets, 5 language counts, original German identities; shared centered model row')

assert 'VoicePacks.download' not in ui
activity = (app / 'src/main/java/ai/gradium/phonon/tts/SettingsActivity.kt').read_text()
assert activity.count('VoicePacks.download(') == 1
assert activity.index('downloadPack = {') < activity.index('VoicePacks.download(') < activity.index('selectInstalled = {')
assert 'shownVoiceGeneration != VoicePacks.generation' in activity
assert 'preview?.shutdown()' in activity
print('PASS: voice downloads only in explicit action; preview refresh uses a generation guard')
