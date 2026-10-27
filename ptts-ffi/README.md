# Android CPU C interface

[`include/ptts.h`](include/ptts.h) defines the ABI consumed by the
[shared Kotlin/JNA wrapper](../android/PhononTTS.kt). Build both Android ABIs with
[`android/build.sh`](../android/build.sh); see [development](../docs/android-development.md).

`ptts_new` loads a checkpoint directory containing `config.json`, `tokenizer.json`,
weights (`model.q8.gguf` or `model.safetensors`) and optional voice embeddings.
Use `PTTS_UNIT_CPU`; the ANE constant remains reserved for ABI compatibility and is
rejected by the CPU engine. Model acquisition is handled by the Android app.

Keep a handle open across utterances and serialize calls off the UI thread.
`ptts_speak` streams 24 kHz mono float PCM to its callback; returning false cancels.
The callback runs on the calling thread and must not reenter the handle.
`ptts_last_error` reports failures; strings returned by the library are borrowed.
`ptts_free` releases the handle. Voice/language getters and setters are declared in
the header. JNA must map C bool as one byte and size_t as pointer-sized.

The CPU module and exported structs/functions are shared runtime code. Changes to
signatures or layouts must preserve the header and Kotlin mappings together.
