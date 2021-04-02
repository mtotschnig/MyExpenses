package org.totschnig.myexpenses.test.espresso

import android.app.Activity
import android.content.Intent
import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.testutils.BaseUiTest

class AccountEditTest : BaseUiTest() {
    private lateinit var activityScenario: ActivityScenario<AccountEdit>
    @After
    @Throws(RemoteException::class, OperationApplicationException::class)
    fun tearDown() {
        val accountId = Account.findAnyOpen(LABEL)
        if (accountId > -1) {
            Account.delete(accountId)
        }
    }

    @Test
    fun saveAccount() {
        val i = Intent(targetContext, AccountEdit::class.java)
        activityScenario = ActivityScenario.launch(i)
        Espresso.onView(ViewMatchers.withId(R.id.Currency)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.Currency)).perform(org.totschnig.myexpenses.testutils.Espresso.wait(org.totschnig.myexpenses.testutils.Matchers.withListSize(Matchers.greaterThan(0)), 1000))
        Espresso.onView(ViewMatchers.withId(R.id.Label)).perform(ViewActions.typeText(LABEL))
        Espresso.onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
        assertFinishing()
        assertThat(Account.findAnyOpen(LABEL) > -1).isTrue
    }

    override val testScenario: ActivityScenario<out ProtectedFragmentActivity?>
        get() = activityScenario

    companion object {
        private const val LABEL = "Test account"
    }
}