package org.totschnig.myexpenses.testutils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.test.espresso.SettingsTest
import org.totschnig.myexpenses.util.distrib.DistributionHelper.versionNumber
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil
import java.util.Locale


abstract class TestMain(locale: String?) : BaseMyExpensesTest() {

    @Rule
    @JvmField
    val chain: TestRule = RuleChain
        .outerRule(
            GrantPermissionRule.grant(
                *buildList {
                    add(Manifest.permission.WRITE_CALENDAR)
                    add(Manifest.permission.READ_CALENDAR)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    add(Manifest.permission.CHANGE_CONFIGURATION)
                }.toTypedArray()
            )
        )
        .around(
            locale?.let { LocaleTestRule(it) } ?: LocaleTestRule()
        )

    open val shouldTakeScreenShot = false


    @Before
    fun configureLocale() {
        LocaleUtil.localeFromString(LocaleUtil.getTestLocale())?.let {
            //targetContext.updateWith(it)
            testContext.updateWith(it)
        }
    }

    @After
    fun cleanUp() {
        app.fixture.cleanup(contentResolver)
    }

    fun runScenario(scenario: String) {
        loadFixture(scenario == "2")
        scenario(scenario)
    }

    private fun drawerAction(action: ViewAction) {
        //no drawer on w700dp
        try {
            onView(withId(R.id.drawer)).perform(action)
        } catch (_: NoMatchingViewException) {
        }
    }

    private fun scenario(scenario: String) {
        when (scenario) {
            "1" -> {
                drawerAction(DrawerActions.open())
                takeScreenshot("summarize")
                drawerAction(DrawerActions.close())
                takeScreenshot("group")
                clickMenuItem(R.id.RESET_COMMAND)
                closeSoftKeyboard()
                takeScreenshot("export")
                pressBack()
                clickContextItem(R.string.details)
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    //https://github.com/android/android-test/issues/444
                    Thread.sleep(500)
                }
                composeTestRule.onNodeWithText(getString(R.string.menu_edit)).performClick()
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
                listNode.onChildren()[0].performClick()
                doWithRotation {
                    onIdle()
                    //wait for sum to load IdlingResource is too cumbersome to set up, since
                    //onActivity does not get us hold on BudgetActivity
                    Thread.sleep(500)
                    takeScreenshot("budget")
                }
                onIdle()
                Thread.sleep(500)
                pressBack()
                pressBack()
                clickMenuItem(R.id.SETTINGS_COMMAND)
                SettingsTest.navigateTo(
                    R.string.synchronization,
                    R.string.pref_manage_sync_backends_title
                )
                onView(withText(containsString("Drive"))).perform(click())
                onView(withText(containsString("Dropbox"))).perform(click())
                onView(withText(containsString("WebDAV"))).perform(scrollTo(), click())
                if(shouldTakeScreenShot) {
                    Thread.sleep(5000)
                }
                takeScreenshot("sync")
            }

            "2" -> {
                //tablet screenshots
                takeScreenshot("main")
                clickMenuItem(R.id.DISTRIBUTION_COMMAND)
                takeScreenshot("distribution")
                pressBack()
                listNode.onChildren()
                    .filter(
                        hasText(
                            testContext.getString(org.totschnig.myexpenses.test.R.string.testData_transaction1SubCat),
                            substring = true
                        )
                    )
                    .onFirst()
                    .performClick()
                composeTestRule.onNodeWithText(getString(R.string.menu_edit)).performClick()
                pressBack() //close keyboard
                onView(withPositionInParent(R.id.AttachmentGroup, 0))
                    .perform(click())
                takeScreenshot("edit")
            }

            else -> {
                throw IllegalArgumentException("Unknown scenario" + BuildConfig.TEST_SCENARIO)
            }
        }
    }

    private fun loadFixture(withPicture: Boolean) {
        unlock()
        app.fixture.setup(withPicture, repository, app.appComponent.plannerUtils(), homeCurrency)
        prefHandler.putInt(PrefKey.CURRENT_VERSION, versionNumber)
        prefHandler.putInt(PrefKey.FIRST_INSTALL_VERSION, versionNumber)
        launch(app.fixture.account1.id)
    }

    private fun Context.updateWith(locale: Locale) {
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun takeScreenshot(fileName: String) {
        if (shouldTakeScreenShot) {
            onIdle()
            Screengrab.screenshot(fileName)
        }
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
    }
}