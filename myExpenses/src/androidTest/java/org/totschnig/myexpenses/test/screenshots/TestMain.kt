package org.totschnig.myexpenses.test.screenshots

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.instanceOf
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.MockLicenceHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.versionNumber
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil

/**
 * This test is meant to be run with FastLane Screengrab, but also works on its own.
 */
class TestMain : BaseMyExpensesTest() {
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )

    @After
    fun cleanUp() {
        app.fixture.cleanup()
    }

    @Test
    fun mkScreenShots() {
        loadFixture(BuildConfig.TEST_SCENARIO == 2)
        scenario()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    private fun drawerAction(action: ViewAction) {
        //no drawer on w700dp
        try {
            onView(withId(R.id.drawer)).perform(action)
        } catch (ignored: NoMatchingViewException) {
        }
    }

    private fun scenario() {
        Thread.sleep(500)
        when (BuildConfig.TEST_SCENARIO) {
            1 -> {
                drawerAction(DrawerActions.open())
                takeScreenshot("summarize")
                drawerAction(DrawerActions.close())
                takeScreenshot("group")
                clickMenuItem(R.id.RESET_COMMAND)
                closeSoftKeyboard()
                takeScreenshot("export")
                pressBack()
                listNode.onChildren().onFirst()
                    .assertTextContains(getString(R.string.split_transaction), substring = true)
                clickContextItem(R.string.details)
                onView(withId(android.R.id.button1)).perform(click())
                closeSoftKeyboard()
                takeScreenshot("split")
                pressBack()
                clickMenuItem(R.id.DISTRIBUTION_COMMAND)
                takeScreenshot("distribution")
                pressBack()
                clickMenuItem(R.id.HISTORY_COMMAND)
                clickMenuItem(R.id.GROUPING_COMMAND)
                onView(withText(R.string.grouping_month)).perform(click())
                clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)
                takeScreenshot("history")
                pressBack()
                clickMenuItem(R.id.BUDGET_COMMAND)
                onView(withId(R.id.recycler_view))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                val device = UiDevice.getInstance(getInstrumentation())
                device.setOrientationRight()
                onIdle()
                //wait for sum to load IdlingResource is too cumbersome to set up, since
                //onActivity does not get us hold on BudgetActivity
                Thread.sleep(500)
                takeScreenshot("budget")
                device.setOrientationNatural()
                onIdle()
                pressBack()
                pressBack()
                clickMenuItem(R.id.SETTINGS_COMMAND)
                onView(instanceOf(RecyclerView::class.java))
                    .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                            hasDescendant(withText(R.string.synchronization)),
                            click()
                        )
                    )
                onView(instanceOf(RecyclerView::class.java))
                    .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                            hasDescendant(withText(R.string.pref_manage_sync_backends_title)),
                            click()
                        )
                    )
                onView(withText(containsString("Drive"))).perform(click())
                onView(withText(containsString("Dropbox"))).perform(click())
                onView(withText(containsString("WebDAV"))).perform(scrollTo(), click())
                Thread.sleep(5000)
                takeScreenshot("sync")
            }
            2 -> {
                //tablet screenshots
                takeScreenshot("main")
                clickMenuItem(R.id.DISTRIBUTION_COMMAND)
                takeScreenshot("distribution")
                pressBack()
                onView(
                    org.totschnig.myexpenses.testutils.Matchers.first(
                        withText(
                            containsString(testContext.getString(org.totschnig.myexpenses.test.R.string.testData_transaction1SubCat))
                        )
                    )
                ).perform(click())
                onView(withId(android.R.id.button1))
                    .perform(click())
                pressBack() //close keyboard
                onView(withId(R.id.PictureContainer))
                    .perform(click())
                takeScreenshot("edit")
            }
            else -> {
                throw IllegalArgumentException("Unknown scenario" + BuildConfig.TEST_SCENARIO)
            }
        }
    }

    private fun loadFixture(withPicture: Boolean) {
        //LocaleTestRule only configure for app context, fixture loads resources from instrumentation context
        LocaleUtil.localeFromString(LocaleUtil.getTestLocale())?.let { configureLocale(it) }
        (app.licenceHandler as MockLicenceHandler).setLockState(false)
        app.fixture.setup(withPicture, repository, homeCurrency)
        prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, app.fixture.account1.id)
        prefHandler.putInt(PrefKey.CURRENT_VERSION, versionNumber)
        prefHandler.putInt(PrefKey.FIRST_INSTALL_VERSION, versionNumber)
        val startIntent = Intent(app, TestMyExpenses::class.java)
        testScenario = ActivityScenario.launch(startIntent)
    }

    private fun takeScreenshot(fileName: String) {
        onIdle()
        Screengrab.screenshot(fileName)
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            CleanStatusBar.enableWithDefaults()
        }

        @JvmStatic
        @AfterClass
        fun afterAll() {
            CleanStatusBar.disable()
        }

        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }
}