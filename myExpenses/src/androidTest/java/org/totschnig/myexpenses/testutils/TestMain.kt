package org.totschnig.myexpenses.testutils

import android.os.Build
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_ACCOUNTS
import org.totschnig.myexpenses.compose.TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.test.espresso.SettingsTest
import org.totschnig.myexpenses.util.distrib.DistributionHelper.versionNumber
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule


abstract class TestMain(locale: String?) : BaseMyExpensesTest() {

    @Rule
    @JvmField
    val chain: TestRule = RuleChain
        .outerRule(buildGrantPermissionRule())
        .around(
            locale?.let { LocaleTestRule(it) } ?: LocaleTestRule()
        )

    open val shouldTakeScreenShot = false

    @After
    fun cleanUp() {
        app.fixture.cleanup()
    }

    fun runScenario(scenario: String) {
        loadFixture(scenario == "2")
        scenario(scenario)
    }

    private fun scenario(scenario: String) {
        when (scenario) {
            "1" -> {
                navigateToAccounts()
                //Expand Portfolio
                composeTestRule.onNode(
                    hasText(getString(R.string.Sub_2_1)) and
                            hasAnyAncestor(hasTestTag(TEST_TAG_ACCOUNTS))
                ).performClick()
                if (shouldTakeScreenShot) {
                    Thread.sleep(1000)
                }
                takeScreenshot("2_summarize")
                if (!isLarge) {
                    navigateToTransactions()
                    if (shouldTakeScreenShot) {
                        Thread.sleep(1000)
                    }
                    takeScreenshot("1_group")
                }
                clickMenuItemOverflowCompose(
                    MenuItem.Reset.testTag,
                    menuTestTag = TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
                )
                composeTestRule.waitForIdle()
                try {
                    closeSoftKeyboard()
                } catch (_: Exception) {
                }
                takeScreenshot("7_export")
                pressBack()
                clickContextItem(R.string.details)
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    //https://github.com/android/android-test/issues/444
                    Thread.sleep(500)
                }
                composeTestRule.onNodeWithText(getString(R.string.menu_edit)).performClick()
                closeSoftKeyboard()
                takeScreenshot("6_split")
                pressBack()
                clickMenuItemOverflowCompose(
                    MenuItem.Distribution.testTag,
                    menuTestTag = TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
                )
                takeScreenshot("3_distribution")
                pressBack()
                clickMenuItemOverflowCompose(
                    MenuItem.History.testTag,
                    menuTestTag = TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
                )
                clickMenuItem(R.id.GROUPING_COMMAND)
                onView(withText(R.string.grouping_month)).perform(click())
                clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)
                takeScreenshot("4_history")
                pressBack()
                selectNavigationItem(MenuItem.Budget.testTag)
                listNode.onChildren()[0].performClick()
                if (isLarge) {
                    takeScreenshot("5_budget")
                } else {
                    doWithRotation {
                        onIdle()
                        //wait for sum to load IdlingResource is too cumbersome to set up, since
                        //onActivity does not get us hold on BudgetActivity
                        Thread.sleep(500)
                        takeScreenshot("5_budget")
                    }
                    onIdle()
                    Thread.sleep(500)
                }
                pressBack()
                pressBack()
                selectNavigationItem(MenuItem.Settings.testTag)
                SettingsTest.navigateTo(
                    R.string.synchronization,
                    R.string.pref_manage_sync_backends_title
                )
                onView(withText(containsString("Drive"))).perform(click())
                onView(withText(containsString("Dropbox"))).perform(click())
                onView(withText(containsString("WebDAV"))).perform(scrollTo(), click())
                if (shouldTakeScreenShot) {
                    Thread.sleep(5000)
                }
                takeScreenshot("8_sync")
            }

            "2" -> {
                //tablet screenshots
                //Expand Portfolio
                composeTestRule.onNodeWithText(getString(R.string.Sub_2_1)).performClick()
                if (shouldTakeScreenShot) {
                    Thread.sleep(1000)
                }
                takeScreenshot("1_summarize")
                clickMenuItemOverflowCompose(
                    MenuItem.Distribution.testTag,
                    menuTestTag = TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
                )
                if (shouldTakeScreenShot) {
                    Thread.sleep(1000)
                }
                takeScreenshot("3_distribution")
                pressBack()
                clickMenuItemOverflowCompose(
                    MenuItem.History.testTag,
                    menuTestTag = TEST_TAG_OVERFLOW_MENU_TRANSACTIONS
                )
                clickMenuItem(R.id.GROUPING_COMMAND)
                onView(withText(R.string.grouping_month)).perform(click())
                clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)
                takeScreenshot("4_history")
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
                takeScreenshot("2_edit")
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

    private fun takeScreenshot(fileName: String) {
        if (shouldTakeScreenShot) {
            onIdle()
            Screengrab.screenshot(fileName)
        }
    }

    companion object {

        // Replace CleanStatusBar.enableWithDefaults() in TestMain.kt with:
        private fun enableCleanStatusBar() {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val commands = listOf(
                "settings put global sysui_demo_allowed 1",
                "am broadcast -a com.android.systemui.demo -e command enter",
                "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200",
                "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false",
                "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4",
                "am broadcast -a com.android.systemui.demo -e command status -e bluetooth hide",
                "am broadcast -a com.android.systemui.demo -e command notifications -e visible false"
            )
            for (cmd in commands) {
                uiAutomation.executeShellCommand(cmd).close()
            }
        }

        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            enableCleanStatusBar()
        }

        @JvmStatic
        @AfterClass
        fun afterAll() {
            CleanStatusBar.disable()
        }
    }
}