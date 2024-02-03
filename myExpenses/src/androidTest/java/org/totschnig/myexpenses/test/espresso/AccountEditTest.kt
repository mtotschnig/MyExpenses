package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.getUuidForAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso.wait
import org.totschnig.myexpenses.testutils.Matchers.withListSize

class AccountEditTest : BaseUiTest<AccountEdit>() {
    @After

    @Test
    fun saveAccount() {
        val i = Intent(targetContext, AccountEdit::class.java)
        testScenario = ActivityScenario.launchActivityForResult(i)
        onView(withId(R.id.Currency)).check(matches(isDisplayed()))
        onView(withId(R.id.Currency)).perform(wait(withListSize(greaterThan(0)), 1000))
        onView(withId(R.id.Label)).perform(ViewActions.typeText(LABEL), closeSoftKeyboard())
        clickFab()
        assertFinishing()
        assertThat(repository.findAnyOpenByLabel(LABEL)).isNotNull
    }

    @Test
    fun shouldKeepUuidAfterSave() {
        val (id, uuid) =  with(buildAccount(LABEL)) {
            id to uuid
        }
        val i = Intent(targetContext, AccountEdit::class.java).apply {
            putExtra(DatabaseConstants.KEY_ROWID, id)
        }
        testScenario = ActivityScenario.launchActivityForResult(i)
        clickFab()
        assertThat(repository.getUuidForAccount(id)).isEqualTo(uuid)
    }

    companion object {
        private const val LABEL = "Test account"
    }
}