package org.totschnig.myexpenses.test.screenshots

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil

/**
 * When not run from ScreenGrab, it runs with device locale
 */
class ScreenGrabTest: TestMain() {
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