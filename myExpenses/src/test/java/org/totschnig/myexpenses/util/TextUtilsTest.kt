package org.totschnig.myexpenses.util

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.TextUtils.appendCurrencyDescription
import org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.TextUtils.formatQifCategory
import org.totschnig.myexpenses.util.TextUtils.joinEnum

internal class TextUtilsTest {
    @Suppress("unused")
    enum class TestEnum {
        One, Two, Three
    }

    private val ctx = mock(Context::class.java).also {
        Mockito.`when`(it.getString(
            ArgumentMatchers.eq(R.string.Main_1)
        )).thenReturn("Main1")
        Mockito.`when`(it.getString(
            ArgumentMatchers.eq(R.string.Main_2)
        )).thenReturn("Main2")
    }
    private val currency = CurrencyUnit("EUR", "€", 2, "Euro")

    @Test
    fun joinEnum() {
        assertThat(joinEnum(TestEnum::class.java)).isEqualTo("'One','Two','Three'")
    }

    @Test
    fun concatResStrings() {
        assertThat(concatResStrings(ctx, " ", R.string.Main_1, R.string.Main_2)).isEqualTo("Main1 Main2")
    }

    @Test
    fun appendCurrencySymbol() {
        assertThat(appendCurrencySymbol(ctx, R.string.Main_1, currency)).isEqualTo("Main1 (€)")
    }

    @Test
    fun appendCurrencyDescription() {
        assertThat(appendCurrencyDescription(ctx, R.string.Main_1, currency)).isEqualTo("Main1 (Euro)")
    }

    @Test
    fun formatQifCategory() {
        assertThat(formatQifCategory("Main", "Sub")).isEqualTo("Main:Sub")
    }
}