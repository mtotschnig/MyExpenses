package org.totschnig.myexpenses.io

import com.google.common.truth.Truth.assertThat
import org.apache.commons.csv.CSVFormat
import org.junit.Test
import org.mockito.kotlin.mock
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DateTime
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Date

class CsvParserTest {
    
    private val referenceDate: Date
        get() = DateTime.date(2023, 9, 6).atMidnight().asDate()

    @Test
    fun shouldParseSimple() {
        val result = parseCSV(
            fileContent = """
                         "","9/6/23","Peter","0","1.00","Food:Baker","Notiz"
                        """.trimIndent()
        )
        assertThat(result).hasSize(1)
        val account = result[0]
        assertThat(account.transactions).hasSize(1)
        with(account.transactions[0]) {
            assertThat(date).isEqualTo(referenceDate)
            assertThat(payee).isEqualTo("Peter")
            assertThat(amount).isEqualToIgnoringScale(-1L)
            assertThat(category).isEqualTo("Food:Baker")
            assertThat(memo).isEqualTo("Notiz")
        }
    }

    @Test
    fun shouldParseTransfer() {
        val result = parseCSV("""
            "","9/6/23","","0","122.00","[Budget Book]"
        """.trimIndent())
        assertThat(result).hasSize(1)
        val account = result[0]
        assertThat(account.transactions).hasSize(1)
        with(account.transactions[0]) {
            assertThat(date).isEqualTo(referenceDate)
            assertThat(amount).isEqualToIgnoringScale(-122L)
            assertThat(toAccount).isEqualTo("Budget Book")
        }
    }

    @Test
    fun shouldParseSplitTransaction() {
        val result = parseCSV("""
            "*","9/6/23","","0","100.00",""
            "-","9/6/23","","0","50.00","Salary:Leave allowance"
            "-","9/6/23","","0","50.00","[Budget Book]"

        """.trimIndent())
        assertThat(result).hasSize(1)
        val account = result[0]
        assertThat(account.transactions).hasSize(1)
        val transaction = account.transactions[0]
        with(transaction) {
            assertThat(date).isEqualTo(referenceDate)
            assertThat(splits).hasSize(2)
            assertThat(amount).isEqualToIgnoringScale(-100L)
        }
        with(transaction.splits!![0]) {
            assertThat(date).isEqualTo(referenceDate)
            assertThat(amount).isEqualToIgnoringScale(-50L)
            assertThat(category).isEqualTo("Salary:Leave allowance")
        }
        with(transaction.splits[1]) {
            assertThat(date).isEqualTo(referenceDate)
            assertThat(amount).isEqualToIgnoringScale(-50L)
            assertThat(toAccount).isEqualTo("Budget Book")
        }
    }


    private fun parseCSV(
        fileContent: String,
        dateFormat: QifDateFormat = QifDateFormat.US,
        columnToFieldMap: IntArray = intArrayOf(
            R.string.split_transaction,
            R.string.date,
            R.string.payer_or_payee,
            R.string.income,
            R.string.expense,
            R.string.category,
            R.string.notes
        )
    ): List<ImportAccount> {
        val reader = InputStreamReader(
            ByteArrayInputStream(fileContent.toByteArray()),
            StandardCharsets.UTF_8
        )
        val records = CSVFormat.DEFAULT.parse(reader).records
        val p = CSVParser(
            mock(),
            records,
            columnToFieldMap,
            dateFormat,
            CurrencyUnit.DebugInstance,
            AccountType.BANK
        )
        p.parse()
        return p.accounts
    }
}