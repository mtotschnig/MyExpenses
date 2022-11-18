package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth
import org.junit.Test
import java.time.LocalDate

class CriteriaTest {

    @Test
    fun testDateCriterion() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val roundTrip = DateCriterion.fromStringExtra(DateCriterion(today, tomorrow).toStringExtra())
        Truth.assertThat(roundTrip.values).asList().containsExactly(today, tomorrow)
    }
}