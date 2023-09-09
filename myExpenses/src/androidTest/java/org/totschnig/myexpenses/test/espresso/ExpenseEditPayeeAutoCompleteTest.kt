package org.totschnig.myexpenses.test.espresso

import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.requireParty


class ExpenseEditPayeeAutoCompleteTest: BaseExpenseEditTest() {

    @Before
    fun fixture() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        repository.requireParty("John")
        testScenario = ActivityScenario.launch(intentForNewTransaction)
    }

    @Test
    fun payeeFieldShouldShowSuggestions() {
        onView(ViewMatchers.withId(R.id.Payee)).perform(typeText("J"))
        onView(withText("John"))
            .inRoot(isPlatformPopup())
            .perform(click())
        //Auto Fill Dialog
        onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(Button::class.java),
                withText(R.string.response_yes)
            )
        ).perform(click())
        onView(ViewMatchers.withId(R.id.Payee)).check(matches(withText("John")))
    }
}