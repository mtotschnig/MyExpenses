package org.totschnig.myexpenses.test.screenshots

import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.testutils.TestMain
import org.totschnig.myexpenses.testutils.getBooleanInstrumentationArgument
import org.totschnig.myexpenses.testutils.getInstrumentationArgument
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * When not run from ScreenGrab, it runs with device locale
 */
@LargeTest
class ScreenGrabTest: TestMain(null) {

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    override val shouldTakeScreenShot = getBooleanInstrumentationArgument("screenshots")

    @Test
    fun mkScreenshots() {
        val scenario = getInstrumentationArgument("scenario", "1")
        runScenario(scenario)
    }
}