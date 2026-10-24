# Update 11 validation

0.4.1-android.11 / versionCode 12. Minified release, established Android Debug signer. No commit, push or publication.

- Selected JVM tests: 46 passed, 1 opt-in live test skipped, no failures/errors. Existing 36 passing tests plus ten deletion tests.
- Production Inventory filesystem removal and SelectionPolicy tests cover cancellation gate, other-model/shared-pack/token sentinels, legacy transaction artifacts, last model, locale fallback, unrelated defaults, invalid identities, stale path checks, symlink rejection and interrupted deletion recovery prevention.
- Deletion confirmation uses the Android Cancel label. Operation captures its failure message; deletion failures warn that cleanup may not have fully freed storage in all five locales without claiming a tombstoned target is retained. Other operation/cancellation messages are unchanged.
- All 74 strings match across five locales; seven pinned catalogs; 102 lazy public voices; zero embedding binaries.
- package-preview.sh completed assembleRelease, selected tests and required lintVital. verify-update.py compared against .10: identical app ID, certificate, permissions, SDK levels, native bytes and German identity manifest. zipalign 16 KiB check passed.
- Artifact: /opt/data/projects/Phonon-TTS-v0.4.1-android.11.apk, 18,055,445 bytes.
- SHA-256: `81288bed6b0caf37f2c0edbbb0c4efab880d5c71acb60d354c20a2cd4c0fcd72`
- Certificate SHA-256: `63e64d670aced1303b1b8cabbdfe879e99c7c9396891c2662970cc3c82d856d0`.
- Final build/validation log: `/opt/data/profiles/mrs_patmore/cache/scratch/phonon-delete/build-final.log`. Earlier build log is retained in the same directory.

Limitations: no device/emulator, native handle-close/synthesis concurrency runtime test or Compose accessibility interaction test. Preference tests exercise production selection policy snapshots, not Android SharedPreferences. Filesystem cleanup failure after marking retains a deletion marker, reports failure and prevents rediscovery; it never restores partially deleted weights. The original eight deletion tests were added alongside implementation. The new legitimate ancestor-symlink regression was demonstrated RED before the fix (root-alias-red.log), then GREEN (root-alias-green.log); the final suite also covers symlinked model/models escapes through an aliased root to a sibling with a shared path prefix. Path checks use canonical-root anchoring and exact expected relative paths, not prefix containment. Existing .1–.10 APKs were retained.
