# Contributing

Contributions should support the Android system TTS app described in the [user guide](docs/user-guide.md). Explain the user-visible problem and keep changes focused. For larger changes, open an issue first so requirements can be discussed.

Bug reports should include Android/device/CPU and app versions, selected model and voice, reproducible steps and expected versus observed behavior. Use harmless sample text for speech or performance reports. Remove tokens, account details and private text from logs or images. See [security reporting](SECURITY.md) for vulnerabilities.

For code changes, follow the [development guide](docs/android-development.md), run checks appropriate to the affected behavior and report what was actually tested. Distinguish host tests from device checks. For translations, keep all five locale resources and format placeholders consistent. For documentation, check local links and keep the English user guide consistent with the app.

Preserve model access controls, download verification, token privacy and the distinct community app identity. Do not include gated weights, credentials, signing keys or generated APKs in a pull request. Preserve the shared CPU runtime, Android ABI and license notices.

Describe any dependency, license or model/voice attribution changes. Code uses MIT OR Apache-2.0; model and voice licenses remain separate. Submit only material you have the right to contribute under the applicable terms.
