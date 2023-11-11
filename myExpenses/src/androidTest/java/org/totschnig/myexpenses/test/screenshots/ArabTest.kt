package org.totschnig.myexpenses.test.screenshots

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

class ArabTest: TestMain() {
    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule(locale)

    @Test
    fun runArab() {
        runScenario("1", locale)
    }

    companion object {
        const val locale = "ar-SA"
    }
}