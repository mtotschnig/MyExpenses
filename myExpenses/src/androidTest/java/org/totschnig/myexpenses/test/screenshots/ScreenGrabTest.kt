package org.totschnig.myexpenses.test.screenshots

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * When not run from ScreenGrab, it runs with device locale
 */
@LargeTest
class ScreenGrabTest: TestMain(null) {

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    override val shouldTakeScreenShot = getInstrumentationArgument("screenshots", "1") == "true"

    @Test
    fun mkScreenshots() {
        val scenario = getInstrumentationArgument("scenario", "1")
        runScenario(scenario)
    }

    private fun getInstrumentationArgument(key: String, @Suppress("SameParameterValue") defaultValue: String) =
        InstrumentationRegistry.getArguments().getString(key, defaultValue)
}