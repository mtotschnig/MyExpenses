package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
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
import org.totschnig.myexpenses.util.minorUnitDelta
import java.util.Currency

@TestShard2
class ForeignTransferEditTest : BaseExpenseEditTest() {
    private lateinit var account2: Account
    private val currency1 = CurrencyUnit(Currency.getInstance("USD"))

    @Before
    fun fixture() {
        account1 = buildAccount(
            ACCOUNT_LABEL_1,
            currency = currency1.code
        )
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    private fun runForeignTransferTest(
        currency2: CurrencyUnit,
        initialAmount: Long,
        initialTransferAmount: Long
    ) {
        account2 = buildAccount(
            ACCOUNT_LABEL_2,
            currency = currency2.code
        )
        val transaction = repository.insertTransfer(
            accountId = account1.id,
            transferAccountId = account2.id,
            amount = initialAmount,
            transferAmount = initialTransferAmount,
        )
        val transferId = transaction.data.id
        val peerId = transaction.transferPeer!!.id

        testScenario = ActivityScenario.launchActivityForResult(
            intent.putExtra(KEY_ROWID, transferId)
        )

        closeKeyboardAndSave()

        assertFinishing()
        assertTransfer(
            id = transferId,
            expectedAccount = account1.id,
            expectedAmount = initialAmount,
            expectedTransferAccount = account2.id,
            expectedTransferAmount = initialTransferAmount,
            expectedPeer = peerId
        )
    }

    @Test
    fun shouldSaveForeignTransfer() {
        runForeignTransferTest(
            currency2 = CurrencyUnit(Currency.getInstance("EUR")),
            initialAmount = -2000L,
            initialTransferAmount = 3000L
        )
    }

    @Test
    fun shouldSaveForeignTransferWithDifferentMinorUnits() {
        val currency2 = CurrencyUnit(Currency.getInstance("XOF"))
        assertThat(currency2.minorUnitDelta(currency1)).isEqualTo(-2)
        runForeignTransferTest(
            currency2 = currency2,
            initialAmount = -180L, // $1.8
            initialTransferAmount = 1000L, // 1000 CFA
        )
    }
}
