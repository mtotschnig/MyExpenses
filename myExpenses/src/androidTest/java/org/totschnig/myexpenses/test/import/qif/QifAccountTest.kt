package org.totschnig.myexpenses.test.import.qif

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import java.math.BigDecimal

class QifAccountTest {

    @Test
    fun shouldPreserveDataUponConversion() {
        val qifAccount = ImportAccount(
            type = PREDEFINED_NAME_BANK,
            memo = "People's Bank",
            desc = "Savings",
            openingBalance = BigDecimal("-1234456.78")
        )

        with(qifAccount.toAccount(CurrencyUnit.DebugInstance, AccountType(name= PREDEFINED_NAME_BANK))) {
            assertThat(label).isEqualTo("People's Bank")
            assertThat(description).isEqualTo("Savings")
            assertThat(type.name).isEqualTo(PREDEFINED_NAME_BANK)
            assertThat(currency).isEqualTo(CurrencyUnit.DebugInstance.code)
            assertThat(openingBalance).isEqualTo(-123445678)
        }
    }
}