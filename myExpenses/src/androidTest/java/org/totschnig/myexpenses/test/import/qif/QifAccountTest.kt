package org.totschnig.myexpenses.test.import.qif

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal

class QifAccountTest {

    @Test
    fun shouldPreserveDataUponConversion() {
        val qifAccount = ImportAccount(
            type = AccountType.BANK,
            memo = "People's Bank",
            desc = "Savings",
            openingBalance = BigDecimal("-1234456.78")
        )

        with(qifAccount.toAccount(CurrencyUnit.DebugInstance)) {
            Truth.assertThat(label).isEqualTo("People's Bank")
            Truth.assertThat(description).isEqualTo("Savings")
            Truth.assertThat(type).isEqualTo(AccountType.BANK)
            Truth.assertThat(currency).isEqualTo(CurrencyUnit.DebugInstance.code)
            Truth.assertThat(openingBalance).isEqualTo(-123445678)
        }
    }
}