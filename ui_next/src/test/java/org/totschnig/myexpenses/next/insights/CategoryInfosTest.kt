package org.totschnig.myexpenses.next.insights

import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.next.core.model.ledger.Catalog
import org.totschnig.myexpenses.next.core.model.ledger.CategoryInfo

class CategoryInfosTest {

    @Test
    fun givesCategoriesPathsAndTheDirectionOfTheirMainCategory() {
        val categories = categoryInfos(
            listOf(
                RawCategory(1, null, "Essen", FLAG_EXPENSE.toInt()),
                RawCategory(2, 1, "Supermarkt", 0),
                RawCategory(3, null, "Sonstiges", 0),
            )
        )
        assertEquals(
            listOf(
                CategoryInfo(1, "Essen", "Essen", isExpense = true, isIncome = false),
                CategoryInfo(2, "Supermarkt", "Essen > Supermarkt", isExpense = true, isIncome = false, mainId = 1, parentId = 1),
                CategoryInfo(3, "Sonstiges", "Sonstiges", isExpense = true, isIncome = true),
            ),
            categories
        )
        assertEquals("Essen > Supermarkt", Catalog(categories).categoryPaths[2])
    }
}
