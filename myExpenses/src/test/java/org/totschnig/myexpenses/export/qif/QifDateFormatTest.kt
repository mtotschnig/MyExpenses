package org.totschnig.myexpenses.export.qif

import com.google.common.truth.Truth
import org.junit.Test
import java.util.*

class QifDateFormatTest {

    @Test
    fun testDefaultFormats() {
        Truth.assertThat(QifDateFormat.defaultForLocale(Locale.US)).isEqualTo(QifDateFormat.US)
        Truth.assertThat(QifDateFormat.defaultForLocale(Locale.GERMANY)).isEqualTo(QifDateFormat.EU)
        Truth.assertThat(QifDateFormat.defaultForLocale(Locale.CHINA)).isEqualTo(QifDateFormat.YMD)
    }
}