package ai.gradium.phonon.tts

/** Only an exact, unique catalog fingerprint selects a non-default download. */
internal fun initialModelSelection(matches: List<Boolean>): Int = matches.indices.filter { matches[it] }.singleOrNull() ?: 0
