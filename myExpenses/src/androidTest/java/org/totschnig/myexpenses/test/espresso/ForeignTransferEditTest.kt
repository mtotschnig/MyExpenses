package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency

@TestShard2
class ForeignTransferEditTest : BaseExpenseEditTest() {
    private var transfer: Transfer? = null
    lateinit var account2: Account
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
        account2 = buildAccount(
            accountLabel2,
            currency = currency2.code
        )
        transfer = Transfer.getNewInstance(account1.id, currency1, account2.id).apply {
            setAmountAndTransferAmount(Money(currency1, -2000L), Money(currency2, -3000L))
            save(contentResolver)
        }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun shouldSaveForeignTransfer() {
        val i = intent
        i.putExtra(DatabaseConstants.KEY_ROWID, transfer!!.id)
        testScenario = ActivityScenario.launchActivityForResult(i)
        androidx.test.espresso.Espresso.onIdle()
        closeKeyboardAndSave()
        assertFinishing()
    }
}
