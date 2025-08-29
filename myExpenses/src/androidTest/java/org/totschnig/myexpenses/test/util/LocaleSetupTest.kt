package org.totschnig.myexpenses.test.util

import android.app.Application
import android.app.LocaleConfig
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R

@RunWith(AndroidJUnit4::class)
class LocaleSetupTest {

    @Test
    fun languageArrayIsComplete() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        Assume.assumeTrue(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
        val supportedLocales = LocaleConfig(context).supportedLocales
        val stringArray = context.resources.getStringArray(R.array.pref_ui_language_values)
        for (i in 0..<supportedLocales!!.size()) {
            assertThat(stringArray).asList().contains(
                supportedLocales.get(i).toString().replace('_','-').let {
                    if (it == "iw") "he" else it
                }
            )
        }
    }
}