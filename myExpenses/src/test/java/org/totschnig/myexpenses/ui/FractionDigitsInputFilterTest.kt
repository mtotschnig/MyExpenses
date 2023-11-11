package org.totschnig.myexpenses.ui

import android.text.SpannedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FractionDigitsInputFilterTest {

    private fun buildFilter(fractionDigits: Int = 2) = FractionDigitsInputFilter('.',',', fractionDigits)

    private fun FractionDigitsInputFilter.filter(source: String, dest: String, dstart: Int, dEnd: Int) =
        filter(source, 0, source.length, SpannedString(dest), dstart, dEnd)

    @Test
    fun shouldNotAcceptSeparatorsWithoutFractionDigits() {
        val filter = buildFilter(0)
        assertThat(filter.filter("112.3", "", 0,0)).isEqualTo("112")
    }

    @Test
    fun shouldAllowClearingOfData() {
        assertThat(buildFilter(0).filter("", "", 0, 0)).isEqualTo("")
        assertThat(buildFilter(2).filter("", "", 0, 0)).isEqualTo("")
    }

    @Test
    fun shouldAcceptSeparatorLessInputWithoutFractionDigits() {
        val filter = buildFilter(0)
        assertThat(filter.filter("112", "", 0,0)).isEqualTo("112")
    }

    @Test
    fun shouldKeepLastSeparator() {
        val filter = buildFilter()
        assertThat(filter.filter("5,912.00", "", 0,0)).isEqualTo("5912.00")
    }

    @Test
    fun allowValidInputIfAllIsSelected() {
        val filter = buildFilter()
        assertThat(filter.filter(".", "123", 0,3)).isEqualTo(".")
    }

    @Test
    fun shouldReplaceSeparator() {
        val filter = buildFilter()
        assertThat(filter.filter(",", "", 0,0)).isEqualTo(".")
    }

    @Test
    fun shouldAllowSeparatorOnlyOnValidPositions() {
        val filter = buildFilter()
        fun helper(dstart: Int, dEnd: Int, expected: Boolean) {
            assertThat(filter.filter(".", 0, 1, SpannedString("5912"), dstart,dEnd))
                .isEqualTo(if (expected) "." else "")
        }
        helper(0, 0, false)
        helper(0, 1, false)
        helper(0, 2, true)
        helper(0, 3, true)
        helper(0, 4, true)
        helper(1, 1, false)
        helper(1, 2, true)
        helper(1, 3, true)
        helper(1, 4, true)
        helper(2, 2, true)
        helper(2, 3, true)
        helper(2, 4, true)
        helper(3, 3, true)
        helper(3, 4, true)
        helper(4, 4, true)
    }
}