package org.totschnig.myexpenses.next.core.model.words

import java.util.concurrent.ConcurrentHashMap

/**
 * Finds words in texts of transactions, e.g. payee and purpose, independent of case and punctuation
 */
object Words {
    /**
     * The patterns searched for are constants of the features, each is normalized only once.
     * The analyses check every transaction against many of them.
     */
    private val patterns = ConcurrentHashMap<String, String>()

    private fun pattern(pattern: String) = patterns.getOrPut(pattern) { normalize(pattern) }

    /**
     * Words separated by single spaces, e.g. "Kfz-Haftpflicht" becomes "kfz haftpflicht". Letters,
     * numbers and "+" are kept, like the regex `[^\\p{L}\\p{N}+]+` replaced by a space would. Written
     * out, since the analyses call it for every transaction and a regex takes long on Android.
     */
    fun normalize(text: String): String {
        val lower = text.lowercase()
        val result = StringBuilder(lower.length)
        var pendingSpace = false
        var i = 0
        while (i < lower.length) {
            val codePoint = lower.codePointAt(i)
            if (isWordChar(codePoint)) {
                if (pendingSpace && result.isNotEmpty()) result.append(' ')
                pendingSpace = false
                result.appendCodePoint(codePoint)
            } else {
                pendingSpace = true
            }
            i += Character.charCount(codePoint)
        }
        return result.toString()
    }

    private fun isWordChar(codePoint: Int) = codePoint == '+'.code || Character.isLetter(codePoint) ||
            when (Character.getType(codePoint).toByte()) {
                Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER -> true
                else -> false
            }

    /** The normalized [parts] with a space at start and end, as [startsWord] and [isWord] expect */
    fun textOf(vararg parts: String?) = " " + parts.filterNotNull().joinToString(" ") { normalize(it) } + " "

    /** [pattern] starts a word of [text], it may span several words, e.g. "apple music" */
    fun startsWord(text: String, pattern: String) = text.contains(" ${pattern(pattern)}")

    /** [word] is a whole word of [text], for short ones like "gas", which starts "Gastronomie" */
    fun isWord(text: String, word: String) = text.contains(" ${pattern(word)} ")

    /** Where [pattern] starts a word of [text] first, -1 if it does not, see [startsWord] */
    fun indexOfWordStart(text: String, pattern: String) = text.indexOf(" ${pattern(pattern)}")

    /** Where [word] is a whole word of [text] first, -1 if it is not, see [isWord] */
    fun indexOfWord(text: String, word: String) = text.indexOf(" ${pattern(word)} ")

    /** [pattern] appears anywhere in [text], also within a word */
    fun contains(text: String, pattern: String) = text.contains(pattern(pattern))
}
