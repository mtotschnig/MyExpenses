package org.totschnig.myexpenses.test.espresso

import android.content.OperationApplicationException
import android.os.RemoteException
import android.widget.Button
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.compose.TEST_TAG_ACCOUNTS
import org.totschnig.myexpenses.compose.TEST_TAG_BALANCE_HEADER
import org.totschnig.myexpenses.compose.TEST_TAG_DELETE_ACCOUNT
import org.totschnig.myexpenses.compose.TEST_TAG_EDIT_ACCOUNT
import org.totschnig.myexpenses.compose.TEST_TAG_FAB_MENU
import org.totschnig.myexpenses.compose.TEST_TAG_OVERFLOW_MENU
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.dialog.MenuItem.Templates
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowMenu
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withIdAndParent

@TestShard4
class MyExpensesTest : BaseMyExpensesTest() {
    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var account3: Account

    @Before
    fun fixture() {
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
        composeTestRule.onNodeWithTag(TEST_TAG_FAB_MENU).performClick()
        intended(hasComponent(ExpenseEdit::class.java.name))
        pressBack()
    }

    @Test
    fun newBalanceOpensForm() {
        composeTestRule.onNodeWithTag(TEST_TAG_BALANCE_HEADER).performClick()
        composeTestRule.onNodeWithText(getString(R.string.new_balance)).performClick()
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
        TODO()
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
        selectNavigationItem(MenuItem.Settings.testTag)
        intended(
            hasComponent(PreferenceActivity::class.java.name)
        )
    }

    fun assertMenuItemHidden(vararg menuItems: MenuItem) {
        val overflowNodes =
            composeTestRule.onAllNodesWithTag(TEST_TAG_OVERFLOW_MENU).fetchSemanticsNodes()
        if (overflowNodes.isNotEmpty()) {
            composeTestRule.onNodeWithTag(TEST_TAG_OVERFLOW_MENU).performClick()

            menuItems.forEach {
                composeTestRule.onNodeWithTag(it.testTag).assertDoesNotExist()
            }

            pressBack()
        }
    }

    @Test
    fun inActiveItemsAreHidden() {
        assertMenuItemHidden(MenuItem.Reset, MenuItem.Distribution, MenuItem.Print)
    }

    @Test
    fun newAccountShowNew() {
        navigateToAccounts()
        composeTestRule.onNodeWithTag(TEST_TAG_FAB_MENU).performClick()
        onView(withText(R.string.menu_create_account))
            .perform(click())
        intended(
            allOf(
                hasComponent(AccountEdit::class.java.name),
                not(hasExtraWithKey(KEY_ROWID))
            )
        )
        onView(withId(R.id.Label)).perform(
            typeText("A"),
            closeSoftKeyboard()
        )
        super.clickFab()
        navigateToTransactions()
        checkTitle("A")
        cleanup {
            deleteAccount("A")
        }
    }

    @Test
    fun editAccountFormIsOpened() {
        navigateToAccounts()
        clickContextItem(TEST_TAG_EDIT_ACCOUNT)
        intended(
            allOf(
                hasComponent(
                    AccountEdit::class.java.name
                ), hasExtraWithKey(KEY_ROWID)
            )
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteConfirmationDialogDeleteButtonDeletes() {
        navigateToAccounts()
        clickContextItem(TEST_TAG_DELETE_ACCOUNT)
        onView(withText(dialogTitleWarningDeleteAccount))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(Matchers.`is`(getString(R.string.menu_delete)))
            )
        ).perform(click())
        navigateToTransactions()
        assertDataSize(3)
        //after deletion of Account 1, Account 2 should still be selected
        checkTitle("Test account 2")
    }

    private val dialogTitleWarningDeleteAccount: String
        get() = getQuantityString(R.plurals.dialog_title_warning_delete_account, 1, 1)

    @Test
    fun deleteConfirmationDialogCancelButtonCancels() {
        navigateToAccounts()
        clickContextItem(TEST_TAG_DELETE_ACCOUNT)
        onView(withText(dialogTitleWarningDeleteAccount))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(Matchers.`is`(getString(android.R.string.cancel)))
            )
        ).perform(click())
        navigateToTransactions()
        assertDataSize(4)
    }

    private fun clickContextItem(testTag: String, accountLabel: String = "Test account 1") {
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS, useUnmergedTree = true)
            .onChildren()
            .filterToOne(hasAnyDescendant(hasText(accountLabel)))
            .performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_OVERFLOW_MENU)
            .performClick()
        composeTestRule.onNodeWithTag(testTag).performClick()
    }

    @Test
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun deleteCorrectAccount() {
        val label1 = "Konto A"
        val label2 = "Konto B"
        val account1 = buildAccount(label1)
        val account2 = buildAccount(label2)

        navigateToAccounts()

        //we try to delete account 1
        //we select  label2, but call context on label 1 and make sure the correct account is deleted
        val caretDescription = getString(R.string.import_select_transactions)

        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNTS)
            .onChildren()
            .filter(hasAnyDescendant(hasText(label2)))
            .onFirst()
            .onChildren()
            .onFirst()
            .onChildren()
            .onFirst()
            .performClick()

        navigateToAccounts()
        clickContextItem(TEST_TAG_DELETE_ACCOUNT, label1)
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
        composeTestRule.onNodeWithTag(Templates.testTag).performClick()
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
}