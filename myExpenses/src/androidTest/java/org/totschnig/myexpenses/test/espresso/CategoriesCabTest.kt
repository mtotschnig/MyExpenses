package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentUris.appendId
import android.content.ContentValues
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.FlakyTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Rule
import org.junit.Test
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
import java.time.LocalDate
import java.util.*

class CategoriesCabTest : BaseUiTest() {
    @get:Rule
    var mActivityRule: ActivityTestRule<ManageCategories?> = ActivityTestRule(ManageCategories::class.java, true, false)

    private val contentResolver
        get() = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    val currency = CurrencyUnit.create(Currency.getInstance("EUR"))
    lateinit var account: Account

    private fun fixtureWithMappedTransaction() {
        account = Account("Test account 1", currency, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val categoryId = Category.write(0, "TestCategory", null)
        with(Transaction.getNewInstance(account.id)) {
            amount = Money(CurrencyUnit.create(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedTemplate() {
        account = Account("Test account 1", CurrencyUnit.create(Currency.getInstance("EUR")), 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val categoryId = Category.write(0, "TestCategory", null)
        with(Template(account, Transactions.TYPE_TRANSACTION, null)) {
            amount = Money(CurrencyUnit.create(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedBudget() {
        account = Account("Test account 1", CurrencyUnit.create(Currency.getInstance("EUR")), 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val categoryId = Category.write(0, "TestCategory", null)
        val budget = Budget(0L, account.id, "TITLE", "DESCRIPTION", currency, Money(currency, 200000L), Grouping.MONTH, -1, null as LocalDate?, null as LocalDate?, account.getLabel(), true)
        val budgetId = ContentUris.parseId(contentResolver!!.insert(TransactionProvider.BUDGETS_URI, budget.toContentValues())!!)
        setCategoryBudget(budgetId, categoryId, 50000)
    }

    fun setCategoryBudget(budgetId: Long, categoryId: Long, amount: Long) {
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
    }

    @FlakyTest
    @Test
    fun shouldNotDeleteCategoryMappedToTransaction() {
        fixtureWithMappedTransaction()
        mActivityRule.launchActivity(null)
        val origListSize = waitForAdapter().count
        Espresso.onData(Matchers.`is`(Matchers.instanceOf(org.totschnig.myexpenses.viewmodel.data.Category::class.java)))
                .atPosition(0)
                .perform(ViewActions.longClick())
        clickMenuItem(R.id.DELETE_COMMAND, R.string.menu_delete, true)
        Assertions.assertThat(waitForAdapter().count).isEqualTo(origListSize)
        onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(mActivityRule.activity!!.resources.getQuantityString(
                        R.plurals.not_deletable_mapped_transactions, 1, 1))))
    }

    @FlakyTest
    @Test
    fun shouldNotDeleteCategoryMappedToTemplate() {
        fixtureWithMappedTemplate()
        mActivityRule.launchActivity(null)
        val origListSize = waitForAdapter().count
        Espresso.onData(Matchers.`is`(Matchers.instanceOf(org.totschnig.myexpenses.viewmodel.data.Category::class.java)))
                .atPosition(0)
                .perform(ViewActions.longClick())
        clickMenuItem(R.id.DELETE_COMMAND, R.string.menu_delete, true)
        Assertions.assertThat(waitForAdapter().count).isEqualTo(origListSize)
        onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(mActivityRule.activity!!.resources.getQuantityString(
                        R.plurals.not_deletable_mapped_templates, 1, 1))))
    }

    @Test
    fun shouldNotDeleteCategoryMappedToBudget() {
        fixtureWithMappedBudget()
        mActivityRule.launchActivity(null)
        val origListSize = waitForAdapter().count
        Espresso.onData(Matchers.`is`(Matchers.instanceOf(org.totschnig.myexpenses.viewmodel.data.Category::class.java)))
                .atPosition(0)
                .perform(ViewActions.longClick())
        clickMenuItem(R.id.DELETE_COMMAND, R.string.menu_delete, true)
        onView(withText(R.string.warning_delete_category_with_budget)).check(matches(isDisplayed()))
        onView(withText(android.R.string.cancel)).perform(ViewActions.click())
        Assertions.assertThat(waitForAdapter().count).isEqualTo(origListSize)

    }

    override fun getTestRule(): ActivityTestRule<out ProtectedFragmentActivity?> {
        return mActivityRule
    }
}