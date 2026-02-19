package org.totschnig.myexpenses.test.espresso

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.CursorMatchers.withRowString
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TransactionType
import org.totschnig.myexpenses.db2.countTransactionsPerAccount
import org.totschnig.myexpenses.db2.createPlan
import org.totschnig.myexpenses.db2.createSplitTemplate
import org.totschnig.myexpenses.db2.createTemplate
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deletePlan
import org.totschnig.myexpenses.db2.entities.Recurrence
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_TITLE
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withDrawableState
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalTime
import java.time.ZonedDateTime

//TODO test CAB actions
@TestShard3
class ManageTemplatesTest : BaseUiTest<ManageTemplates>() {
    @get:Rule
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )
    private lateinit var account1: Account
    private lateinit var account2: Account

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
                repository.insertTemplate(
                    accountId = account1.id,
                    defaultAction = defaultAction,
                    title = title,
                    amount = -1200L
                )
            }

            TYPE_TRANSFER -> {
                repository.insertTemplate(
                    accountId = account1.id,
                    transferAccountId = account2.id,
                    defaultAction = defaultAction,
                    title = title,
                    amount = -1200L
                )
            }

            TYPE_SPLIT -> {
                repository.createSplitTemplate(
                    Template(
                        accountId = account1.id,
                        defaultAction = defaultAction,
                        title = title,
                        amount = -1200L,
                        categoryId = SPLIT_CATID,
                        uuid = generateUuid()
                    ), listOf(
                        Template(
                            accountId = account1.id,
                            amount = -1200L,
                            title = "",
                            uuid = generateUuid(),
                        )
                    )
                )
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
                TransactionProvider.TRANSACTIONS_URI,
                "$KEY_ACCOUNTID = ? AND $KEY_PARENTID IS NULL",
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
    fun savePlanInstance() {
        doThePlanTest(Template.Action.SAVE)
    }

    @Test
    fun editPlanInstance() {
        doThePlanTest(Template.Action.EDIT)
    }

    private fun doThePlanTest(action: Template.Action) {
        buildAccount()
        assertWithMessage("Unable to create planner").that(
            plannerUtils.createPlanner(true)
        ).isNotEqualTo(INVALID_CALENDAR_ID)
        val title = "Espresso Plan"
        val template = Template(
            accountId = account1.id,
            title = title,
            amount = -1200L,
            uuid = generateUuid(),
        )
        val today = ZonedDateTime.now().with(LocalTime.NOON)

        val eventId = repository.createPlan(
            title,
            description = "description",
            date = today.toLocalDate(),
            recurrence = Recurrence.WEEKLY
        ).id
        try {
            val id = repository.createTemplate(
                template.copy(planId = eventId)
            ).id
            launch()
            onData(withRowString(KEY_TITLE, "Espresso Plan"))
                .perform(click())
            onView(
                allOf(
                    withDrawableState(com.caldroid.R.attr.state_date_today),
                    isDisplayed()
                )
            ).perform(click())
            onView(
                withText(
                    when (action) {
                        Template.Action.SAVE -> R.string.menu_create_instance_save

                        Template.Action.EDIT -> R.string.menu_create_instance_edit
                    }
                )
            )
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click())
            if (action == Template.Action.EDIT) {
                clickFab()
            }
            contentResolver.query(
                TransactionProvider.PLAN_INSTANCE_STATUS_URI,
                null, null, null, null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(KEY_TEMPLATEID, id)
                hasLong(KEY_TRANSACTIONID) { isGreaterThan(0) }
                hasLong(
                    KEY_INSTANCEID,
                    CalendarProviderProxy.calculateId(today.toEpochSecond() * 1000)
                )
            }
        } finally {
            repository.deletePlan(eventId)
        }
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
        onData(withRowString(KEY_TITLE, title))
            .perform(click())
        when (defaultAction) {
            Template.Action.SAVE -> verifySaveAction()
            Template.Action.EDIT -> verifyEditAction()
        }
        Intents.release()
    }
}
