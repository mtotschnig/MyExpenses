package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentUris.appendId
import android.content.ContentValues
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.util.*

class CategoriesCabTest : BaseUiTest() {
    private lateinit var activityScenario: ActivityScenario<ManageCategories>

    private val contentResolver
        get() = targetContext.contentResolver

    val currency = CurrencyUnit(Currency.getInstance("EUR"))
    private lateinit var account: Account
    private var categoryId: Long = 0

    private fun baseFixture() {
        account = Account("Test account 1", currency, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        categoryId = Category.write(0, "TestCategory", null)
    }

    private fun fixtureWithMappedTransaction() {
        baseFixture()
        with(Transaction.getNewInstance(account.id)) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedTemplate() {
        baseFixture()
        with(Template(account, Transactions.TYPE_TRANSACTION, null)) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedBudget() {
        baseFixture()
        val budget = Budget(0L, account.id, "TITLE", "DESCRIPTION", currency, Money(currency, 200000L), Grouping.MONTH, -1, null as LocalDate?, null as LocalDate?, account.label, true)
        val budgetId = ContentUris.parseId(contentResolver!!.insert(TransactionProvider.BUDGETS_URI, budget.toContentValues())!!)
        setCategoryBudget(budgetId, categoryId, 50000)
    }

    private fun setCategoryBudget(budgetId: Long, categoryId: Long, @Suppress("SameParameterValue") amount: Long) {
        with(ContentValues(1)) {
            put(DatabaseConstants.KEY_BUDGET, amount)
            contentResolver!!.update(appendId(appendId(TransactionProvider.BUDGETS_URI.buildUpon(), budgetId), categoryId).build(),
                    this, null, null)
        }
    }

    @After
    fun tearDown() {
        Account.delete(account.id)
        contentResolver?.delete(Category.CONTENT_URI, null, null)
        activityScenario.close()
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTransaction() {
        fixtureWithMappedTransaction()
        val origListSize = launchAndOpenCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        assertThat(waitForAdapter().count).isEqualTo(origListSize)
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(getQuantityString(
                        R.plurals.not_deletable_mapped_transactions, 1, 1))))
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTemplate() {
        fixtureWithMappedTemplate()
        val origListSize = launchAndOpenCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        assertThat(waitForAdapter().count).isEqualTo(origListSize)
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(getQuantityString(
                        R.plurals.not_deletable_mapped_templates, 1, 1))))
    }

    @Test
    fun shouldNotDeleteCategoryMappedToBudget() {
        fixtureWithMappedBudget()
        val origListSize = launchAndOpenCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        onView(withText(R.string.warning_delete_category_with_budget)).check(matches(isDisplayed()))
        onView(withText(android.R.string.cancel)).perform(click())
        assertThat(waitForAdapter().count).isEqualTo(origListSize)
    }

    private fun launchAndOpenCab(): Int {
        activityScenario = ActivityScenario.launch(ManageCategories::class.java)
        val origListSize = waitForAdapter().count
        Espresso.onData(Matchers.`is`(Matchers.instanceOf(org.totschnig.myexpenses.viewmodel.data.Category::class.java)))
                .atPosition(0)
                .perform(ViewActions.longClick())
        return origListSize
    }

    @Test
    fun shouldCreateSubCategory() {
        baseFixture()
        launchAndOpenCab()
        clickMenuItem(R.id.CREATE_SUB_COMMAND, true)
        onView(withId(R.id.editText))
                .perform(replaceText("Subcategory"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click())
        assertThat(Category.countSub(categoryId)).isEqualTo(1)
    }

    override val testScenario: ActivityScenario<out ProtectedFragmentActivity>
        get() = activityScenario
}