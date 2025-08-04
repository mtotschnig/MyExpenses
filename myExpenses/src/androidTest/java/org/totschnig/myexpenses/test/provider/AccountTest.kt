package org.totschnig.myexpenses.test.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import androidx.test.rule.provider.ProviderTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.BudgetInfo
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_BY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.SORT_URI
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class AccountTest {
    @get:Rule
    val providerRule: ProviderTestRule =
        ProviderTestRule.Builder(TransactionProvider::class.java, TransactionProvider.AUTHORITY)
            .build()

    private val resolver: ContentResolver
        get() = providerRule.resolver

    private val testAccounts = arrayOf(
        AccountInfo("Account 0", 1, 0),
        AccountInfo("Account 1", 2, 100),
        AccountInfo("Account 2", 3, -100)
    )

    private fun insertData() {
        //TODO set up account type
        for (account in testAccounts) {
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                account.contentValues
            )
        }
    }

    @After
    fun clearDb() {
        resolver.delete(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null
        )
    }

    private fun insertAccountWithTwoBudgets(): Long {
        val grouping = Grouping.MONTH

        //TODO set up account type
        return ContentUris.parseId(
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                AccountInfo(
                    "Account with 2 budgets",
                    1,
                    -100,
                    "EUR"
                ).contentValues
            )!!
        ).also {
            resolver.update(
                ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, it)
                    .buildUpon()
                    .appendPath(grouping.name).build(),
                null, null, null
            )
            val budgets = arrayOf(
                BudgetInfo(it, "budget 1", 400, grouping, "description"),
                BudgetInfo(it, "budget 2", 5000, grouping, "description")
            )
            for (budgetInfo in budgets) {
                resolver.insert(
                    TransactionProvider.BUDGETS_URI,
                    budgetInfo.contentValues
                )
            }
        }
    }

    @Test
    fun testQueriesOnAccountUri() {
        val testProjection = arrayOf(
            KEY_LABEL,
            KEY_DESCRIPTION,
            KEY_CURRENCY
        )
        val commentSelection = "$TABLE_ACCOUNTS.$KEY_LABEL = ?"
        val selectionColumns =
            "$commentSelection OR $commentSelection OR $commentSelection"
        val selectionArgs = arrayOf("Account 0", "Account 1", "Account 2")
        val sortOrder = "$KEY_LABEL ASC"
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(0)
        }
        insertData()

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert  {
            hasCount(testAccounts.size)
        }

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            testProjection,
            null,
            null,
            null
        ).useAndAssert {
            hasColumnCount(testProjection.size)
            hasColumns(*testProjection)
        }

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            testProjection,
            selectionColumns,
            selectionArgs,
            sortOrder
        ).useAndAssert {
            hasCount(selectionArgs.size)
            var index = 0
            while (actual.moveToNext()) {
                hasString(0, selectionArgs[index])
                index++
            }
        }
    }

    @Test
    fun testAccountIdUriWithMultipleBudgets() {
        val accountId = insertAccountWithTwoBudgets()
        val uri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId)
        resolver.query(
            uri,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(1)
        }
    }

    @Test
    fun testQueriesOnAccountIdUri() {
        val selection = "$TABLE_ACCOUNTS.$KEY_LABEL = ?"
        val query = "Account 0"
        val args = arrayOf(query)
        val projection = arrayOf(
            KEY_ROWID,
            KEY_LABEL
        )
        insertData()
        val inputAccountId = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(testAccounts.size)
            movesToFirst()
            actual.getLong(0)
        }
        val uri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, inputAccountId)

        resolver.query(
            uri, arrayOf(KEY_LABEL),
            selection,
            args,
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasString(0, query)
        }
    }

    @Test
    fun testInserts() {
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(0)
        }
        val account = AccountInfo(
            "Account 4",
            4, 1000
        )

        val accountId = ContentUris.parseId(
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                account.contentValues
            )!!
        )

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        ).useAndAssert {
            val descriptionIndex = actual.getColumnIndex(KEY_DESCRIPTION)
            val labelIndex = actual.getColumnIndex(KEY_LABEL)
            val balanceIndex = actual.getColumnIndex(KEY_OPENING_BALANCE)
            val currencyIndex = actual.getColumnIndex(KEY_CURRENCY)
            hasCount(1)
            movesToFirst()
            hasString(labelIndex, account.label)
            hasString(descriptionIndex, account.description)
            hasLong(balanceIndex, account.openingBalance)
            hasString(currencyIndex, account.currency)
        }
        val values = account.contentValues
        values.put(KEY_ROWID, accountId)
        assertThrows(Exception::class.java) {
            resolver.insert(TransactionProvider.ACCOUNTS_URI, values)
        }
    }

    @Test
    fun testDeletes() {
        val selection = "$TABLE_ACCOUNTS.$KEY_LABEL = ?"
        val args = arrayOf("Account 0")
        assertThat(
            resolver.delete(
                TransactionProvider.ACCOUNTS_URI,
                selection,
                args
            )
        ).isEqualTo(0)
        insertData()
        assertThat(
            resolver.delete(
                TransactionProvider.ACCOUNTS_URI,
                selection,
                args
            )
        ).isEqualTo(1)
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            selection,
            args,
            null
        ).useAndAssert {
            hasCount(0)
        }
    }

    @Test
    fun testUpdates() {
        val selection = "$TABLE_ACCOUNTS.$KEY_LABEL = ?"
        val selectionArgs = arrayOf("Account 1")
        val values = ContentValues().apply {
            put(KEY_LABEL, "Testing an update with this string")
        }

        assertThat(
            resolver.update(
                TransactionProvider.ACCOUNTS_URI,
                values,
                selection,
                selectionArgs
            )
        ).isEqualTo(0)
        insertData()
        assertThat(
            resolver.update(
                TransactionProvider.ACCOUNTS_URI,
                values,
                selection,
                selectionArgs
            )
        ).isEqualTo(1)
    }

    @Test
    fun testGrouping() {
        val projection = arrayOf(
            KEY_ROWID,
            KEY_GROUPING
        )
        insertData()
        val id = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(testAccounts.size)
            movesToFirst()
            hasString(1, Grouping.NONE.name)
            actual.getLong(0)
        }
        val uri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, id)

        resolver.update(
            ContentUris.withAppendedId(
                TransactionProvider.ACCOUNT_GROUPINGS_URI,
                id
            ).buildUpon().appendPath(Grouping.YEAR.name).build(),
            null, null, null
        )
        resolver.query(
            uri,
            arrayOf(DbUtils.fqcn(TABLE_ACCOUNTS, KEY_GROUPING)),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasString(0, Grouping.YEAR.name)
        }
    }

    @Test
    fun testSortDirection() {
        val projection = arrayOf(
            KEY_ROWID,
            KEY_SORT_BY,
            KEY_SORT_DIRECTION
        )
        insertData()
        val id = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        ).useAndAssert {
            hasCount(testAccounts.size)
            movesToFirst()
            hasString(1, KEY_DATE)
            hasString(2, SortDirection.DESC.name)
            actual.getLong(0)
        }

        val accountIdUri =
            ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, id)

        resolver.update(
            ContentUris.withAppendedId(SORT_URI, id)
                .buildUpon()
                .appendPath(KEY_AMOUNT)
                .appendPath(SortDirection.ASC.name)
                .build(),
            null, null, null
        )
        resolver.query(
            accountIdUri, arrayOf(KEY_SORT_BY, KEY_SORT_DIRECTION),
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            hasString(0, KEY_AMOUNT)
            hasString(1, SortDirection.ASC.name)
        }
    }

    @Test
    fun testQueryWithSum() {
        val id = resolver.insert(
            TransactionProvider.ACCOUNTS_URI,
            AccountInfo("Account 0", 1, 0).contentValues
        ).let {
            ContentUris.parseId(it!!)
        }
        resolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,
            "$KEY_ROWID = ?",
            arrayOf(id.toString()),
            null
        )!!.useAndAssert {
            //TODO insert transactions and test calculated sum
            hasCount(1)
            movesToFirst()
            hasLong(KEY_SUM_EXPENSES, 0)
        }
    }
}