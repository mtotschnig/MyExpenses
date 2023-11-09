package org.totschnig.myexpenses.repository

import android.content.ContentUris
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.shared_test.CursorSubject.Companion.assertThat

@RunWith(RobolectricTestRunner::class)
class GroupQueryTest : BaseTestWithRepository() {

    private var testAccountId: Long = 0
    private var searchCategory: Long = 0

    private fun insertCategory(label: String, typeFlags: UByte) = ContentUris.parseId(
        repository.saveCategory(
            Category(
                label = label,
                typeFlags = typeFlags
            )
        )!!
    )

    private fun insertTransaction(amount: Long, categoryId: Long) {
        contentResolver.insert(
            TransactionProvider.TRANSACTIONS_URI, TransactionInfo(
                accountId = testAccountId,
                amount = amount,
                catId = categoryId
            ).contentValues
        )
    }

    @Before
    fun setup() {
        val testAccount = AccountInfo("Test account", AccountType.CASH, 0, "USD")
        testAccountId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                testAccount.contentValues
            )!!
        )
        val neutralCategoryId = insertCategory("Neutral", FLAG_NEUTRAL)
        val expenseCategoryId = insertCategory("Expense", FLAG_EXPENSE)
        val incomeCategoryId = insertCategory("Income", FLAG_INCOME)
        val transferCategoryId = insertCategory("Transfer", FLAG_TRANSFER)
        insertTransaction(100, neutralCategoryId)
        insertTransaction(-200, expenseCategoryId)
        insertTransaction(400, incomeCategoryId)
        insertTransaction(800, transferCategoryId)
        searchCategory = neutralCategoryId
    }

    @Test
    fun groupQueryFilterWithCategoryFilter() {
        val filter = WhereFilter(listOf(CategoryCriterion("Neutral", searchCategory)))
        contentResolver.query(
            BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build(),
            null,
            filter.getSelectionForParts(VIEW_WITH_ACCOUNT),
            filter.getSelectionArgs(true),
            null
        )?.use {
            with(assertThat(it)) {
                hasCount(1)
                hasColumns(
                    KEY_YEAR,
                    KEY_SECOND_GROUP,
                    KEY_SUM_EXPENSES,
                    KEY_SUM_INCOME,
                    KEY_SUM_TRANSFERS
                )
                movesToFirst()
                hasInt(0, 1)
                hasInt(1, 1)
                hasLong(2, 0)
                hasLong(3, 100)
                hasLong(4, 0)
            }
        }
    }

    @Test
    fun groupQueryFilterWithAmountFilter() {
        val filter = WhereFilter(
            listOf(
                AmountCriterion.create(
                    WhereFilter.Operation.EQ, "EUR",  true, 400L, null
                )
            )
        )
        contentResolver.query(
            BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build(),
            null,
            filter.getSelectionForParts(VIEW_WITH_ACCOUNT),
            filter.getSelectionArgs(true),
            null
        )?.use {
            with(assertThat(it)) {
                hasCount(1)
                hasColumns(
                    KEY_YEAR,
                    KEY_SECOND_GROUP,
                    KEY_SUM_EXPENSES,
                    KEY_SUM_INCOME,
                    KEY_SUM_TRANSFERS
                )
                movesToFirst()
                hasInt(0, 1)
                hasInt(1, 1)
                hasLong(2, 0)
                hasLong(3, 400)
                hasLong(4, 0)
            }
        }
    }

    @Test
    fun groupQueryWithoutFilter() {
        contentResolver.query(
            BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build(),
            null,
            null,
            null,
            null
        )?.use {
            with(assertThat(it)) {
                hasCount(1)
                hasColumns(
                    KEY_YEAR,
                    KEY_SECOND_GROUP,
                    KEY_SUM_EXPENSES,
                    KEY_SUM_INCOME,
                    KEY_SUM_TRANSFERS
                )
                movesToFirst()
                hasInt(0, 1)
                hasInt(1, 1)
                hasLong(2, -200)
                hasLong(3, 500)
                hasLong(4, 800)
            }
        }
    }
}