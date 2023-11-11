package org.totschnig.myexpenses.test.screenshots

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

class JapaneseTest: TestMain() {
    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule(locale)

    @Test
    fun runJapanese() {
        runScenario("1", locale)
    }

    companion object {
        const val locale = "ja-JP"
    }
}