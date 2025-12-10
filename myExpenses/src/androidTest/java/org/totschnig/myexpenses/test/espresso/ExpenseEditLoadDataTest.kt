package org.totschnig.myexpenses.test.espresso

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import com.adevinta.android.barista.assertion.BaristaProgressBarAssertions.assertProgress
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.createPlan
import org.totschnig.myexpenses.db2.createSplitTemplate
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.createTemplate
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deletePlan
import org.totschnig.myexpenses.db2.entities.Recurrence
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.STATUS_EXPORTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withIdAndAncestor
import org.totschnig.myexpenses.testutils.withIdAndParent
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.Currency

@TestShard2
class ExpenseEditLoadDataTest : BaseExpenseEditTest() {

    @get:Rule
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )
    private lateinit var foreignCurrency: CurrencyUnit
    private lateinit var account2: Account
    private lateinit var transaction: RepositoryTransaction
    private lateinit var transfer: RepositoryTransaction

    @Before
    fun fixture() {
        foreignCurrency = CurrencyUnit(Currency.getInstance("AUD"))
        check(foreignCurrency.code != homeCurrency.code)
        account1 = buildAccount("Test account 1")
        account2 = buildAccount("Test account 2")
        transaction = repository.createTransaction(
            Transaction(
                accountId = account1.id,
                amount = 500L,
                status = STATUS_EXPORTED,
                uuid = generateUuid()
            )
        )

        transfer = repository.insertTransfer(
            amount = -600L, accountId = account1.id, transferAccountId = account2.id
        )
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    private fun buildForeignAccount(): Account =
        buildAccount("Test account 2", currency = foreignCurrency.code, dynamicExchangeRates = true)

    private fun load(id: Long) = launchAndWait(intent.apply {
        putExtra(KEY_ROWID, id)
    })

    @Test
    fun shouldPopulateWithTransactionAndPrepareForm() {
        load(transaction.id).use {
            checkEffectiveGone(R.id.OperationType)
            checkToolbarTitle(R.string.menu_edit_transaction)
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkAmount(5)
        }
    }

    @Test
    fun shouldPopulateWithOriginalAmount() {
        val transaction = repository.insertTransaction(
            amount = 500L,
            accountId = account1.id,
            originalAmount = 1000,
            originalCurrency = foreignCurrency.code
        )
        load(transaction.id).use {
            checkAmount(5)
            onView(withId(R.id.OriginalAmountRow)).check(matches(isDisplayed()))
            checkAmount(10, R.id.OriginalAmount)
            onView(withIdAndAncestor(R.id.ExchangeRateEdit1, R.id.OriginalAmount))
                .check(matches(withText("%.1f".format(0.5))))
            onView(withIdAndAncestor(R.id.ExchangeRateEdit2, R.id.OriginalAmount))
                .check(matches(withText("2")))
            onView(withIdAndAncestor(R.id.AmountCurrency, R.id.OriginalAmount))
                .check(matches(withSpinnerText(create(foreignCurrency.code, app).toString())))
        }
    }

    @Test
    fun shouldPopulateWithEquivalentAmount() {
        val foreignAccount = buildForeignAccount()
        val transaction = repository.insertTransaction(
            accountId = foreignAccount.id, amount = 500L, equivalentAmount = 1000
        )

        load(transaction.id).use {
            checkAmount(5)
            onView(withId(R.id.EquivalentAmountRow)).check(matches(isDisplayed()))
            checkAmount(10, R.id.EquivalentAmount)
            onView(withIdAndAncestor(R.id.ExchangeRateEdit1, R.id.EquivalentAmount))
                .check(matches(withText("2")))
            onView(withIdAndAncestor(R.id.ExchangeRateEdit2, R.id.EquivalentAmount))
                .check(matches(withText("%.1f".format(0.5))))
        }
        cleanup {
            repository.deleteAccount(foreignAccount.id)
        }
    }

    @Test
    fun shouldKeepStatusAndUuidAfterSave() {
        repository.markAsExported(account1.id, null)
        load(transaction.id).use {
            val uuid = transaction.data.uuid
            closeKeyboardAndSave()
            val t = repository.loadTransaction(transaction.id).data
            assertThat(t.status).isEqualTo(STATUS_EXPORTED)
            assertThat(t.uuid).isEqualTo(uuid)
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldPopulateWithForeignExchangeTransfer() {
        val foreignAccount = buildForeignAccount()
        val foreignTransfer = repository.insertTransfer(
            account1.id, foreignAccount.id, 100L, 200L
        )
        load(foreignTransfer.data.id).use {
            checkAmount(1)
            checkAmount(2, R.id.TransferAmount)
            onView(
                withIdAndAncestor(
                    R.id.ExchangeRateEdit1,
                    R.id.ExchangeRate
                )
            ).check(matches(withText("2")))
            onView(
                withIdAndAncestor(
                    R.id.ExchangeRateEdit2,
                    R.id.ExchangeRate
                )
            ).check(matches(withText(formatAmount(0.5f))))
        }
        cleanup {
            repository.deleteTransaction(foreignTransfer.data.id)
            repository.deleteAccount(foreignAccount.id)
        }
    }

    private fun formatAmount(amount: Float): String {
        return DecimalFormat("0.##").format(amount.toDouble())
    }

    private fun launchAndWait(i: Intent) =
        ActivityScenario.launchActivityForResult<TestExpenseEdit>(i).also {
            testScenario = it
        }

    @Test
    fun shouldPopulateWithTransferAndPrepareForm() {
        testTransfer(false)
    }

    @Test
    fun shouldPopulateWithTransferFromPeerAndPrepareForm() {
        testTransfer(true)
    }

    private fun testTransfer(loadFromPeer: Boolean) {
        load((if (loadFromPeer) transfer.transferPeer!! else transfer.data).id).use {
            checkEffectiveGone(R.id.OperationType)
            checkToolbarTitle(R.string.menu_edit_transfer)
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
                R.id.TransferAccountRow
            )
            checkAmount(6)
            checkTransferDirection(loadFromPeer)

        }
    }

    private fun checkTransferDirection(loadFromPeer: Boolean) {
        val account1Spinner = if (loadFromPeer) R.id.TransferAccount else R.id.Account
        val account2Spinner = if (loadFromPeer) R.id.Account else R.id.TransferAccount
        onView(withId(account1Spinner)).check(
            matches(
                allOf(
                    withSpinnerText(
                        account1.label
                    ),
                    withParent(hasDescendant(withText(R.string.transfer_from_account)))
                )
            )
        )
        onView(withId(account2Spinner)).check(
            matches(
                allOf(
                    withSpinnerText(
                        account2.label
                    ),
                    withParent(hasDescendant(withText(R.string.transfer_to_account)))
                )
            )
        )
    }

    @Test
    fun shouldPopulateWithTransferCloneAndPrepareForm() {
        testTransferClone(false)
    }

    @Test
    fun shouldPopulateWithTransferCloneFromPeerAndPrepareForm() {
        testTransferClone(true)
    }

    private fun testTransferClone(loadFromPeer: Boolean) {
        launchAndWait(intent.apply {
            putExtra(
                KEY_ROWID,
                (if (loadFromPeer) transfer.transferPeer!! else transfer.data).id
            )
            putExtra(ExpenseEdit.KEY_CLONE, true)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
                R.id.TransferAccountRow
            )
            checkToolbarTitle(R.string.menu_create_transfer)
            checkAmount(6)
            checkTransferDirection(loadFromPeer)
        }
    }

    @Test
    fun shouldSwitchAccountViewsForReceivingTransferPart() {
        load(transfer.transferPeer!!.id).use {
            testScenario.onActivity { activity: ExpenseEdit ->
                assertThat((activity.findViewById<View>(R.id.Amount) as AmountInput).type).isTrue()
                assertThat(
                    (activity.findViewById<View>(R.id.AccountRow) as ViewGroup).getChildAt(
                        1
                    ).id
                ).isEqualTo(R.id.TransferAccount)
            }
            checkAmount(6)
        }
    }

    @Test
    fun shouldKeepAccountViewsForGivingTransferPart() {
        load(transfer.data.id).use {
            testScenario.onActivity { activity: ExpenseEdit ->
                assertThat((activity.findViewById<View>(R.id.Amount) as AmountInput).type).isFalse()
                assertThat(
                    (activity.findViewById<View>(R.id.AccountRow) as ViewGroup).getChildAt(
                        1
                    ).id
                ).isEqualTo(R.id.Account)
            }
            checkAmount(6)
        }
    }

    @Test
    fun shouldPopulateWithSplitTransactionAndPrepareForm() {
        val splitTransaction = repository.createSplitTransaction(
            Transaction(
                amount = 0L,
                accountId = account1.id,
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ), emptyList<Transaction>()
        )
        load(splitTransaction.id).use {
            checkEffectiveGone(R.id.OperationType)
            checkToolbarTitle(R.string.menu_edit_split)
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitRow,
                R.id.PayeeRow, R.id.AccountRow
            )

        }
    }

    @Test
    fun shouldPopulateWithSplitTemplateAndLoadParts() {
        launchAndWait(intent.apply {
            putExtra(KEY_TEMPLATEID, buildSplitTemplate())
        }).use {
            it.onActivity { activity: ExpenseEdit ->
                assertThat(activity.isTemplate).isTrue()
            }
            checkToolbarTitleForTemplate(true, Transactions.TYPE_SPLIT)
            checkEffectiveVisible(R.id.SplitRow)
            checkEffectiveGone(R.id.OperationType)
            onView(withId(R.id.list))
                .check(matches(ViewMatchers.hasChildCount(1)))
        }
    }

    @Test
    fun shouldPopulateFromSplitTemplateAndLoadParts() {
        launchAndWait(intent.apply {
            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, buildSplitTemplate())
        }).use {
            it.onActivity { activity: ExpenseEdit ->
                assertThat(activity.isTemplate).isFalse()
            }
            checkToolbarTitle(R.string.menu_create_split)
            checkEffectiveVisible(R.id.SplitRow)
            onView(withId(R.id.list))
                .check(matches(ViewMatchers.hasChildCount(1)))

        }
    }

    private fun buildSplitTemplate(): Long {
        val template = repository.createSplitTemplate(
            Template(
                amount = 0L,
                accountId = account1.id,
                categoryId = SPLIT_CATID,
                title = "Split",
                uuid = generateUuid()
            ), listOf(
                Template(
                    amount = 0L,
                    accountId = account1.id,
                    title = "Part",
                    uuid = generateUuid()
                )
            )
        )
        return template.id
    }

    @Test
    fun shouldPopulateWithPlanAndPrepareForm() {
        val template = Template(
            accountId = account1.id,
            title = "Daily plan",
            amount = 700L,
            uuid = generateUuid(),
            planExecutionAutomatic = true,
            planExecutionAdvance = 7
        )
        val eventId = repository.createPlan(
            title = "Daily plan",
            description = "description",
            date = LocalDate.now(),
            recurrence = Recurrence.DAILY
        ).id
        val plan = repository.createTemplate(
            template.copy(planId = eventId)
        )

        launchAndWait(intent.apply {
            putExtra(KEY_TEMPLATEID, plan.id)
        }).use {
            checkEffectiveVisible(
                R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow, R.id.PB
            )
            checkEffectiveGone(R.id.Recurrence)
            testScenario.onActivity { activity: ExpenseEdit ->
                assertThat(activity.isTemplate).isTrue()
            }
            checkAmount(7)
            closeSoftKeyboard()
            onView(withId(R.id.PlanExecution)).check(matches(isChecked()))
            assertProgress(R.id.advanceExecutionSeek, 7)
            onView(withId(R.id.Title))
                .check(matches(withText("Daily plan")))

        }
        repository.deletePlan(eventId)
    }

    @Test
    fun shouldInstantiateFromTemplateAndPrepareForm() {
        val template = repository.insertTemplate(
            accountId = account1.id,
            title =  "Nothing but a plan",
            amount = 800,
        )
        launchAndWait(intent.apply {
            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, template.id)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkEffectiveGone(R.id.PB, R.id.TitleRow)
            testScenario.onActivity { activity: ExpenseEdit ->
                assertThat(activity.isTemplate).isFalse()
            }
            checkAmount(8)
        }
    }

    @Test
    fun shouldPopulateFromIntent() {
        launchAndWait(intent.apply {
            action = Intent.ACTION_INSERT
            putExtra(Transactions.ACCOUNT_LABEL, account1.label)
            putExtra(Transactions.AMOUNT_MICROS, 1230000L)
            putExtra(Transactions.PAYEE_NAME, "John Doe")
            putExtra(Transactions.CATEGORY_LABEL, "A")
            putExtra(Transactions.COMMENT, "A note")
        }).use {
            checkAccount(account1.label)
            onView(
                withIdAndParent(
                    R.id.AmountEditText,
                    R.id.Amount
                )
            ).check(matches(withText(formatAmount(1.23f))))
            onView(withId(R.id.auto_complete_textview))
                .check(matches(withText("John Doe")))
            onView(withId(R.id.Comment))
                .check(matches(withText("A note")))
            onView(withId(R.id.Category))
                .check(matches(withText("A")))
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotEditSealed() {
        val sealedAccount = buildAccount("Sealed account")
        val sealed = repository.insertTransaction(accountId = sealedAccount.id, amount = 500L)
        val values = ContentValues(1)
        values.put(KEY_SEALED, true)
        app.contentResolver.update(
            ContentUris.withAppendedId(
                TransactionProvider.ACCOUNTS_URI,
                sealedAccount.id
            ), values, null, null
        )
        load(sealed.id).use {
            assertCanceled()
        }
        cleanup {
            repository.deleteAccount(sealedAccount.id)
        }
    }
}