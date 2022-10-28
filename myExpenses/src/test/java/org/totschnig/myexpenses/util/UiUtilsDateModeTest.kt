package org.totschnig.myexpenses.util

import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.UiUtils.DateMode

@RunWith(RobolectricTestRunner::class)
class UiUtilsDateModeTest {
    private lateinit var prefHandler: PrefHandler
    @Before
    fun setup() {
        prefHandler = Mockito.mock(PrefHandler::class.java)
    }

    private fun mockPref(prefKey: PrefKey, value: Boolean) {
        Mockito.`when`(
            prefHandler.getBoolean(
                ArgumentMatchers.eq(prefKey),
                ArgumentMatchers.anyBoolean()
            )
        ).thenReturn(value)
    }

    @Test
    fun withTimeAndWithValueDate() {
        mockPref(PrefKey.TRANSACTION_WITH_TIME, true)
        mockPref(PrefKey.TRANSACTION_WITH_VALUE_DATE, true)
        Truth.assertThat(dateMode(AccountType.CASH)).isEqualTo(DateMode.DATE_TIME)
        Truth.assertThat(dateMode(AccountType.BANK)).isEqualTo(DateMode.BOOKING_VALUE)
    }

    @Test
    fun withTimeAndWithoutValueDate() {
        mockPref(PrefKey.TRANSACTION_WITH_TIME, true)
        mockPref(PrefKey.TRANSACTION_WITH_VALUE_DATE, false)
        Truth.assertThat(dateMode(AccountType.CASH)).isEqualTo(DateMode.DATE_TIME)
        Truth.assertThat(dateMode(AccountType.BANK)).isEqualTo(DateMode.DATE_TIME)
    }

    @Test
    fun withoutTimeAndWithValueDate() {
        mockPref(PrefKey.TRANSACTION_WITH_TIME, false)
        mockPref(PrefKey.TRANSACTION_WITH_VALUE_DATE, true)
        Truth.assertThat(dateMode(AccountType.CASH)).isEqualTo(DateMode.DATE)
        Truth.assertThat(dateMode(AccountType.BANK)).isEqualTo(DateMode.BOOKING_VALUE)
    }

    @Test
    fun withoutTimeAndWithoutValueDate() {
        mockPref(PrefKey.TRANSACTION_WITH_TIME, false)
        mockPref(PrefKey.TRANSACTION_WITH_VALUE_DATE, false)
        Truth.assertThat(dateMode(AccountType.CASH)).isEqualTo(DateMode.DATE)
        Truth.assertThat(dateMode(AccountType.BANK)).isEqualTo(DateMode.DATE)
    }

    private fun dateMode(accountType: AccountType): DateMode {
        return getDateMode(accountType, prefHandler)
    }
}