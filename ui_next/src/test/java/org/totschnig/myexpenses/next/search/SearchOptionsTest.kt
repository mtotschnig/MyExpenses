package org.totschnig.myexpenses.next.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchOptionsTest {

    private val options = tree(
        listOf(
            SearchOption(3, "Wohnen"),
            SearchOption(1, "Lebensmittel"),
            SearchOption(2, "Bäcker", parentId = 1),
            SearchOption(4, "Getränke", parentId = 1),
            SearchOption(5, "Miete", parentId = 3),
            SearchOption(6, "Waise", parentId = 99),
        )
    )

    @Test
    fun sortsSubcategoriesBelowTheirCategory() {
        assertEquals(listOf(1L, 2L, 4L, 6L, 3L, 5L), options.map { it.id })
        assertEquals(listOf(0, 1, 1, 0, 0, 1), options.map { it.depth })
    }

    @Test
    fun findsOptionsWithTheOnesAboveThem() {
        assertEquals(listOf(1L, 2L), options.matching("backer").map { it.id })
        assertEquals(listOf(3L, 5L), options.matching(" MIETE ").map { it.id })
        assertEquals(listOf(1L), options.matching("lebens").map { it.id })
        assertEquals(options, options.matching(""))
    }
}
