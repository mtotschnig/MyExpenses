package org.totschnig.webdav.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.webdav.R
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.shared_test.LocalizationTestHelper
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AppNameLocalizationTest {

    @Test
    fun shouldBuildWithAppName() {
        val context = getInstrumentation().targetContext
        LocalizationTestHelper.checkAppNameLocalization(context, intArrayOf(
                    R.string.description_webdav_url
        )) {
            ContextHelper.wrap(context, Locale.forLanguageTag(it))
        }
    }
}