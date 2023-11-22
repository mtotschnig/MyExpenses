package org.totschnig.myexpenses.test.espresso

import android.content.OperationApplicationException
import android.os.RemoteException
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
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
import org.totschnig.myexpenses.util.formatMoney

class MyExpensesTest : BaseMyExpensesTest() {
    lateinit var account: Account
    @Before
    fun fixture() {
        prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, true)
        account =  buildAccount("Test account 1")
        launch(account.id)
        Intents.init()
    }

    @After
    override fun tearDown() {
        Intents.release()
        repository.deleteAccount(account.id)
    }

    @Test
    fun viewPagerIsSetup() {
        composeTestRule.onNodeWithText(getString(R.string.no_expenses)).assertIsDisplayed()
        assertDataSize(1)
    }

    @Test
    fun floatingActionButtonOpensForm() {
        Espresso.onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
        Intents.intended(
            IntentMatchers.hasComponent(
                ExpenseEdit::class.java.name
            )
        )
    }

    @Test
    fun helpDialogIsOpened() {
        openActionBarOverflowMenu()
        Espresso.onData(Matchers.hasToString(getString(R.string.menu_help)))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(Matchers.containsString(getString(R.string.help_MyExpenses_title))))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(Button::class.java),
                ViewMatchers.withText(Matchers.`is`(app.getString(android.R.string.ok)))
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun settingsScreenIsOpened() {
        openActionBarOverflowMenu()
        Espresso.onData(Matchers.hasToString(getString(R.string.settings_label)))
            .perform(ViewActions.click())
        Intents.intended(
            IntentMatchers.hasComponent(
                PreferenceActivity::class.java.name
            )
        )
    }

    @Test
    fun inActiveItemsOpenDialog() {
        testInActiveItemHelper(
            R.id.RESET_COMMAND,
            R.string.dialog_command_disabled_reset_account
        )
        testInActiveItemHelper(
            R.id.DISTRIBUTION_COMMAND,
            R.string.dialog_command_disabled_distribution
        )
        testInActiveItemHelper(
            R.id.PRINT_COMMAND,
            R.string.dialog_command_disabled_reset_account
        )
    }

    /**
     * Call a menu item and verify that a message is shown in dialog
     */
    private fun testInActiveItemHelper(menuItemId: Int, messageResId: Int) {
        clickMenuItem(menuItemId)
        Espresso.onView(ViewMatchers.withText(messageResId))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun newAccountFormIsOpened() {
        openDrawer()
        Espresso.onView(ViewMatchers.withId(R.id.expansionTrigger)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.menu_create_account))
            .perform(ViewActions.click())
        Intents.intended(
            Matchers.allOf(
                IntentMatchers.hasComponent(
                    AccountEdit::class.java.name
                ),
                Matchers.not(IntentMatchers.hasExtraWithKey(DatabaseConstants.KEY_ROWID))
            )
        )
    }

    private fun openDrawer() {
        try {
            Espresso.onView(ViewMatchers.withId(R.id.drawer)).perform(DrawerActions.open())
        } catch (e: NoMatchingViewException) { /*drawerLess layout*/
        }
    }

    private fun clickContextItem(@StringRes resId: Int, position: Int = 1) {
        clickContextItem(resId, composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS), position, onLongClick = true)
    }

    @Test
    fun editAccountFormIsOpened() {
        openDrawer()
        clickContextItem(R.string.menu_edit)
        Intents.intended(
            Matchers.allOf(
                IntentMatchers.hasComponent(
                    AccountEdit::class.java.name
                ), IntentMatchers.hasExtraWithKey(DatabaseConstants.KEY_ROWID)
            )
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteConfirmationDialogDeleteButtonDeletes() {
        openDrawer()
        clickContextItem(R.string.menu_delete)
        Espresso.onView(ViewMatchers.withText(dialogTitleWarningDeleteAccount))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(Button::class.java),
                ViewMatchers.withText(Matchers.`is`(getString(R.string.menu_delete)))
            )
        ).perform(ViewActions.click())
        assertDataSize(0)
    }

    private val dialogTitleWarningDeleteAccount: String
        get() = getQuantityString(R.plurals.dialog_title_warning_delete_account, 1)

    @Test
    fun deleteConfirmationDialogCancelButtonCancels() {
        openDrawer()
        clickContextItem(R.string.menu_delete)
        Espresso.onView(ViewMatchers.withText(dialogTitleWarningDeleteAccount))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(Button::class.java),
                ViewMatchers.withText(Matchers.`is`(getString(android.R.string.cancel)))
            )
        ).perform(ViewActions.click())
        assertDataSize(1)
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
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS).onChildren().filter(hasText(label2)).onFirst().performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS).onChildren().filter(hasText(label1)).onFirst().performTouchInput {
            longClick()
        }
        composeTestRule.onNodeWithText(getString(R.string.menu_delete)).performClick()
        Espresso.onView(
            ViewMatchers.withSubstring(
                getString(
                    R.string.warning_delete_account,
                    label1
                )
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(Button::class.java),
                ViewMatchers.withText(R.string.menu_delete)
            )
        ).perform(ViewActions.click())
        Truth.assertThat(repository.loadAccount(account1.id)).isNull()
        Truth.assertThat(repository.loadAccount(account2.id)).isNotNull()
    }

    @Test
    fun templateScreenIsOpened() {
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        Intents.intended(
            IntentMatchers.hasComponent(
                ManageTemplates::class.java.name
            )
        )
    }

    @Test
    fun titleAndSubtitleAreSetAndSurviveOrientationChange() {
        checkTitle()
        rotate()
        checkTitle()
    }

    private fun checkTitle() {
        val currencyFormatter = app.appComponent.currencyFormatter()
        val balance = currencyFormatter.formatMoney(Money(homeCurrency, 0))
        Espresso.onView(
            Matchers.allOf(
                CoreMatchers.instanceOf(
                    TextView::class.java
                ),
                ViewMatchers.withParent(ViewMatchers.withId(R.id.toolbar)),
                ViewMatchers.withText("Test account 1")
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                CoreMatchers.instanceOf(
                    TextView::class.java
                ),
                ViewMatchers.withParent(ViewMatchers.withId(R.id.toolbar)),
                ViewMatchers.withText(balance)
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}