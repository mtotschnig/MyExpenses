package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Before
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.BudgetEdit
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withAccount
import kotlin.reflect.KClass
import kotlin.test.Test


@TestShard1
class BudgetEditTest : BaseUiTest<BudgetEdit>() {

    lateinit var account1: Account
    lateinit var account2: Account

    @Before
    fun fixture() {
        account1 = buildAccount("Test account 1", 0)
        account2 = buildAccount("Test account 2", 0)
        testScenario = ActivityScenario.launch(Intent(targetContext, BudgetEdit::class.java))
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun testCategoryFilter() {
        testFilterWithIntent(R.id.FILTER_CATEGORY_COMMAND, ManageCategories::class)
    }

    @Test
    fun testPayeeFilter() {
        testFilterWithIntent(R.id.FILTER_PAYEE_COMMAND, ManageParties::class)
    }

    @Test
    fun testTagFilter() {
        testFilterWithIntent(R.id.FILTER_TAG_COMMAND, ManageTags::class)
    }

    private fun testFilterWithIntent(@IdRes command: Int, activity: KClass<out BaseActivity>) {
        closeSoftKeyboard()
        onView(withId(command)).perform(click())
        intended(hasComponent(activity.java.name))
    }

    private fun testFilterWithDialog(@IdRes command: Int, @StringRes dialogTitle: Int) {
        closeSoftKeyboard()
        onView(withId(command)).perform(click())
        onView(withText(dialogTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun testMethodFilter() {
        testFilterWithDialog(R.id.FILTER_METHOD_COMMAND, R.string.search_method)
    }

    @Test
    fun testStatusFilter() {
        testFilterWithDialog(R.id.FILTER_STATUS_COMMAND, R.string.search_status)
    }

    @Test
    fun testAccountFilter() {
        onView(withId(R.id.FILTER_ACCOUNT_COMMAND)).check(
            matches(
                withEffectiveVisibility(
                    ViewMatchers.Visibility.GONE
                )
            )
        )
        onView(withId(R.id.Accounts)).perform(click())
        onData( withAccount(homeCurrency.code))
            .perform(click())
        testFilterWithDialog(R.id.FILTER_ACCOUNT_COMMAND, R.string.search_account)
    }
}