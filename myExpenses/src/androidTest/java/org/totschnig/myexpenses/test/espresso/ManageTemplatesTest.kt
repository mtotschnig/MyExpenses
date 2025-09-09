package org.totschnig.myexpenses.test.espresso

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.CursorMatchers
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TransactionType
import org.totschnig.myexpenses.db2.countTransactionsPerAccount
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import java.time.LocalDate

//TODO test CAB actions
@TestShard3
class ManageTemplatesTest : BaseUiTest<ManageTemplates>() {
    @get:Rule
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )
    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var plan1: Plan

    private fun buildAccount() {
        account1 = buildAccount("Test account 1", 0)
        account2 = buildAccount("Test account 2", 0)
    }

    private fun fixture(type: Int, defaultAction: Template.Action) {
        buildAccount()
        createInstance(type, defaultAction)
        Intents.init()
    }

    private fun createInstance(@TransactionType type: Int, defaultAction: Template.Action) {
        val title = "Espresso $type Template ${defaultAction.name}"
        when (type) {
            TYPE_TRANSACTION -> {
                Template(
                    contentResolver,
                    account1.id,
                    homeCurrency,
                    TYPE_TRANSACTION,
                    null
                ).apply {
                    amount = Money(homeCurrency, -1200L)
                    this.defaultAction = defaultAction
                    this.title = title
                    save(contentResolver)
                }
            }

            TYPE_TRANSFER -> {
                Template.getTypedNewInstance(
                    contentResolver,
                    TYPE_TRANSFER,
                    account1.id,
                    homeCurrency,
                    false,
                    null
                )!!.apply {
                    amount = Money(homeCurrency, -1200L)
                    setTransferAccountId(account2.id)
                    this.title = title
                    this.defaultAction = defaultAction
                    save(contentResolver)
                }
            }

            TYPE_SPLIT -> {
                Template.getTypedNewInstance(
                    contentResolver,
                    TYPE_SPLIT,
                    account1.id,
                    homeCurrency,
                    false,
                    null
                )!!.apply {
                    amount = Money(homeCurrency, -1200L)
                    this.title = title
                    this.defaultAction = defaultAction
                    save(contentResolver, true)
                    val part = Template.getTypedNewInstance(
                        contentResolver,
                        TYPE_SPLIT,
                        account1.id,
                        homeCurrency,
                        false,
                        id
                    )!!
                    part.save(contentResolver)
                }
            }
        }
    }

    @After
    fun tearDown() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    private fun verifyEditAction() {
        Intents.intended(
            IntentMatchers.hasComponent(
                ExpenseEdit::class.java.name
            )
        )
    }

    private fun verifySaveAction() {
        assertThat(
            repository.count(
                Transaction.CONTENT_URI,
                DatabaseConstants.KEY_ACCOUNTID + " = ? AND " + DatabaseConstants.KEY_PARENTID + " IS NULL",
                arrayOf(account1.id.toString())
            )
        ).isEqualTo(1)
    }

    @Test
    fun defaultActionEditWithTransaction() {
        doTheTest(TYPE_TRANSACTION, Template.Action.EDIT)
    }

    @Test
    fun defaultActionSaveWithTransaction() {
        doTheTest(TYPE_TRANSACTION, Template.Action.SAVE)
    }

    @Test
    fun defaultActionEditWithTransfer() {
        doTheTest(TYPE_TRANSFER, Template.Action.EDIT)
    }

    @Test
    fun defaultActionSaveWithTransfer() {
        doTheTest(TYPE_TRANSFER, Template.Action.SAVE)
    }

    @Test
    fun defaultActionEditWithSplit() {
        unlock()
        doTheTest(TYPE_SPLIT, Template.Action.EDIT)
    }

    @Test
    fun defaultActionSaveWithSplit() {
        unlock()
        doTheTest(TYPE_SPLIT, Template.Action.SAVE)
    }

    @Test
    fun planIsDisplayed() {
        buildAccount()
        assertWithMessage("Unable to create planner").that(
            plannerUtils.createPlanner(true)
        ).isNotEqualTo(INVALID_CALENDAR_ID)
        Template(
            contentResolver,
            account1.id,
            homeCurrency,
            TYPE_TRANSACTION,
            null
        ).apply {
            amount = Money(homeCurrency, -1200L)
            this.isPlanExecutionAutomatic = true
            title = "Espresso Plan"
            plan1 = Plan(
                LocalDate.now(),
                "FREQ=WEEKLY;COUNT=10;WKST=SU",
                title,
                compileDescription(app)
            ).apply {
                save(contentResolver, plannerUtils)
            }
            planId = plan1.id
            save(contentResolver)
        }
        launch()
        onData(CursorMatchers.withRowString(DatabaseConstants.KEY_TITLE, "Espresso Plan"))
            .perform(ViewActions.click())
        Plan.delete(contentResolver, plan1.id)
    }

    private fun launch() {
        testScenario = ActivityScenario.launch(Intent(targetContext, ManageTemplates::class.java))
    }

    private fun doTheTest(@TransactionType type: Int, defaultAction: Template.Action) {
        fixture(type, defaultAction)
        assertThat(repository.countTransactionsPerAccount(account1.id))
            .isEqualTo(0)
        launch()
        val title = "Espresso $type Template $defaultAction"
        onData(CursorMatchers.withRowString(DatabaseConstants.KEY_TITLE, title))
            .perform(ViewActions.click())
        when (defaultAction) {
            Template.Action.SAVE -> verifySaveAction()
            Template.Action.EDIT -> verifyEditAction()
        }
        Intents.release()
    }
}
