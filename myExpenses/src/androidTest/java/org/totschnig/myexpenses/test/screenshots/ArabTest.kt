package org.totschnig.myexpenses.test.screenshots

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

class ArabTest: TestMain() {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    )

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