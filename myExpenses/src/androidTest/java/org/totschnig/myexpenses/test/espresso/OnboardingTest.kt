package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.util.Utils

class OnboardingTest : BaseUiTest<OnboardingActivity>() {

    @Before
    fun launch() {
        testScenario =
            ActivityScenario.launch(Intent(targetContext, OnboardingActivity::class.java))
    }

    @Test
    fun navigateSwipe() {
        checkPage(0)
        swipeNext()
        checkPage(1)
        swipeNext()
        checkPage(2)
        swipePrevious()
        checkPage(1)
        swipePrevious()
        checkPage(0)
    }

    @Test
    fun navigateButtonsAndBackKey() {
        checkPage(0)
        clickNextButton()
        checkPage(1)
        clickNextButton()
        checkPage(2)
        pressBack()
        checkPage(1)
        pressBack()
        checkPage(0)
    }

    @Test
    fun getStarted() {
        Intents.init()
        checkPage(0)
        clickNextButton()
        checkPage(1)
        clickNextButton()
        checkPage(2)
        clickButton(R.id.suw_navbar_done)
        Intents.intended(
            IntentMatchers.hasComponent(
                MyExpenses::class.java.name
            )
        )
        Intents.release()
    }

    private fun swipeNext() {
        onView(withId(R.id.viewPager)).perform(swipeLeft())
    }

    private fun swipePrevious() {
        onView(withId(R.id.viewPager)).perform(swipeRight())
    }

    private fun clickNextButton() {
        clickButton(R.id.suw_navbar_next)
    }

    private fun clickButton(id: Int) {
        onView(allOf(withId(id), isDisplayed())).perform(click())
    }

    private fun checkPage(page: Int) {
        onView(
            withText(
                when (page) {
                    0 -> Utils.getTextWithAppName(
                        targetContext, R.string.onboarding_ui_title
                    ).toString()

                    1 -> getString(R.string.onboarding_privacy_title)
                    else -> getString(R.string.onboarding_data_title)
                }
            )
        )
            .check(matches(isDisplayed()))
    }
}
