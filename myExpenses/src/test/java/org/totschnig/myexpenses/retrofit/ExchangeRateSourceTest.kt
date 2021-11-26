package org.totschnig.myexpenses.retrofit

import androidx.test.core.app.ApplicationProvider
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R

@RunWith(RobolectricTestRunner::class)
class ExchangeRateSourceTest {
    @Test
    fun testEnumMatchesPreference() {
        val enumValues: Array<String> = ExchangeRateSource.values().map { it.name }.toTypedArray()
        val prefValues: Array<String> = ApplicationProvider.getApplicationContext<MyApplication>().getStringArray(R.array.exchange_rate_provider_values)
        Truth.assertWithMessage("ExchangeRateSource enum differs from exchange_rate_provider_values").that(enumValues contentEquals prefValues).isTrue()
    }
}