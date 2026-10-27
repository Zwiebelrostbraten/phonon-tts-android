"""Android source/build contract; run with Python 3.11+ from any directory."""
import hashlib
from pathlib import Path
import tomllib
import unittest

ROOT = Path(__file__).resolve().parents[1]

class RepositoryContract(unittest.TestCase):
    def test_workspace_and_path_closure(self):
        root = tomllib.loads((ROOT / "Cargo.toml").read_text())
        self.assertEqual(set(root["workspace"]["members"]), {"ptts", "ptts-ffi"})
        for manifest in [ROOT / "Cargo.toml", ROOT / "ptts/Cargo.toml", ROOT / "ptts-ffi/Cargo.toml"]:
            def check(value):
                if isinstance(value, dict):
                    if "path" in value:
                        self.assertIn((manifest.parent / value["path"]).resolve(), {ROOT / "ptts", ROOT / "ptts-ffi"})
                    for child in value.values():
                        check(child)
                elif isinstance(value, list):
                    for child in value:
                        check(child)
            check(tomllib.loads(manifest.read_text()))

    def test_platform_sources_removed(self):
        for path in ["ios", "qnn", "ptts-coreml", "ptts-wasm", "ptts-pyo3", "ptts-openai-server", "ptts-ws-server", "Package.swift", "ptts/src/bin", "ptts/examples", ".github/workflows", "CLAUDE.md", ".spi.yml"]:
            with self.subTest(path=path):
                self.assertFalse((ROOT / path).exists())
        self.assertNotIn("target_vendor", (ROOT / "ptts-ffi/src/lib.rs").read_text())

    def test_critical_sources_and_abi(self):
        for path in ["LICENSE-MIT", "LICENSE-APACHE", "android/build.sh", "android/PhononTTS.kt", "android/tts-engine/gradlew", "android/tts-engine/gradle/wrapper/gradle-wrapper.jar", "ptts/tests/fixtures/config.json", "android/tts-engine/src/androidTest/java/ai/gradium/phonon/tts/EngineTest.kt", "android/tts-engine/src/main/assets/voices-manifest.json"]:
            self.assertTrue((ROOT / path).is_file(), path)
        self.assertEqual(hashlib.sha256((ROOT / "ptts-ffi/include/ptts.h").read_bytes()).hexdigest(), "df199f7a174e2eb935797a12d1f484f9cea097db421cc274c61749e63d7bb3a6")

if __name__ == "__main__":
    unittest.main()
