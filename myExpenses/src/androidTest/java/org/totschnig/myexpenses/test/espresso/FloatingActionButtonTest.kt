package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.compose.TEST_TAG_FAB_TRANSACTIONS
import org.totschnig.myexpenses.compose.transactions.Action.Expense
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.EnumPreferenceAccessor
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup

@TestShard4
class FloatingActionButtonTest : BaseMyExpensesTest() {
    private lateinit var account1: Account
    private lateinit var account2: Account

    @Before
    fun fixture() {
        account1 = buildAccount("Test account 1")
        account2 = buildAccount("Test account 2")
        kotlinx.coroutines.runBlocking {
            EnumPreferenceAccessor(
                dataStore,
                stringPreferencesKey("lastAction"),
                Expense
            ).set(Expense)
        }
        launch(account2.id)
        Intents.init()
    }

    @After
    fun clearDb() {
        Intents.release()
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun floatingActionButtonOpensForm() {
        composeTestRule.onNodeWithTag(TEST_TAG_FAB_TRANSACTIONS).performClick()
        intended(
            allOf(
                hasComponent(ExpenseEdit::class.java.name),
                hasExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION),
                hasExtra(ExpenseEdit.KEY_INCOME, false)
            )
        )
    }

    @Test
    fun extendedMenuExpenseActionOpensForm() {
        expandMenu()
        composeTestRule.onNodeWithText(getString(R.string.expense)).performClick()
        intended(
            allOf(
                hasComponent(ExpenseEdit::class.java.name),
                hasExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION),
                hasExtra(ExpenseEdit.KEY_INCOME, false)
            )
        )
    }

    @Test
    fun extendedMenuIncomeActionOpensForm() {
        expandMenu()
        composeTestRule.onNodeWithText(getString(R.string.income)).performClick()
        intended(
            allOf(
                hasComponent(ExpenseEdit::class.java.name),
                hasExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION),
                hasExtra(ExpenseEdit.KEY_INCOME, true)
            )
        )
    }

    @Test
    fun extendedMenuTransferActionOpensForm() {
        expandMenu()
        composeTestRule.onNodeWithText(getString(R.string.transfer)).performClick()
        intended(
            allOf(
                hasComponent(ExpenseEdit::class.java.name),
                hasExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSFER),
                hasExtra(ExpenseEdit.KEY_INCOME, false)
            )
        )
    }

    @Test
    fun extendedMenuSplitActionOpensForm() {
        expandMenu()
        composeTestRule.onNodeWithText(getString(R.string.split_transaction)).performClick()
        handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
        intended(
            allOf(
                hasComponent(ExpenseEdit::class.java.name),
                hasExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT),
                hasExtra(ExpenseEdit.KEY_INCOME, false)
            )
        )
    }

    private fun expandMenu() {
        composeTestRule.onNode(
            hasContentDescription(
                getString(androidx.appcompat.R.string.abc_action_menu_overflow_description)
            ) and hasAnyAncestor(
                hasTestTag(TEST_TAG_FAB_TRANSACTIONS)
            )
        ).performClick()
    }
}
