package org.totschnig.myexpenses.preference

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.prefHandler
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimePreferenceTest {

    @Before
    fun setup() {
        whenever(prefHandler.getInt(eq(PrefKey.AUTO_BACKUP_TIME), any())).thenReturn(7 * 100)

    }

    private fun fixedClock(hour: Int) =
        Clock.fixed(
            ZonedDateTime.of(
                LocalDate.now(),
                LocalTime.of(hour, 0),
                ZoneId.systemDefault()
            ).toInstant(),
            ZoneId.systemDefault()
        )

    @Test
    fun getScheduledTimeSameDay() {
        assertThat(
            TimePreference.getScheduledTime(
                prefHandler,
                PrefKey.AUTO_BACKUP_TIME,
                fixedClock(6)
            )
        )
            .isEqualTo(Duration.ofHours(1).toMillis())
    }

    @Test
    fun getScheduledTimeNextDay() {
        assertThat(
            TimePreference.getScheduledTime(
                prefHandler,
                PrefKey.AUTO_BACKUP_TIME,
                fixedClock(8)
            )
        )
            .isEqualTo(Duration.ofHours(23).toMillis())
    }
}