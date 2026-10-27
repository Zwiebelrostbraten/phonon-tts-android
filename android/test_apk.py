"""Focused ELF alignment verifier regression tests, using synthetic ELF headers."""
import importlib.util
from pathlib import Path
import struct
import unittest
spec = importlib.util.spec_from_file_location('verify_apk', Path(__file__).parent / 'tts-engine/verify-apk.py')
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)

class ElfAlignment(unittest.TestCase):
    def test_sdk_badging_uses_min_sdk_field(self):
        badging = """package: name='io.github.zwiebelrostbraten.phonontts' versionCode='14' versionName='1.1.0'
minSdkVersion:'24'
targetSdkVersion:'36'
locales: '--_--' 'de' 'es' 'fr' 'pt'
"""
        verify.check_badging(badging)
        with self.assertRaises(AssertionError):
            verify.check_badging(badging.replace("minSdkVersion:'24'", "minSdkVersion:'26'"))

    def test_rejects_4k_load_alignment(self):
        data = bytearray(120)
        data[:5] = b'\x7fELF\x02'
        struct.pack_into('<H', data, 18, 183)
        struct.pack_into('<Q', data, 32, 64)
        struct.pack_into('<HH', data, 54, 56, 1)
        struct.pack_into('<I', data, 64, 1)
        struct.pack_into('<Q', data, 112, 4096)
        with self.assertRaises(AssertionError):
            verify.check_elf(data, 183)
        struct.pack_into('<Q', data, 112, 16384)
        verify.check_elf(data, 183)

if __name__ == '__main__':
    unittest.main()
