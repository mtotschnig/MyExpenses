package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.util.Currency

class ForeignTransferEditTest : BaseExpenseEditTest() {
    private var transfer: Transfer? = null
    @Before
    fun fixture() {
        val currency1 = CurrencyUnit(Currency.getInstance("USD"))
        val currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(
            accountLabel1,
            currency = currency1.code
        )
        val accountLabel2 = "Test label 2"
        val id = buildAccount(
            accountLabel2,
            currency = currency1.code
        ).id
        transfer = Transfer.getNewInstance(account1.id, currency1, id).apply {
            setAmountAndTransferAmount(Money(currency1, -2000L), Money(currency2, -3000L))
            save(contentResolver)
        }
    }

    @Test
    fun shouldSaveForeignTransfer() {
        val i = intent
        i.putExtra(DatabaseConstants.KEY_ROWID, transfer!!.id)
        testScenario = ActivityScenario.launchActivityForResult(i)
        closeKeyboardAndSave()
        assertFinishing()
    }
}
