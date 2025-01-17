package org.totschnig.myexpenses.db2

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
                repository.budgetAllocation(budgetId, 0, Grouping.MONTH, 2024, 1)!!.budget
            ).isEqualTo(1234L)
            assertThat(
                repository.budgetAllocation(budgetId, category, Grouping.MONTH, 2024, 1)!!.budget
            ).isEqualTo(1000L)
        }
    }
}