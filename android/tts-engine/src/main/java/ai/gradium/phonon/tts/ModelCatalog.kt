package ai.gradium.phonon.tts

data class ModelFile(val name: String, val size: Long, val hash: String, val gitBlob: Boolean = false)
data class DownloadModel(val id: String, val languages: List<String>, val label: Int, val repo: String, val revision: String, val gated: Boolean, val files: List<ModelFile>) {
    val total get() = files.sumOf { it.size }
    val page get() = "https://huggingface.co/$repo"
}
object ModelCatalog {
    val all = listOf(
        DownloadModel("phonon-31m-en", listOf("en"), R.string.english_free, "gradium/phonon-31m-en", "92741c5b07d085ff840e26fea8ce43110b87852f", false, listOf(
            ModelFile("config.json", 4634, "11cfeb2513febe31358a8ff942093e0b2c28710a", true),
            ModelFile("tokenizer.json", 277967, "70d39980cb2154bd364dbed8424b1343923a03e2", true),
            ModelFile("model.q8.gguf", 64512352, "a7d9aad2cd6dc66473f24a08bc6d2939030ef4d751ef82349361fbef3272dbae", false),
            ModelFile("default-voice.safetensors", 256136, "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", false),
        )),
        DownloadModel("phonon-40m-en", listOf("en"), R.string.english_40m, "gradium/phonon-40m-en", "cc738b7e4d182c2ba1bafb8062aa4846af000637", true, listOf(
            ModelFile("config.json", 4634, "f6852aa041b306de1f34bbe6dc38f17073cc332e", true),
            ModelFile("tokenizer.json", 277967, "70d39980cb2154bd364dbed8424b1343923a03e2", true),
            ModelFile("model.q8.gguf", 74553568, "f6acc8f43cb296be31341b4428a6f01b6402659cd0039b624c4aea224936cf95", false),
            ModelFile("default-voice.safetensors", 256136, "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", false),
        )),
        DownloadModel("phonon-40m-de", listOf("de"), R.string.german, "gradium/phonon-40m-de", "79cfc7b47fcfc0b666657b981eb366415023a862", true, listOf(
            ModelFile("config.json", 4634, "1a2d919878f37c9bc0578ef19f9d19c29762cb24", true),
            ModelFile("tokenizer.json", 278111, "72c0dcacaa598164a35a3d5b9918865342dbb955", true),
            ModelFile("model.q8.gguf", 74553568, "f8680286808bccc1a158a4ba5a2a143b17542f84023f4b7ef9f26bd0e1d08cb4", false),
            ModelFile("default-voice.safetensors", 256136, "b645c79148a02f6cd2ef7a6b3658373166e19a9a1c49fb81408be5d79667e6cb", false),
        )),
        DownloadModel("phonon-40m-fr", listOf("fr"), R.string.french, "gradium/phonon-40m-fr", "d91086970206b88efd2c88ca971d0628b5968f0f", true, listOf(
            ModelFile("config.json", 4634, "0e6e5d28c65e6617e31d245941a181b3b24b6ca4", true),
            ModelFile("tokenizer.json", 277749, "e2d7bebf27916234f26883ad8e55c5219d0ae148", true),
            ModelFile("model.q8.gguf", 74553568, "e56a68900c0b3883a81e66f2506167f2e48ba7bdd3ce36b5195e24009d484dcb", false),
            ModelFile("default-voice.safetensors", 256136, "a8c97074fdb4044fc8da2873bdccd8b2a64beeedab859d241d5fc11c0c400aad", false),
        )),
        DownloadModel("phonon-40m-es", listOf("es"), R.string.spanish, "gradium/phonon-40m-es", "f0d2f39c9b58ad4c19a189e3ba989ee4df393cc5", true, listOf(
            ModelFile("config.json", 4634, "eb971fe8e5150119e927209dcbba5a7124f58a68", true),
            ModelFile("tokenizer.json", 278837, "731a051ef2b816bf148443f6f18056fecbdafad7", true),
            ModelFile("model.q8.gguf", 74553568, "b90d5897e5e663c56a8975a8b2d6f7db557b9bad59ad82a82361ac56614c2a5c", false),
            ModelFile("default-voice.safetensors", 256136, "25c6df91a261bcb96d9e5152c9b0988398859c0e6f4ee5f08dd577fd3bd0300d", false),
        )),
        DownloadModel("phonon-40m-pt", listOf("pt"), R.string.portuguese, "gradium/phonon-40m-pt", "7e7c4f7ba700f466641fbda9d9c048e3aee0c8f2", true, listOf(
            ModelFile("config.json", 4634, "b45c9e49061f36c362e69aaf94fbd3be8bc5e63c", true),
            ModelFile("tokenizer.json", 279034, "d24831c796c012ecb6f013e0d4d5292b23c20d40", true),
            ModelFile("model.q8.gguf", 74553568, "e1ab3d6a1a6835ab73ac1fc1b326c9a8238292c101d2050296413ab30dba7681", false),
            ModelFile("default-voice.safetensors", 256136, "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", false),
        )),
        DownloadModel("phonon-40m-multilingual", listOf("en","de","fr","es","pt"), R.string.multilingual, "gradium/phonon-40m-multilingual", "1537ce46c64f42903e0d608b132ccb073dac46d2", true, listOf(
            ModelFile("config.json", 4674, "5ddd955511e851a1f8c744ac23b69eb6000565d5", true),
            ModelFile("tokenizer.json", 277749, "e2d7bebf27916234f26883ad8e55c5219d0ae148", true),
            ModelFile("model.q8.gguf", 74553568, "96fa067813244e0c6c5997b98ec2596b65ec8574b14916b48e97db056ff97e27", false),
            ModelFile("default-voice.safetensors", 256136, "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", false),
        )),
    )
}
