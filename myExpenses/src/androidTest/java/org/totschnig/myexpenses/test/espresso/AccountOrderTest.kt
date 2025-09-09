package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Before
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountFlag
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.model.PREDEFINED_NAME_INACTIVE
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import kotlin.test.Test

@TestShard1

class AccountOrderTest : BaseMyExpensesTest() {
    private lateinit var accounts: List<TestAccount>
    private lateinit var hiddenAccount: TestAccount

    data class TestAccount(
        val label: String,
        val lastUsed: Long,
        val usages: Int,
        val customSort: Int,
        val id: Long = 0,
        val visible: Boolean = true
    )

    @Before
    fun fixture() {
        prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, true)
        accounts = listOf(
            TestAccount("Test account 1", 1L, 2, 3).buildAccount(),
            TestAccount("Test account 2", 3L, 1, 1).buildAccount(),
            TestAccount("Test account 3", 2L, 3, 2).buildAccount()
        )
        hiddenAccount = TestAccount("Test account 4", 0L, 0, 0, visible = false).buildAccount()
    }

    private fun TestAccount.buildAccount(): TestAccount {
        val dbAccount = buildAccount(label).also {
            repository.updateAccount(it.id) {
                put(KEY_LAST_USED, lastUsed)
                put(KEY_USAGES, usages)
                put(KEY_SORT_KEY, customSort)
                if (!visible) {
                    put(KEY_FLAG, repository.findAccountFlag(PREDEFINED_NAME_INACTIVE)!!.id)
                }
            }
        }
        return copy(id=dbAccount.id)
    }

    @After
    fun clearDb() {
        cleanup {
            accounts.forEach { repository.deleteAccount(it.id) }
            repository.deleteAccount(hiddenAccount.id)
            prefHandler.remove(PrefKey.SORT_ORDER_ACCOUNTS)
        }
    }

    @Test
    fun sortByLabel() {
        doTheTest(Sort.LABEL, accounts)
    }

    @Test
    fun sortByUsages() {
        doTheTest(Sort.USAGES, accounts.sortedByDescending { it.usages })
    }

    @Test
    fun sortByLastUsed() {
        doTheTest(Sort.LAST_USED, accounts.sortedByDescending { it.lastUsed })
    }

    @Test
    fun sortByCustom() {
        doTheTest(Sort.CUSTOM, accounts.sortedBy { it.customSort })
    }


    private fun doTheTest(sort: Sort, expectedOrder: List<TestAccount>) {
        prefHandler.putString(PrefKey.SORT_ORDER_ACCOUNTS, sort.name)
        launch()
        expectedOrder.forEachIndexed { index, account->
            composeTestRule.onNodeWithTag(TEST_TAG_PAGER)
                .performScrollToIndex(index)
            onView(withText(account.label)).check(matches(isDisplayed()))
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PAGER)
            .performScrollToIndex(3)
        onView(withText(homeCurrency.code)).check(matches(isDisplayed()))
    }
}