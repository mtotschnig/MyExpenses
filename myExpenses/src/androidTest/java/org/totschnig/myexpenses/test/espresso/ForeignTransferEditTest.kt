package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency

@TestShard2
class ForeignTransferEditTest : BaseExpenseEditTest() {
    private var transfer: Long = 0
    lateinit var account2: Account
    @Before
    fun fixture() {
        val currency1 = CurrencyUnit(Currency.getInstance("USD"))
        val currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account1 = buildAccount(
            ACCOUNT_LABEL_1,
            currency = currency1.code
        )
        account2 = buildAccount(
            ACCOUNT_LABEL_2,
            currency = currency2.code
        )
        transfer = repository.insertTransfer(
            accountId = account1.id,
            transferAccountId = account2.id,
            amount = -2000L,
            transferAmount = 3000L,
        ).data.id
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
        i.putExtra(KEY_ROWID, transfer)
        testScenario = ActivityScenario.launchActivityForResult(i)
        androidx.test.espresso.Espresso.onIdle()
        closeKeyboardAndSave()
        assertFinishing()
        assertTransfer(transfer, account1.id , -2000L, account2.id, 3000L)
    }
}
