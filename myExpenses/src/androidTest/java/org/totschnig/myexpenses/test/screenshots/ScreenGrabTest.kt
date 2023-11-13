package org.totschnig.myexpenses.test.screenshots

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil

/**
 * When not run from ScreenGrab, it runs with device locale
 */
class ScreenGrabTest: TestMain() {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    override val shouldTakeScreenShot = true

    @Test
    fun mkScreenshots() {
        val scenario = InstrumentationRegistry.getArguments().getString("scenario", "1")
        runScenario(scenario, LocaleUtil.getTestLocale())
    }
}