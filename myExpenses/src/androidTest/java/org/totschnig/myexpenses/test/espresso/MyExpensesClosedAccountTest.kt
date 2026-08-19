package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_FAB_TRANSACTIONS
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.setAccountProperty
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup

@TestShard4
class MyExpensesClosedAccountTest : BaseMyExpensesTest() {
    private lateinit var account1: Account

    @Before
    fun fixture() {
        account1 = buildAccount("Test account 1")
        repository.setAccountProperty(account1.id, KEY_SEALED, true)
        launch(account1.id)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
        }
    }

    @Test
    fun closedAccountFabIsDisabled () {
        Intents.init()
        try {
            composeTestRule.onNodeWithTag(TEST_TAG_FAB_TRANSACTIONS)
                .assertIsDisplayed()
                .assertIsNotEnabled()
                .assert(hasContentDescription(getString(R.string.account_closed)))
                .performClick()
            assertThat(Intents.getIntents()).isEmpty()
        } finally {
            Intents.release()
        }
    }
}