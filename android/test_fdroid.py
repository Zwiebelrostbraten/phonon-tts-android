"""F-Droid contracts; run in the fdroidserver tooling environment (PyYAML)."""
from pathlib import Path
import shlex
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]


class FdroidContract(unittest.TestCase):
    def setUp(self):
        self.recipe = yaml.safe_load((ROOT / 'fdroid/metadata/io.github.zwiebelrostbraten.phonontts.yml').read_text())
        self.build = self.recipe['Builds'][0]

    def test_distribution_license_option(self):
        self.assertEqual(self.recipe['License'], 'MIT')
        self.assertIn('MIT OR Apache-2.0', self.recipe['MaintainerNotes'])

    def test_canonical_sdk_package(self):
        sdk = next(shlex.split(c) for c in self.build['build'] if c.startswith('sdkmanager '))
        self.assertIn('platforms;android-37.0', sdk)
        self.assertNotIn('platforms;android-37', sdk)

    def test_installer_binary_is_pinned(self):
        commands = self.build['build']
        self.assertTrue(all(isinstance(command, str) for command in commands))
        installer = next(i for i, c in enumerate(commands) if c.startswith('$$rustup$$/rustup-init.sh'))
        self.assertIn('export RUSTUP_VERSION=1.28.2', commands[:installer])
        self.assertEqual(self.build['srclibs'], ['rustup@1.28.2'])
        self.assertEqual(str(self.build['ndk']), '27.2.12479018')

    def test_explicit_gradle_route_is_deliberate(self):
        self.assertTrue(self.build['output'].endswith('release-unsigned.apk'))
        self.assertNotIn('predates AGP', self.recipe['MaintainerNotes'])
        self.assertIn('selector supports wrapper 9.3.1 and AGP 9.1', self.recipe['MaintainerNotes'])


if __name__ == '__main__':
    unittest.main()
