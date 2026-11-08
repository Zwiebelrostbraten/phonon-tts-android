package ai.gradium.phonon.tts

/**
 * Some callers (TomTom is a known one) hand raw SSML markup to the system text-to-speech engine
 * instead of the plain text inside it. The Phonon models were trained on ordinary prose, so without
 * this step they read tag names and attributes aloud: "less than break time equals 700
 * milliseconds slash greater than".
 *
 * [strip] removes that markup while keeping the words it wraps, so `<speak>Turn left</speak>`
 * becomes `Turn left` and `<break time="700ms"/>` becomes a space.
 */
object Ssml {
    // Markup must look like a tag: `<`, an optional `/`, `!` or `?`, then a letter. This keeps
    // ordinary comparisons such as `5 < 10 > 3` intact instead of eating a chunk of the sentence.
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val DECLARATION = Regex("<[!?][^>]*>")
    private val TAG = Regex("</?[a-zA-Z][^>]*>")
    private val ENTITY = Regex("&#?[a-zA-Z0-9]+;")
    private val HORIZONTAL = Regex("[ \\t\\u000B\\f]+")

    // A closing inline tag (`</emphasis>,`) would otherwise leave a space before the punctuation.
    private val SPACE_BEFORE_PUNCTUATION = Regex(" +([,.!?;:])")

    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    )

    fun strip(text: String): String {
        // Fast path: nothing that could be markup, so callers that already send plain text keep
        // their exact offsets and whitespace apart from the surrounding trim.
        if (text.indexOf('<') < 0 && text.indexOf('&') < 0) return text.trim()

        var result = COMMENT.replace(text, " ")
        result = DECLARATION.replace(result, " ")
        // Tags become a space so `<b>left</b>right` keeps the word boundary: "left right".
        result = TAG.replace(result, " ")
        // Entities are decoded after tags are gone so `&lt;b&gt;` stays literal text.
        result = ENTITY.replace(result) { decode(it.value) }
        result = HORIZONTAL.replace(result, " ")
        result = SPACE_BEFORE_PUNCTUATION.replace(result, "$1")
        return result.trim()
    }

    private fun decode(entity: String): String {
        val body = entity.substring(1, entity.length - 1)
        if (body.startsWith("#")) {
            val code = runCatching {
                if (body.startsWith("#x", ignoreCase = true)) body.substring(2).toInt(16)
                else body.substring(1).toInt()
            }.getOrNull() ?: return entity
            return if (Character.isValidCodePoint(code)) String(Character.toChars(code)) else entity
        }
        return NAMED[body.lowercase()] ?: entity
    }
}
