package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.CursorMatchers
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest

//TODO test CAB actions
class ManageTemplatesTest : BaseUiTest<ManageTemplates>() {
    private lateinit var account1: Account
    private lateinit var account2: Account

    @Before
    fun fixture() {
        account1 = buildAccount("Test account 1", 0)
        account2 = buildAccount("Test account 2", 0)
        createInstances(Template.Action.SAVE)
        createInstances(Template.Action.EDIT)
        val i = Intent(targetContext, ManageTemplates::class.java)
        testScenario = ActivityScenario.launch(i)
        Intents.init()
    }

    private fun createInstances(defaultAction: Template.Action) {
        val currencyUnit = homeCurrency
        var template = Template(
            contentResolver,
            account1.id,
            currencyUnit,
            Transactions.TYPE_TRANSACTION,
            null
        )
        template.amount = Money(currencyUnit, -1200L)
        template.defaultAction = defaultAction
        template.title = "Espresso Transaction Template " + defaultAction.name
        template.save(contentResolver)
        template = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_TRANSFER,
            account1.id,
            currencyUnit,
            false,
            null
        )!!
        template.amount = Money(currencyUnit, -1200L)
        template.setTransferAccountId(account2.id)
        template.title = "Espresso Transfer Template " + defaultAction.name
        template.defaultAction = defaultAction
        template.save(contentResolver)
        template = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_SPLIT,
            account1.id,
            currencyUnit,
            false,
            null
        )!!
        template.amount = Money(currencyUnit, -1200L)
        template.title = "Espresso Split Template " + defaultAction.name
        template.defaultAction = defaultAction
        template.save(contentResolver, true)
        val part = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_SPLIT,
            account1.id,
            currencyUnit,
            false,
            template.id
        )!!
        part.save(contentResolver)
        Assertions.assertThat(
            repository.countTransactionsPerAccount(
                account1.id
            )
        ).isEqualTo(0)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun verifyEditAction() {
        Intents.intended(
            IntentMatchers.hasComponent(
                ExpenseEdit::class.java.name
            )
        )
    }

    private fun verifySaveAction() {
        Assertions.assertThat(
            repository.count(
                Transaction.CONTENT_URI,
                DatabaseConstants.KEY_ACCOUNTID + " = ? AND " + DatabaseConstants.KEY_PARENTID + " IS NULL",
                arrayOf(
                    account1.id.toString()
                )
            )
        ).isEqualTo(1)
    }

    @Test
    fun defaultActionEditWithTransaction() {
        doTheTest("EDIT", "Transaction")
    }

    @Test
    fun defaultActionSaveWithTransaction() {
        doTheTest("SAVE", "Transaction")
    }

    @Test
    fun defaultActionEditWithTransfer() {
        doTheTest("EDIT", "Transfer")
    }

    @Test
    fun defaultActionSaveWithTransfer() {
        doTheTest("SAVE", "Transfer")
    }

    @Test
    fun defaultActionEditWithSplit() {
        unlock()
        doTheTest("EDIT", "Split")
    }

    @Test
    fun defaultActionSaveWithSplit() {
        unlock()
        doTheTest("SAVE", "Split")
    }

    private fun doTheTest(action: String, type: String) {
        val title = String.format("Espresso %s Template %s", type, action)
        Espresso.onData(CursorMatchers.withRowString(DatabaseConstants.KEY_TITLE, title))
            .perform(ViewActions.click())
        when (action) {
            "SAVE" -> verifySaveAction()
            "EDIT" -> verifyEditAction()
        }
    }
}
