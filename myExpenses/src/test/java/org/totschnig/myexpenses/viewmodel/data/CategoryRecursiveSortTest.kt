package org.totschnig.myexpenses.viewmodel.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryRecursiveSortTest {

    // Helper to create a budget allocation easily
    private fun mockBudget(allocated: Long) = BudgetAllocation(
        budget = allocated,
        rollOverNext = 0,
        rollOverPrevious = 0,
        oneTime = false
    )

    private val child1 = Category(
        level = 1,
        id = 1,
        label = "Beta",
        sum = -100L,
        budget = mockBudget(500L)
    )

    private val grandchild1 = Category(
        level = 2,
        id = 3,
        label = "Gamma 9",
        sum = 450,
        budget = mockBudget(100L)
    )

    private val grandchild2 = Category(
        level = 2,
        id = 4,
        label = "Gamma 10",
        sum = 0,
        budget = mockBudget(0L)
    )


    private val child2 = Category(
        level = 1,
        id = 2,
        label = "Alpha",
        sum = -500L,
        budget = mockBudget(200L),
        children = listOf(grandchild1, grandchild2)
    )

    private val root = Category(
        level = 0,
        id = 0,
        label = "Root",
        children = listOf(
            child2,
            child1
        )
    )

    @Test
    fun testAggregateValues() {
        assertThat(child2.aggregateSum).isEqualTo(-50L)
    }

    @Test
    fun sortChildrenByBudgetRecursive_ordersByAllocatedDescending() {
        // Beta (500) > Alpha (200)
        val sorted = root.sortChildrenByBudgetRecursive()

        assertThat(sorted.children[0].id).isEqualTo(1L)
        assertThat(sorted.children[1].id).isEqualTo(2L)
    }

    @Test
    fun sortChildrenBySumRecursive_ordersByAbsoluteAggregateSum() {
        // Beta's aggregateSum = abs(-100) = 100
        // Alpha's aggregateSum = abs(-500 + 450) = 50
        val sorted = root.sortChildrenBySumRecursive()

        // Based on logic Beta (50) vs Alpha (100), sorted descending:
        assertThat(sorted.children[0].id).isEqualTo(1L)
        assertThat(sorted.children[1].id).isEqualTo(2L)
    }

    @Test
    fun sortChildrenByAvailableRecursive_ordersBySumPlusAllocated() {
        // Beta: 500 (budget) + -100 (sum) = 400
        // Alpha: 200 (budget) + (-500 + 450) (aggregate sum) = 150
        val sorted = root.sortChildrenByAvailableRecursive()

        assertThat(sorted.children[0].id).isEqualTo(1L) // Beta (400)
        assertThat(sorted.children[1].id).isEqualTo(2L) // Alpha (150)
    }

    @Test
    fun verifyRecursionWorks() {
        // Create a deeper nested structure to ensure grandchildren are also sorted
        val nestedRoot = Category(
            label = "Root",
            children = listOf(
                Category(
                    label = "Parent",
                    children = listOf(
                        Category(id = 10, label = "Small Budget", budget = mockBudget(10)),
                        Category(id = 20, label = "Big Budget", budget = mockBudget(100))
                    )
                )
            )
        )

        val sorted = nestedRoot.sortChildrenByBudgetRecursive()
        val parent = sorted.children[0]

        // Check that the grandchild list was sorted: 100 should come before 10
        assertThat(parent.children[0].id).isEqualTo(20L)
        assertThat(parent.children[1].id).isEqualTo(10L)
    }
}