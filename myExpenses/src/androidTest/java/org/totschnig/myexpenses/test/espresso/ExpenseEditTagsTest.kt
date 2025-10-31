package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest

private const val TAG_LABEL ="Wichtig"

class ExpenseEditTagsTest: BaseExpenseEditTest() {

    var tagId: Long = 0

    fun fixture() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        tagId = repository.writeTag(TAG_LABEL)
        launch()
    }

    @Test
    fun shouldSaveTags() {
        runTest {
            fixture()
            setAmount(101)
            onView(withId(R.id.TagSelection)).perform(click())
            onView(withText(TAG_LABEL)).perform(click())
            clickFab() // confirm tag selection
            clickFab() // save transaction
            assertTransaction(
                id = repository.loadTransactions(account1.id).first().id,
                expectedAccount = account1.id,
                expectedAmount = -10100,
                expectedTags = listOf(TAG_LABEL)
            )
        }
    }
}