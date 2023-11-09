package org.totschnig.myexpenses.test.espresso

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.Espresso.*
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.ui.AmountInput
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*

class ExpenseEditLoadDataTest : BaseExpenseEditTest() {

    @get:Rule
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )
    private lateinit var foreignCurrency: CurrencyUnit
    private lateinit var account2: Account
    private lateinit var transaction: Transaction
    private lateinit var transfer: Transfer

    @Before
    fun fixture() {
        //IdlingRegistry.getInstance().register(getIdlingResource());
        foreignCurrency = CurrencyUnit(Currency.getInstance("USD"))
        account1 = buildAccount("Test account 1")
        account2 = buildAccount("Test account 2")
        transaction = Transaction.getNewInstance(account1.id, homeCurrency).apply {
            amount = Money(homeCurrency, 500L)
            save(contentResolver)
        }
        transfer = Transfer.getNewInstance(account1.id, homeCurrency, account2.id).apply {
            setAmount(Money(homeCurrency, -600L))
            save(contentResolver)
        }
    }

    private fun load(id: Long) = launchAndWait(intent.apply {
        putExtra(DatabaseConstants.KEY_ROWID, id)
    })

    @Test
    fun shouldPopulateWithTransactionAndPrepareForm() {
        load(transaction.id).use {
            checkEffectiveGone(R.id.OperationType)
            toolbarTitle().check(matches(withText(R.string.menu_edit_transaction)))
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkAmount(5)
        }
    }

    @Test
    fun shouldKeepStatusAndUuidAfterSave() {
        load(transaction.id).use {
            val uuid = transaction.uuid
            val status = transaction.status
            closeKeyboardAndSave()
            val t = getTransactionFromDb(transaction.id)
            Assertions.assertThat(t.status).isEqualTo(status)
            Assertions.assertThat(t.uuid).isEqualTo(uuid)
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldPopulateWithForeignExchangeTransfer() {
        val foreignAccount = Account(
            label ="Test account 2",
            currency = foreignCurrency.code,
        ).createIn(repository)
        val foreignTransfer = Transfer.getNewInstance(account1.id, homeCurrency, foreignAccount.id)
        foreignTransfer.setAmountAndTransferAmount(
            Money(homeCurrency, 100L), Money(
                foreignCurrency, 200L
            )
        )
        foreignTransfer.save(contentResolver)
        load(foreignTransfer.id).use {
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
    }

    private fun formatAmount(amount: Float): String {
        return DecimalFormat("0.##").format(amount.toDouble())
    }

    private fun launchAndWait(i: Intent) = ActivityScenario.launchActivityForResult<TestExpenseEdit>(i).also {
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
        load((if (loadFromPeer) transfer.transferPeer else transfer.id)!!).use {
            checkEffectiveGone(R.id.OperationType)
            toolbarTitle().check(matches(withText(R.string.menu_edit_transfer)))
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
                CoreMatchers.allOf(
                    ViewMatchers.withSpinnerText(
                        account1.label
                    ),
                    ViewMatchers.withParent(ViewMatchers.hasDescendant(withText(R.string.transfer_from_account)))
                )
            )
        )
        onView(withId(account2Spinner)).check(
            matches(
                CoreMatchers.allOf(
                    ViewMatchers.withSpinnerText(
                        account2.label
                    ),
                    ViewMatchers.withParent(ViewMatchers.hasDescendant(withText(R.string.transfer_to_account)))
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
                DatabaseConstants.KEY_ROWID,
                if (loadFromPeer) transfer.transferPeer else transfer.id
            )
            putExtra(ExpenseEdit.KEY_CLONE, true)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
                R.id.TransferAccountRow
            )
            toolbarTitle().check(matches(withText(R.string.menu_create_transfer)))
            checkAmount(6)
            checkTransferDirection(loadFromPeer)
        }
    }

    @Test
    fun shouldSwitchAccountViewsForReceivingTransferPart() {
        load(transfer.transferPeer!!).use {
            testScenario.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat((activity.findViewById<View>(R.id.Amount) as AmountInput).type).isTrue
                Assertions.assertThat(
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
        load(transfer.id).use {
            testScenario.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat((activity.findViewById<View>(R.id.Amount) as AmountInput).type).isFalse
                Assertions.assertThat(
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
        val splitTransaction: Transaction = SplitTransaction.getNewInstance(contentResolver, account1.id, homeCurrency)
        splitTransaction.status = DatabaseConstants.STATUS_NONE
        splitTransaction.save(contentResolver, true)
        load(splitTransaction.id).use {
            checkEffectiveGone(R.id.OperationType)
            toolbarTitle().check(matches(withText(R.string.menu_edit_split)))
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitRow,
                R.id.PayeeRow, R.id.AccountRow
            )

        }
    }

    @Test
    fun shouldPopulateWithSplitTemplateAndLoadParts() {
        launchAndWait(intent.apply {
            putExtra(DatabaseConstants.KEY_TEMPLATEID, buildSplitTemplate())
        }).use {
            it.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat(activity.isTemplate).isTrue()
            }
            toolbarTitle().check(matches(ViewMatchers.withSubstring(getString(R.string.menu_edit_template))))
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
            putExtra(DatabaseConstants.KEY_TEMPLATEID, buildSplitTemplate())
        }).use {
            it.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat(activity.isTemplate).isFalse()
            }
            toolbarTitle().check(matches(ViewMatchers.withSubstring(getString(R.string.menu_create_split))))
            checkEffectiveVisible(R.id.SplitRow)
            onView(withId(R.id.list))
                .check(matches(ViewMatchers.hasChildCount(1)))

        }
    }

    private fun buildSplitTemplate(): Long {
        val template =
            Template.getTypedNewInstance(contentResolver, Transactions.TYPE_SPLIT, account1.id, homeCurrency, false, null)
        template!!.save(contentResolver, true)
        val part = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_SPLIT,
            account1.id,
            homeCurrency,
            false,
            template.id
        )
        part!!.save(contentResolver)
        return template.id
    }

    @Test
    fun shouldPopulateWithPlanAndPrepareForm() {
        val plan = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_TRANSACTION,
            account1.id,
            homeCurrency,
            false,
            null
        )
        plan!!.title = "Daily plan"
        plan.amount = Money(homeCurrency, 700L)
        plan.plan = Plan(
            LocalDate.now(),
            Plan.Recurrence.DAILY,
            "Daily",
            plan.compileDescription(app)
        )
        plan.save(contentResolver, plannerUtils, false)
        launchAndWait(intent.apply {
            putExtra(DatabaseConstants.KEY_TEMPLATEID, plan.id)
        }).use {
            checkEffectiveVisible(
                R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow, R.id.PB
            )
            checkEffectiveGone(R.id.Recurrence)
            testScenario.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat(activity.isTemplate).isTrue()
            }
            checkAmount(7)
            onView(withId(R.id.Title))
                .check(matches(withText("Daily plan")))

        }
        Plan.delete(contentResolver, plan.planId)
    }

    @Test
    fun shouldInstantiateFromTemplateAndPrepareForm() {
        val template = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_TRANSACTION,
            account1.id,
            homeCurrency,
            false,
            null
        )
        template!!.title = "Nothing but a plan"
        template.amount = Money(homeCurrency, 800L)
        template.save(contentResolver)
        launchAndWait(intent.apply {
            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
            putExtra(DatabaseConstants.KEY_TEMPLATEID, template.id)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkEffectiveGone(R.id.PB, R.id.TitleRow)
            testScenario.onActivity { activity: ExpenseEdit ->
                Assertions.assertThat(activity.isTemplate).isFalse()
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
            onView(withId(R.id.Account)).check(
                matches(
                    ViewMatchers.withSpinnerText(
                        account1.label
                    )
                )
            )
            onView(
                withIdAndParent(
                    R.id.AmountEditText,
                    R.id.Amount
                )
            ).check(matches(withText(formatAmount(1.23f))))
            onView(withId(R.id.Payee))
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
        val sealed = Transaction.getNewInstance(sealedAccount.id, homeCurrency)
        sealed.amount = Money(homeCurrency, 500L)
        sealed.save(contentResolver)
        val values = ContentValues(1)
        values.put(DatabaseConstants.KEY_SEALED, true)
        app.contentResolver.update(
            ContentUris.withAppendedId(
                TransactionProvider.ACCOUNTS_URI,
                sealedAccount.id
            ), values, null, null
        )
        load(sealed.id).use {
            assertCanceled()
        }
    }
}