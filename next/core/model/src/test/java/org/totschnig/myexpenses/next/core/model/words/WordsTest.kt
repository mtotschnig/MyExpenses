package org.totschnig.myexpenses.next.core.model.words

import org.junit.Assert.assertEquals
import org.junit.Test

class WordsTest {

    /** What [Words.normalize] did before it was written out */
    private fun byRegex(text: String) = text.lowercase().replace(Regex("[^\\p{L}\\p{N}+]+"), " ").trim()

    @Test
    fun normalizesLikeTheRegex() {
        listOf(
            "Kfz-Haftpflicht",
            "  REWE Markt GmbH, Filiale 4711  ",
            "Apple Music/iCloud+ 2,99€",
            "Café Müller & Söhne",
            "Straße ½ ² Ⅻ",
            "PayPal (Europe) S.à r.l. et Cie, S.C.A.",
            "Tab\tand\nnewline",
            "Emoji 😀 between",
            "Ünïcödé ÄÖÜ ẞ",
            "---",
            ""
        ).forEach { assertEquals(it, byRegex(it), Words.normalize(it)) }
    }

    @Test
    fun findsPatternsAtTheStartOfWords() {
        val text = Words.textOf("Kfz-Haftpflicht", null, "Apple Music")
        assertEquals(" kfz haftpflicht apple music ", text)
        assertEquals(true, Words.startsWord(text, "Haftpflicht"))
        assertEquals(true, Words.startsWord(text, "apple mus"))
        assertEquals(false, Words.startsWord(text, "pflicht"))
        assertEquals(true, Words.isWord(text, "kfz"))
        assertEquals(false, Words.isWord(text, "app"))
        assertEquals(true, Words.contains(text, "pflicht"))
    }
}
