package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
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
        Espresso.onView(ViewMatchers.withId(R.id.Currency)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.Currency)).perform(wait(withListSize(Matchers.greaterThan(0)), 1000))
        Espresso.onView(ViewMatchers.withId(R.id.Label)).perform(ViewActions.typeText(LABEL), closeSoftKeyboard())
        Espresso.onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
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
        Espresso.onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
        assertThat(repository.getUuidForAccount(id)).isEqualTo(uuid)
    }

    companion object {
        private const val LABEL = "Test account"
    }
}