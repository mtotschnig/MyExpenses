package org.totschnig.myexpenses.test.import.qif

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.export.qif.QifAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal

class QifAccountTest {

    @Test
    fun shouldPreserveDataUponConversion() {
        val qifAccount = QifAccount().apply {
            type = "Bank"
            memo = "People's Bank"
            desc = "Savings"
            openingBalance = BigDecimal("-1234456.78")
        }

        with(qifAccount.toAccount(CurrencyUnit.DebugInstance)) {
            Truth.assertThat(label).isEqualTo("People's Bank")
            Truth.assertThat(description).isEqualTo("Savings")
            Truth.assertThat(type).isEqualTo(AccountType.BANK)
            Truth.assertThat(openingBalance.currencyUnit.code).isEqualTo(CurrencyUnit.DebugInstance.code)
            Truth.assertThat(openingBalance.amountMinor).isEqualTo(-123445678)
        }
    }
}