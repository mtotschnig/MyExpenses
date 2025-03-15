package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.budgetAllocationUri
import org.totschnig.myexpenses.provider.BudgetInfo
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class BudgetTest : BaseDbTest() {

    private fun insertOneTimeBudget() = ContentUris.parseId(
        mockContentResolver.insert(
            TransactionProvider.BUDGETS_URI,
            BudgetInfo(
                setupTestAccount(),
                "budget 1",
                400,
                Grouping.NONE,
                "description",
                "2023-12-01",
                "2023-12-18"
            ).contentValues,
        )!!
    )

    private fun insertMonthlyBudget() = ContentUris.parseId(
        mockContentResolver.insert(
            TransactionProvider.BUDGETS_URI,
            BudgetInfo(
                setupTestAccount(),
                "budget 1",
                400,
                Grouping.MONTH,
                "description"
            ).contentValues,
        )!!
    )

    private fun assertBudgetAmount(
        budgetId: Long,
        budgetAmount: Long,
        rollOver: Long = 0,
        year: Int? = null,
        second: Int? = null
    ) {
        mockContentResolver.query(
            budgetAllocationQueryUri(budgetId, year?.toString(), second?.toString()),
            arrayOf(KEY_BUDGET),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasLong(0, budgetAmount + rollOver)
        }
    }

    fun testUpdateOfOneTimeBudget() {
        val budgetId = insertOneTimeBudget()
        assertBudgetAmount(budgetId, 400)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(1).apply {
                put(KEY_BUDGET, 500)
            }, null, null
        )
        assertBudgetAmount(budgetId, 500)
    }

    fun testUpdateOfRepeatingBudget() {
        val budgetId = insertMonthlyBudget()
        assertBudgetAmount(budgetId, 400, year = 2023, second = 11)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET, 500)
            }, null, null
        )
        assertBudgetAmount(budgetId, 400, year = 2023, second = 11)
        assertBudgetAmount(budgetId, 500, year = 2023, second = 12)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET_ROLLOVER_PREVIOUS, 50)
            }, null, null
        )
        assertBudgetAmount(budgetId, 500, rollOver = 50, year = 2023, second = 12)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET, 600)
            }, null, null
        )
        assertBudgetAmount(budgetId, 600, rollOver = 50, year = 2023, second = 12)
    }
}