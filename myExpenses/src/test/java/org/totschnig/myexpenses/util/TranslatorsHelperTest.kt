package org.totschnig.myexpenses.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.util.TranslatorsHelper.buildTranslationCredits

@RunWith(RobolectricTestRunner::class)
class TranslatorsHelperTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testBuildTranslationCreditsNotEmpty() {
        val credits = context.buildTranslationCredits()
        for (string in credits) {
            println(string)
        }
        assertThat(credits).isNotEmpty()
    }
}
