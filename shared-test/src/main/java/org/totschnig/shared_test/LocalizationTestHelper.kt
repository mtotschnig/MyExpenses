package org.totschnig.shared_test

import android.content.Context
import org.junit.Assert
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import timber.log.Timber

object LocalizationTestHelper {

    fun checkAppNameLocalization(context: Context, resIds: IntArray, localeFactory: (String) -> Context) {
        val locales = context.resources.getStringArray(R.array.pref_ui_language_values).asList()
            .minus("default")
        val failures = mutableListOf<Pair<String, String>>()
        for (localeString in locales) {
            val localizedContext = localeFactory(localeString)
            for (resId in resIds) {
                val resourceName = context.resources.getResourceName(resId)
                try {
                    Timber.d("%s (%s) -> %s", resourceName, localeString,
                        Utils.getTextWithAppName(localizedContext, resId)
                    )
                } catch (_: Exception) {
                    failures.add(localeString to resourceName)
                }
            }
        }
        if (failures.isNotEmpty()) {
            Assert.fail("Non-compliant resources: " + failures.joinToString())
        }
    }
}