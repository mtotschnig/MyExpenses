package org.totschnig.myexpenses.db2

import android.content.ContentValues
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.BudgetAllocationExport
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.model2.CategoryInfo
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.budgetAllocationUri
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR

@RunWith(AndroidJUnit4::class)
class RepositoryBudgetTest : BaseTestWithRepository() {
    @Test
    fun importBudget() {
        val accountId = insertAccount("Test Account")
        val category = writeCategory("Category", uuid = "categoryUuid")
        val budgetId = runBlocking {
            repository.importBudget(
                BudgetExport(
                    "Budget Title",
                    "Budget Description",
                    Grouping.MONTH,
                    "accountUuid",
                    "EUR",
                    null,
                    null,
                    false,
                    allocations = listOf(
                        BudgetAllocationExport(
                            category = null,
                            year = null,
                            second = null,
                            budget = 1234L,
                            rolloverPrevious = null,
                            rolloverNext = null,
                            oneTime = false
                        ),
                        BudgetAllocationExport(
                            category = listOf(CategoryInfo("categoryUuid", "Category")),
                            year = 2024,
                            second = 1,
                            budget = 1000L,
                            rolloverPrevious = null,
                            rolloverNext = null,
                            oneTime = false
                        )
                    )
                ),
                0,
                accountId,
                null
            )
        }
        with(repository.loadBudget(budgetId)!!) {
            assertThat(title).isEqualTo("Budget Title")
            assertThat(description).isEqualTo("Budget Description")
            assertThat(grouping).isEqualTo(Grouping.MONTH)
            assertThat(
                repository.budgetAllocation(budgetId, 2024, 1)
            ).isEqualTo(1234L)
            assertThat(
                repository.budgetAllocation(budgetId, category, 2024 to 1)
            ).isEqualTo(1000L)
        }
    }

    @Test
    fun calculateAllocationForPeriodicBudget() {
        val accountId = insertAccount("Test Account")
        val budgetId = insertBudget(accountId, "Budget", 100)
        assertThat(
            repository.budgetAllocation(budgetId, 2025, 3)
        ).isEqualTo(100L)
    }

    @Test
    fun calculateAllocationForPeriodicBudgetWithRollover() {
        val accountId = insertAccount("Test Account")
        val budgetId = insertBudget(accountId, "Budget", 100)

        contentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2025)
                put(KEY_SECOND_GROUP, 3)
                put(KEY_BUDGET_ROLLOVER_PREVIOUS, 50)
            }, null, null
        )
        assertThat(
            repository.budgetAllocation(budgetId, 2025, 3)
        ).isEqualTo(150L)
    }

    @Test
    fun calculateAllocationForOneTimeBudget() {
        val accountId = insertAccount("Test Account")
        val budgetId = insertBudget(accountId, "Budget", 100, Grouping.NONE)

        assertThat(
            repository.budgetAllocation(budgetId, null, null)
        ).isEqualTo(100)
    }

    @Test
    fun testGetGrouping() {
        val accountId = insertAccount("Test Account")
        val budgetId = insertBudget(accountId, "Budget", 100, Grouping.NONE)
        assertThat(repository.getGrouping(budgetId)).isEqualTo(Grouping.NONE)
    }
}