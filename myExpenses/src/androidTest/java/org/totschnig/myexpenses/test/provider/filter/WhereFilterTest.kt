package org.totschnig.myexpenses.test.provider.filter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.provider.filter.AmountCriteria
import org.totschnig.myexpenses.provider.filter.CategoryCriteria
import org.totschnig.myexpenses.provider.filter.WhereFilter


class WhereFilterTest {
    private lateinit var whereFilter: WhereFilter
    val c1 = CategoryCriteria("test", 1)
    val c2 = CategoryCriteria("test", 2)
    val a1 = AmountCriteria(WhereFilter.Operation.EQ, "EUR", true, 1000, null)

    @Before
    fun setUp() {
        whereFilter = WhereFilter.empty()
    }

    @Test
    fun addCriteria() {
        whereFilter.put(c1)
        whereFilter.put(c2)
        assertThat(whereFilter.criteria.size).isEqualTo(1)
        assertThat(whereFilter.criteria[0]).isEqualTo(c2)
    }

    @Test
    fun removeCriteria() {
        whereFilter.put(c1)
        assertThat(whereFilter.criteria.size).isEqualTo(1)
        whereFilter.remove(c1.id)
        assertThat(whereFilter.criteria).isEmpty()
    }

    @Test
    fun clearFilter() {
        whereFilter.put(c1)
        whereFilter.put(a1)
        assertThat(whereFilter.criteria.size).isEqualTo(2)
        whereFilter.clear()
        assertThat(whereFilter.criteria).isEmpty()
    }
}