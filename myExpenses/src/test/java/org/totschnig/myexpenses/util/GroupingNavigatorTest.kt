package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra

class GroupingNavigatorTest {

    @Test
    fun current() {
        assertThat(GroupingNavigator.current(Grouping.DAY, DateInfo(1, 1, 1, 2024, 2023, 2023)))
            .isEqualTo(GroupingInfo(Grouping.DAY, 2024, 1))
        assertThat(GroupingNavigator.current(Grouping.WEEK, DateInfo(1, 52, 1, 2024, 2023, 2023)))
            .isEqualTo(GroupingInfo(Grouping.WEEK, 2023, 52))
        assertThat(GroupingNavigator.current(Grouping.MONTH, DateInfo(1, 1, 11, 2024, 2023, 2023)))
            .isEqualTo(GroupingInfo(Grouping.MONTH, 2023, 11))
        assertThat(GroupingNavigator.current(Grouping.YEAR, DateInfo(1, 1, 1, 2024, 2023, 2023)))
            .isEqualTo(GroupingInfo(Grouping.YEAR, 2024, 0))
    }

    @Test
    fun navigateDay() {
        val first = GroupingInfo(Grouping.DAY, 2024, 1)
        val last = GroupingInfo(Grouping.DAY, 2023, 365)
        val dateInfoExtra = DateInfoExtra(365, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    @Test
    fun navigateDayLeapYear() {
        val first = GroupingInfo(Grouping.DAY, 2025, 1)
        val last = GroupingInfo(Grouping.DAY, 2024, 366)
        val dateInfoExtra = DateInfoExtra(366, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    @Test
    fun navigateWeek() {
        val first = GroupingInfo(Grouping.WEEK, 2024, 1)
        val last = GroupingInfo(Grouping.WEEK, 2023, 52)
        val dateInfoExtra = DateInfoExtra(52, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    @Test
    fun navigateWeek53() {
        val first = GroupingInfo(Grouping.WEEK, 2021, 1)
        val last = GroupingInfo(Grouping.WEEK, 2020, 53)
        val dateInfoExtra = DateInfoExtra(53, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    @Test
    fun navigateMonth() {
        val first = GroupingInfo(Grouping.MONTH, 2024, 0)
        val last = GroupingInfo(Grouping.MONTH, 2023, 11)
        val dateInfoExtra = DateInfoExtra(11, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    @Test
    fun navigateYear() {
        val first = GroupingInfo(Grouping.YEAR, 2024, 0)
        val last = GroupingInfo(Grouping.YEAR, 2023, 0)
        val dateInfoExtra = DateInfoExtra(0, LocalDate.now())
        runTheTest(first, last, dateInfoExtra)
    }

    private fun runTheTest(first: GroupingInfo, last: GroupingInfo, dateInfoExtra: DateInfoExtra) {
        runBlocking {
            assertThat(GroupingNavigator.previous(first) { dateInfoExtra })
                .isEqualTo(last)
            assertThat(GroupingNavigator.next(last) { dateInfoExtra })
                .isEqualTo(first)
        }
    }
}
