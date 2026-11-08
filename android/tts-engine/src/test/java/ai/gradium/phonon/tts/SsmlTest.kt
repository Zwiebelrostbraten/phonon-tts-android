package ai.gradium.phonon.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class SsmlTest {
    @Test fun plainTextKeepsOnlyOuterWhitespaceTrimmed() {
        assertEquals("Turn right onto Main Street.", Ssml.strip("  Turn right onto Main Street.  "))
    }

    @Test fun speakWrapperIsRemoved() {
        assertEquals("Turn left in 200 meters.", Ssml.strip("<speak>Turn left in 200 meters.</speak>"))
    }

    @Test fun selfClosingTagsBecomeSpaces() {
        assertEquals("Continue then turn right.", Ssml.strip("Continue<break time=\"700ms\"/>then turn right."))
    }

    @Test fun tagAttributesAndNamesAreDropped() {
        assertEquals("Drive safely.", Ssml.strip("<prosody rate=\"fast\" pitch=\"+2st\">Drive safely.</prosody>"))
    }

    @Test fun inlineMarkupKeepsWordsAndTidiesPunctuation() {
        assertEquals("This is important, okay?", Ssml.strip("This is <emphasis>important</emphasis>, okay?"))
    }

    @Test fun commentsAreRemoved() {
        assertEquals("Turn left now.", Ssml.strip("Turn left<!-- navigation pause -->now."))
    }

    @Test fun entitiesAreDecoded() {
        assertEquals("Speed & limit < 50>", Ssml.strip("Speed &amp; limit &lt; 50&gt;"))
        assertEquals("café", Ssml.strip("caf&#233;"))
    }

    @Test fun encodedTagsAreNotDoubleStripped() {
        assertEquals("Show <b> literally", Ssml.strip("Show &lt;b&gt; literally"))
    }

    @Test fun plainComparisonsAreNotMistakenForTags() {
        assertEquals("5 < 10 and 20 > 3", Ssml.strip("5 < 10 and 20 > 3"))
    }

    @Test fun blankInputStaysBlank() {
        assertEquals("", Ssml.strip(""))
        assertEquals("", Ssml.strip("   "))
    }
}
