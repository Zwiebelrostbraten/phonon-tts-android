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
assert len(base) == 66 and 'prefer_language' not in base
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
