package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ViewPagerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.viewpager2.widget.ViewPager2
import com.adevinta.android.barista.assertion.BaristaAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.util.Utils

@TestShard4
class OnboardingTest : BaseUiTest<OnboardingActivity>() {

    private lateinit var viewPager2IdlingResource: ViewPager2IdlingResource

    @Before
    fun launch() {
        testScenario =
            ActivityScenario.launch(Intent(targetContext, OnboardingActivity::class.java))
        testScenario.onActivity {
            viewPager2IdlingResource =
                ViewPager2IdlingResource(it.findViewById(R.id.viewPager), "viewPagerIdlingResource")
            IdlingRegistry.getInstance().register(viewPager2IdlingResource)
        }
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(viewPager2IdlingResource)
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
        BaristaVisibilityAssertions.assertDisplayed(
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
    }
}

//https://stackoverflow.com/a/64931776/1199911
class ViewPager2IdlingResource(viewPager: ViewPager2, name: String) : IdlingResource {

    private val name: String
    private var isIdle = true // Default to idle since we can't query the scroll state.
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    init {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                isIdle = (state == ViewPager2.SCROLL_STATE_IDLE // Treat dragging as idle, or Espresso will block itself when swiping.
                        || state == ViewPager2.SCROLL_STATE_DRAGGING)
                if (isIdle && resourceCallback != null) {
                    resourceCallback!!.onTransitionToIdle()
                }
            }
        })
        this.name = name
    }

    override fun getName(): String {
        return name
    }

    override fun isIdleNow(): Boolean {
        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }
}