package org.totschnig.myexpenses.ui

import android.text.InputFilter
import android.text.Spanned
import android.text.TextUtils
import timber.log.Timber

class FractionDigitsInputFilter(
    private val decimalSeparator: Char,
    private val otherSeparator: Char,
    private val fractionDigits: Int
) : InputFilter {

    private fun testInput(input: String) = when (input.count { it == decimalSeparator }) {
        0 -> true
        1 -> input.substringAfter(decimalSeparator).length <= fractionDigits
        else -> false
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence {
        val v = CharArray(end - start)
        TextUtils.getChars(source, start, end, v, 0)
        var input: String = String(v).replace(otherSeparator, decimalSeparator)
        val lastSeparatorIndex = input.lastIndexOf(decimalSeparator)
        input = if (fractionDigits == 0 && lastSeparatorIndex > -1)
            input.substring(0, lastSeparatorIndex).filter { it.isDigit() }
        else
            input.filterIndexed { index, c ->
                c.isDigit() || (c == decimalSeparator && index == lastSeparatorIndex)
            }
        val partBeforeInput = dest.substring(0, dstart)
        val partAfterInput = dest.substring(dend, dest.length)
        Timber.i("parts: %s, %s", partBeforeInput, partAfterInput)

        while (!testInput(partBeforeInput + input + partAfterInput) && input.isNotEmpty()) {
            input = input.substring(0, input.length - 1)
        }

        return input
    }
}