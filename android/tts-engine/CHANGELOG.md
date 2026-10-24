## 0.4.1-android.10 (versionCode 11)

- Shared, user-triggered public voice packs per language: 102 metadata entries; no embedding binaries in the APK. Pinned sizes and SHA-256, token-free download, cancellation, staging and rollback.
- Preserve existing German identities and files; filter catalog voices by language and actual availability. Refresh native handles and preview voice caches after installation.
- One centered, accessible model choice row for installed models and downloads; automatic language defaults on installed-model selection; all five UI locales.
- Native libraries, signing identity and permissions retained from .9. Device runtime unverified.

# Changelog

## 0.4.1-android.3 / 4
- Bundle 13 pinned, size/SHA-256 verified Apache-2.0 German voice embeddings.
- Install voices before native loading/validation without copying model weights;
  preserve default/imported voices and keep German pack restricted to German.
- Refresh picker on model readiness/change; remember default voice per language.
- Encrypt explicitly saved/download-entered HF token using AndroidKeyStore AES/GCM
  and atomic no-backup ciphertext storage. Add status, deletion and error handling.
- Preserve application ID, signing certificate, model data and disabled pitch.

## 0.4.1-android.2 / 3
- Disable pitch changes while retaining tempo controls.
