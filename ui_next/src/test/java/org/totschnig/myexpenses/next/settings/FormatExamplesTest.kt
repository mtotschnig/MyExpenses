package org.totschnig.myexpenses.next.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class FormatExamplesTest {

    private val date = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1) }.time

    @Test
    fun showsADateInAPattern() {
        assertEquals("01.10.2026", dateExample("dd.MM.yyyy", Locale.GERMANY, date))
        assertEquals("2026-10-01", dateExample("yyyy-MM-dd", Locale.GERMANY, date))
    }

    @Test
    fun hasNoExampleForAnInvalidPattern() {
        assertNull(dateExample("dd.MM.yyyy'", Locale.GERMANY, date))
        assertNull(numberExample("#,##0.00.0", "EUR", "€"))
    }

    @Test
    fun showsAnAmountInAPatternOfTheLocale() {
        val default = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            assertEquals("1.234,56 €", numberExample(localizedNumberPattern("#,##0.00 ¤"), "EUR", "€"))
            assertEquals("€ 1.234,56", numberExample(localizedNumberPattern("¤ #,##0.00"), "EUR", "€"))
            assertEquals("1234,56 €", numberExample(localizedNumberPattern("#0.00 ¤"), "EUR", "€"))
            assertEquals("1.234,56 EUR", numberExample(localizedNumberPattern("#,##0.00 ¤¤"), "EUR", "€"))
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun putsTheFormatOfTheRegionFirstAndLeavesOutPatternsThatLookTheSame() {
        val options = formatOptions(
            regionPattern = "dd.MM.yy",
            patterns = listOf("dd.MM.yy", "dd.MM.yyyy", "d.M.yyyy"),
            example = { dateExample(it, Locale.GERMANY, date) },
            region = { "$it (region)" }
        )
        assertEquals(
            listOf("dd.MM.yy" to "01.10.26 (region)", "dd.MM.yyyy" to "01.10.2026", "d.M.yyyy" to "1.10.2026"),
            options
        )
    }
}
