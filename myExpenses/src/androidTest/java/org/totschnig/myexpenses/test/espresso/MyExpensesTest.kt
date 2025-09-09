package org.totschnig.myexpenses.test.espresso

import android.content.OperationApplicationException
import android.os.RemoteException
import android.widget.Button
import androidx.annotation.StringRes
import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.*
import org.totschnig.myexpenses.compose.TEST_TAG_ACCOUNTS
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowMenu
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.toolbarMainSubtitle
import org.totschnig.myexpenses.testutils.toolbarMainTitle
import org.totschnig.myexpenses.testutils.withIdAndParent
import org.totschnig.myexpenses.util.formatMoney

@TestShard4
class MyExpensesTest : BaseMyExpensesTest() {
    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var account3: Account

    @Before
    fun fixture() {
        prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, true)
        account1 = buildAccount("Test account 1")
        account2 = buildAccount("Test account 2")
        account3 = buildAccount("Test account 3")
        launch(account2.id)
        Intents.init()
    }

    @After
    fun clearDb() {
        Intents.release()
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteAccount(account3.id)
        }
    }

    @Test
    fun viewPagerIsSetup() {
        composeTestRule.onNodeWithText(getString(R.string.no_expenses)).assertIsDisplayed()
        assertDataSize(4)
    }

    @Test
    fun floatingActionButtonOpensForm() {
        clickFab()
        intended(hasComponent(ExpenseEdit::class.java.name))
        pressBack()
    }

    @Test
    fun newBalanceOpensForm() {
        toolbarMainTitle().perform(click())
        onView(withText(R.string.new_balance)).perform(click())
        onView(
            withIdAndParent(
                R.id.AmountEditText,
                R.id.amount
            )
        ).perform(click(), replaceText("1000"))
        onView(withId(android.R.id.button1)).perform(click())
        intended(hasComponent(ExpenseEdit::class.java.name))
        pressBack()
    }

    @Test
    fun helpDialogIsOpened() {
        openActionBarOverflowMenu()
        onData(hasToString(getString(R.string.menu_help))).perform(click())
        onView(withText(R.string.help_MyExpenses_title))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(Matchers.`is`(app.getString(android.R.string.ok)))
            )
        )
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreenIsOpened() {
        openActionBarOverflowMenu()
        onData(hasToString(getString(R.string.settings_label)))
            .perform(click())
        intended(
            hasComponent(PreferenceActivity::class.java.name)
        )
    }

    @Test
    fun inActiveItemsAreHidden() {
        assertMenuItemHidden(R.id.RESET_COMMAND)
        assertMenuItemHidden(R.id.DISTRIBUTION_COMMAND)
        assertMenuItemHidden(R.id.PRINT_COMMAND)
    }

    @Test
    fun newAccountShowNew() {
        openDrawer()
        onView(withId(R.id.expansionTrigger)).perform(click())
        onView(withText(R.string.menu_create_account))
            .perform(click())
        intended(
            allOf(
                hasComponent(AccountEdit::class.java.name),
                not(hasExtraWithKey(DatabaseConstants.KEY_ROWID))
            )
        )
        onView(withId(R.id.Label)).perform(
            typeText("A"),
            closeSoftKeyboard()
        )
        clickFab()
        checkTitle("A")
        cleanup {
            deleteAccount("A")
        }
    }

    private fun openDrawer() {
        try {
            onView(withId(R.id.drawer)).perform(DrawerActions.open())
        } catch (_: NoMatchingViewException) { /*drawerLess layout*/
        }
    }

    private fun clickContextItem(@StringRes resId: Int) {
        clickContextItem(
            resId,
            composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS).onChildren().filterToOne(hasText("Test account 1")),
            onLongClick = true
        )
    }

    @Test
    fun editAccountFormIsOpened() {
        openDrawer()
        clickContextItem(R.string.menu_edit)
        intended(
            allOf(
                hasComponent(
                    AccountEdit::class.java.name
                ), hasExtraWithKey(DatabaseConstants.KEY_ROWID)
            )
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteConfirmationDialogDeleteButtonDeletes() {
        openDrawer()
        clickContextItem(R.string.menu_delete)
        onView(withText(dialogTitleWarningDeleteAccount))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(Matchers.`is`(getString(R.string.menu_delete)))
            )
        ).perform(click())
        assertDataSize(3)
        //after deletion of Account 1, Account 2 should still be selected
        checkTitle("Test account 2")
    }

    private val dialogTitleWarningDeleteAccount: String
        get() = getQuantityString(R.plurals.dialog_title_warning_delete_account, 1)

    @Test
    fun deleteConfirmationDialogCancelButtonCancels() {
        openDrawer()
        clickContextItem(R.string.menu_delete)
        onView(withText(dialogTitleWarningDeleteAccount))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(Matchers.`is`(getString(android.R.string.cancel)))
            )
        ).perform(click())
        assertDataSize(4)
    }

    @Test
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun deleteCorrectAccount() {
        val label1 = "Konto A"
        val label2 = "Konto B"
        val account1 = buildAccount(label1)
        val account2 = buildAccount(label2)

        //we try to delete account 1
        openDrawer()
        //we select  label2, but call context on label 1 and make sure the correct account is deleted
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS).onChildren().filter(hasText(label2))
            .onFirst().performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS).onChildren().filter(hasText(label1))
            .onFirst().performTouchInput {
                longClick()
            }
        composeTestRule.onNodeWithText(getString(R.string.menu_delete)).performClick()
        onView(
            ViewMatchers.withSubstring(
                getString(
                    R.string.warning_delete_account,
                    label1
                )
            )
        ).check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(R.string.menu_delete)
            )
        ).perform(click())
        assertThat(repository.loadAccount(account1.id)).isNull()
        assertThat(repository.loadAccount(account2.id)).isNotNull()
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }

    @Test
    fun templateScreenIsOpened() {
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        intended(
            hasComponent(ManageTemplates::class.java.name)
        )
    }

    @Test
    fun titleAndSubtitleAreSetAndSurviveOrientationChange() {
        checkTitle("Test account 2")
        doWithRotation {
            checkTitle("Test account 2")
        }
    }

    private fun checkTitle(label: String) {
        val currencyFormatter = app.appComponent.currencyFormatter()
        val balance = currencyFormatter.formatMoney(Money(homeCurrency, 0))
        toolbarMainTitle().check(matches(withText(label)))
        toolbarMainSubtitle().check(matches(withText(balance)))
    }
}