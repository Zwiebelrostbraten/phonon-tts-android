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
