package org.totschnig.myexpenses.test.screenshots

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers
import org.junit.*
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.MockLicenceHandler
import org.totschnig.myexpenses.util.Utils
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

    @Test
    fun mkScreenShots() {
        loadFixture(BuildConfig.TEST_SCENARIO == 2)
        scenario()
    }

    private fun drawerAction(action: ViewAction) {
        //no drawer on w700dp
        try {
            Espresso.onView(ViewMatchers.withId(R.id.drawer)).perform(action)
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
                Espresso.closeSoftKeyboard()
                takeScreenshot("export")
                Espresso.pressBack()
                listNode.onChildren().onFirst()
                    .assertTextContains(getString(R.string.split_transaction), substring = true)
                clickContextItem(R.string.details)
                Espresso.closeSoftKeyboard()
                takeScreenshot("split")
                Espresso.pressBack()
                clickMenuItem(R.id.DISTRIBUTION_COMMAND)
                takeScreenshot("distribution")
                Espresso.pressBack()
                clickMenuItem(R.id.HISTORY_COMMAND)
                clickMenuItem(R.id.GROUPING_COMMAND)
                Espresso.onView(ViewMatchers.withText(R.string.grouping_month))
                    .perform(ViewActions.click())
                clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)
                takeScreenshot("history")
                Espresso.pressBack()
                clickMenuItem(R.id.BUDGET_COMMAND)
                Espresso.onView(ViewMatchers.withId(R.id.recycler_view))
                    .perform(
                        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                            0,
                            ViewActions.click()
                        )
                    )
                takeScreenshot("budget")
                Espresso.pressBack()
                Espresso.pressBack()
                clickMenuItem(R.id.SETTINGS_COMMAND)
                Espresso.onView(
                    Matchers.instanceOf(
                        RecyclerView::class.java
                    )
                )
                    .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                            ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.synchronization)),
                            ViewActions.click()
                        )
                    )
                Espresso.onView(
                    Matchers.instanceOf(
                        RecyclerView::class.java
                    )
                )
                    .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                            ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.pref_manage_sync_backends_title)),
                            ViewActions.click()
                        )
                    )
                Espresso.onView(ViewMatchers.withText(Matchers.containsString("Drive")))
                    .perform(ViewActions.click())
                Espresso.onView(ViewMatchers.withText(Matchers.containsString("Dropbox")))
                    .perform(ViewActions.click())
                Espresso.onView(ViewMatchers.withText(Matchers.containsString("WebDAV")))
                    .perform(ViewActions.click())
                Thread.sleep(5000)
                takeScreenshot("sync")
            }
            2 -> {
                //tablet screenshots
                takeScreenshot("main")
                clickMenuItem(R.id.DISTRIBUTION_COMMAND)
                takeScreenshot("distribution")
                Espresso.pressBack()
                Espresso.onView(
                    org.totschnig.myexpenses.testutils.Matchers.first(
                        ViewMatchers.withText(
                            Matchers.containsString(testContext.getString(org.totschnig.myexpenses.test.R.string.testData_transaction1SubCat))
                        )
                    )
                ).perform(ViewActions.click())
                Espresso.onView(ViewMatchers.withId(android.R.id.button1))
                    .perform(ViewActions.click())
                Espresso.pressBack() //close keyboard
                Espresso.onView(ViewMatchers.withId(R.id.PictureContainer))
                    .perform(ViewActions.click())
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
        prefHandler.putString(PrefKey.HOME_CURRENCY, Utils.getSaveDefault().currencyCode)
        (app.licenceHandler as MockLicenceHandler).setLockState(false)
        app.fixture.setup(withPicture, repository)
        prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, app.fixture.account1.id)
        prefHandler.putInt(PrefKey.CURRENT_VERSION, versionNumber)
        prefHandler.putInt(PrefKey.FIRST_INSTALL_VERSION, versionNumber)
        val startIntent = Intent(app, TestMyExpenses::class.java)
        activityScenario = ActivityScenario.launch(startIntent)
    }

    private fun takeScreenshot(fileName: String) {
        Espresso.onIdle()
        Screengrab.screenshot(fileName)
    }

    companion object {
        @BeforeClass
        fun beforeAll() {
            CleanStatusBar.enableWithDefaults()
        }

        @AfterClass
        fun afterAll() {
            CleanStatusBar.disable()
        }

        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }
}