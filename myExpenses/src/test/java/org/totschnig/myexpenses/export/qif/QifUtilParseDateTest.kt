package org.totschnig.myexpenses.export.qif

import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.export.qif.QifUtils.parseDate
import java.text.SimpleDateFormat

@RunWith(JUnitParamsRunner::class)
class QifUtilParseDateTest {
    private val VERIFICATION_FORMAT = SimpleDateFormat("yyyy-MM-dd")
    private val VERIFICATION_FORMAT_WITH_TIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Test
    @Parameters(
        "6/21' 1",
        "6/21'01",
        "6/21'2001",
        "6/21/2001",
        "06/21/2001",
        "06/21/01",
        "06.21.01",
        "06.21.2001",
        "June 21 2001",
        "June 21\\, 2001",
    )
    fun shouldParseUs(dateString: String) {
        val date = parseDate(dateString, QifDateFormat.US)
        assertThat(VERIFICATION_FORMAT.format(date)).isEqualTo(VERIFICATION)
    }

    @Test
    @Parameters(
        "21/6' 1",
        "21/6'01",
        "21/6'2001",
        "21/6/2001",
        "21/06/2001",
        "21/06/01",
        "21.06.01",
        "21.06.2001",
        "21 June 2001",
        "21Jun2001",
        "21-Jun-2001",
        "Tuesday\\, 21 June 2001"

    )
    fun shouldParseEu(dateString: String) {
        val date = parseDate(dateString, QifDateFormat.EU)
        assertThat(VERIFICATION_FORMAT.format(date)).isEqualTo(VERIFICATION)
    }

    @Test
    @Parameters(
        "2001/6/21",
        "2001/06/21",
        "01/06/21",
        "2001 June 21",
        "20010621", // Pure digits (YMD)
        "\"2001-06-21\"" // Quoted CSV value
    )
    fun shouldParseYMD(dateString: String) {
        val date = parseDate(dateString, QifDateFormat.YMD)
        assertThat(VERIFICATION_FORMAT.format(date)).isEqualTo(VERIFICATION)
    }

    @Test
    @Parameters(
        "21.06.2001 14:58"
    )
    fun shouldParseWithTimeEU(dateTimeString: String) {
        val date = parseDate(dateTimeString, QifDateFormat.EU)
        assertThat(VERIFICATION_FORMAT_WITH_TIME.format(date)).isEqualTo(VERIFICATION_WITH_TIME)
    }

    @Test
    @Parameters(
        "2001-06-21T14:58:00"
    )
    fun shouldParseWithTimeYMD(dateTimeString: String) {
        val date = parseDate(dateTimeString, QifDateFormat.YMD)
        assertThat(VERIFICATION_FORMAT_WITH_TIME.format(date)).isEqualTo(VERIFICATION_WITH_TIME)
    }

    companion object {
        private const val VERIFICATION = "2001-06-21"
        private const val VERIFICATION_WITH_TIME = "2001-06-21 14:58:00"
    }
}
