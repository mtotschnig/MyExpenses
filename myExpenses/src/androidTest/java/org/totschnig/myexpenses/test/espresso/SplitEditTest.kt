package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import java.util.*

@RunWith(AndroidJUnit4::class)
class SplitEditTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(ExpenseEdit::class.java, false, false)
    private val accountLabel1 = "Test label 1"
    private var account1: Account? = null
    private var currency1: CurrencyUnit? = null

    //TODO test split part loading

    @Before
    fun fixture() {
        currency1 = CurrencyUnit.create(Currency.getInstance("USD"))
        account1 = Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR).apply { save() }
    }

    @After
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun tearDown() {
        account1?.let {
            Account.delete(it.id)
        }
    }
    @Test
    fun canceledSplitCleanup() {
        val i = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java)
        i.putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        mActivityRule.launchActivity(i)
        val uncommittedUri = TransactionProvider.UNCOMMITTED_URI
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(1)
        Espresso.closeSoftKeyboard()
        Espresso.pressBackUnconditionally()
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(0)
    }

    @Test
    fun canceledTemplateSplitCleanup() {
        val i = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ExpenseEdit::class.java)
        i.putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        i.putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        mActivityRule.launchActivity(i)
        val uncommittedUri = TransactionProvider.TEMPLATES_UNCOMMITTED_URI
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(1)
        Espresso.closeSoftKeyboard()
        Espresso.pressBackUnconditionally()
        assertThat(Transaction.count(uncommittedUri, DatabaseConstants.KEY_STATUS + "= ?", arrayOf(DatabaseConstants.STATUS_UNCOMMITTED.toString()))).isEqualTo(0)
    }
}