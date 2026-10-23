package ai.gradium.phonon.tts

data class ModelFile(val name: String, val size: Long, val hash: String, val gitBlob: Boolean = false)
data class DownloadModel(val label: Int, val repo: String, val revision: String, val gated: Boolean, val files: List<ModelFile>) {
    val total get() = files.sumOf { it.size }
    val page get() = "https://huggingface.co/$repo"
}
object ModelCatalog {
    private fun files(size: Long, weight: String, voice: String, configSize: Long, config: String, tokenizerSize: Long, tokenizer: String, git: Boolean = false) = listOf(
        ModelFile("config.json", configSize, config, git), ModelFile("tokenizer.json", tokenizerSize, tokenizer, git),
        ModelFile("model.q8.gguf", size, weight), ModelFile("default-voice.safetensors", 256136, voice))
    val all = listOf(
        DownloadModel(R.string.english_free, "gradium/phonon-31m-en", "92741c5b07d085ff840e26fea8ce43110b87852f", false,
            files(64512352, "a7d9aad2cd6dc66473f24a08bc6d2939030ef4d751ef82349361fbef3272dbae", "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", 4634, "b5d0f88353638370d94035b7d73d7d6577c0554d78f8d7ecfd40490aae39ae85", 277967, "691a00068fa7b989f2f3e5ae5daa2d1a5bb319f2d5360deafcfb19a8bb6e7936")),
        DownloadModel(R.string.german, "gradium/phonon-40m-de", "79cfc7b47fcfc0b666657b981eb366415023a862", true,
            files(74553568, "f8680286808bccc1a158a4ba5a2a143b17542f84023f4b7ef9f26bd0e1d08cb4", "b645c79148a02f6cd2ef7a6b3658373166e19a9a1c49fb81408be5d79667e6cb", 4634, "1a2d919878f37c9bc0578ef19f9d19c29762cb24", 278111, "72c0dcacaa598164a35a3d5b9918865342dbb955", true)),
        DownloadModel(R.string.multilingual, "gradium/phonon-40m-multilingual", "1537ce46c64f42903e0d608b132ccb073dac46d2", true,
            files(74553568, "96fa067813244e0c6c5997b98ec2596b65ec8574b14916b48e97db056ff97e27", "dc537478eaadc1a5257fcda2e3501ab81e14c111a5d95a5838f350db2afd0f1a", 4674, "5ddd955511e851a1f8c744ac23b69eb6000565d5", 277749, "e2d7bebf27916234f26883ad8e55c5219d0ae148", true))
    )
}
