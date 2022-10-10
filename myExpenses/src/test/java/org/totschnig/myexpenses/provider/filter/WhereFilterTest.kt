package org.totschnig.myexpenses.provider.filter

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class WhereFilterTest {
    private lateinit var whereFilter: WhereFilter
    val c1 = CategoryCriterion("test", 1)
    val c2 = CategoryCriterion("test", 2)

    @Before
    fun setUp() {
        whereFilter = WhereFilter.empty()
    }

    @Test
    fun addCriteria() {
        whereFilter = whereFilter.put(c1).put(c2)
        Assertions.assertThat(whereFilter.criteria.size).isEqualTo(1)
        Assertions.assertThat(whereFilter.criteria[0]).isEqualTo(c2)
    }

    @Test
    fun removeCriteria() {
        whereFilter = whereFilter.put(c1)
        Assertions.assertThat(whereFilter.criteria.size).isEqualTo(1)
        whereFilter = whereFilter.remove(c1.id)
        Assertions.assertThat(whereFilter.criteria).isEmpty()
    }
}