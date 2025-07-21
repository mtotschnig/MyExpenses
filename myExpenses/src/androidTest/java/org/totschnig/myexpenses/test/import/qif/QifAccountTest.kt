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
            type = "_BANK_",
            memo = "People's Bank",
            desc = "Savings",
            openingBalance = BigDecimal("-1234456.78")
        )

        with(qifAccount.toAccount(CurrencyUnit.DebugInstance, AccountType(name= "_BANK_"))) {
            Truth.assertThat(label).isEqualTo("People's Bank")
            Truth.assertThat(description).isEqualTo("Savings")
            Truth.assertThat(type).isEqualTo("_BANK_")
            Truth.assertThat(currency).isEqualTo(CurrencyUnit.DebugInstance.code)
            Truth.assertThat(openingBalance).isEqualTo(-123445678)
        }
    }
}