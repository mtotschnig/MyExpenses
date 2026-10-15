package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.preference.persistMenu
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanupSuspend

@TestShard1
class CustomizedMenuTest: BaseMyExpensesTest() {

    @Test
    fun startWithCustomizedMenu() = runTest {
        // App should not expect any item to be present
        dataStore.persistMenu(
            MenuItem.MenuContext.V2Navigation,
            listOf(MenuItem.Settings)
        )
        dataStore.persistMenu(
            MenuItem.MenuContext.V2Transactions,
            emptyList()
        )
        val account1 = buildAccount("Test account 1")
        try {
            launch(account1.id)
            composeTestRule.onNodeWithText(getString(R.string.no_expenses)).assertIsDisplayed()
        } finally {
            cleanupSuspend {
                repository.deleteAccount(account1.id)
                dataStore.edit {
                    it.remove(MenuItem.MenuContext.V2Navigation.prefKey)
                    it.remove(MenuItem.MenuContext.V2Transactions.prefKey)
                }
            }
        }
    }
}