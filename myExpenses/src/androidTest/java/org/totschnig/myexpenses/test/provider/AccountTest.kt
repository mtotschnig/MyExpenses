package org.totschnig.myexpenses.test.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import androidx.test.rule.provider.ProviderTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.CursorSubject.Companion.assertThat

class AccountTest {
    @get:Rule
    val providerRule: ProviderTestRule = ProviderTestRule.Builder(TransactionProvider::class.java, TransactionProvider.AUTHORITY).build()

    private val resolver: ContentResolver
        get() = providerRule.resolver
    
    private val testAccounts = arrayOf(
        AccountInfo("Account 0", AccountType.CASH, 0),
        AccountInfo("Account 1", AccountType.BANK, 100),
        AccountInfo("Account 2", AccountType.CCARD, -100)
    )

    private fun insertData() {
        for (account in testAccounts) {
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                account.contentValues
            )
        }
    }

    private fun insertAccountWithTwoBudgets(): Long {
        val grouping = Grouping.MONTH

        return ContentUris.parseId(
            resolver.insert(
                TransactionProvider.ACCOUNTS_URI,
                AccountInfo(
                    "Account with 2 budgets",
                    AccountType.CCARD,
                    -100,
                    "EUR",
                    grouping
                ).contentValues
            )!!
        ).also {
            val budgets = arrayOf(
                BudgetInfo(it, "budget 1", "description", 400, grouping),
                BudgetInfo(it, "budget 2", "description", 5000, grouping)
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
            DatabaseConstants.KEY_LABEL,
            DatabaseConstants.KEY_DESCRIPTION,
            DatabaseConstants.KEY_CURRENCY
        )
        val commentSelection = DatabaseConstants.KEY_LABEL + " = " + "?"
        val selectionColumns =
            "$commentSelection OR $commentSelection OR $commentSelection"
        val selectionArgs = arrayOf("Account 0", "Account 1", "Account 2")
        val sortOrder = DatabaseConstants.KEY_LABEL + " ASC"
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        )!!.use {
            assertThat(it).hasCount(0)
        }
        insertData()

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            null,
            null,
            null
        )!!.use {
            assertThat(it).hasCount(testAccounts.size)
        }

        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            testProjection,
            null,
            null,
            null
        )!!.use { 
            assertThat(it).hasColumnCount(testProjection.size)
            testProjection.forEachIndexed { index, column ->
                assertThat(it.getColumnName(index)).isEqualTo(column)
            }
        }
        
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            testProjection,
            selectionColumns,
            selectionArgs,
            sortOrder
        )!!.use {
            assertThat(it).hasCount(selectionArgs.size)
            var index = 0
            while (it.moveToNext()) {
                assertThat(it.getString(0)).isEqualTo(selectionArgs[index])
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
        )!!.use {
            assertThat(it).hasCount(1)
        }
    }

    @Test
    fun testQueriesOnAccountIdUri() {
        val columns = DatabaseConstants.KEY_LABEL + " = " + "?"
        val query = "Account 0"
        val args = arrayOf(query)
        val projection = arrayOf(
            DatabaseConstants.KEY_ROWID,
            DatabaseConstants.KEY_LABEL
        )
        insertData()
        val inputAccountId = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        )!!.use {
            with(assertThat(it)) {
                hasCount(testAccounts.size)
                moveToFirst()
            }
            it.getLong(0)
        }
        val uri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, inputAccountId)

        resolver.query(
            uri, arrayOf(DatabaseConstants.KEY_LABEL),
            columns,
            args,
            null
        )!!.use {
            with(assertThat(it)) {
                hasCount(1)
                moveToFirst()
                hasString(0, query)
            }
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
        )!!.use {
            assertThat(it).hasCount(0)
        }
        val account = AccountInfo(
            "Account 4",
            AccountType.ASSET, 1000
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
        )!!.use {
            val descriptionIndex = it.getColumnIndex(DatabaseConstants.KEY_DESCRIPTION)
            val labelIndex = it.getColumnIndex(DatabaseConstants.KEY_LABEL)
            val balanceIndex = it.getColumnIndex(DatabaseConstants.KEY_OPENING_BALANCE)
            val currencyIndex = it.getColumnIndex(DatabaseConstants.KEY_CURRENCY)
            with(assertThat(it)) {
                hasCount(1)
                moveToFirst()
                hasString(labelIndex, account.label)
                hasString(descriptionIndex, account.description)
                hasLong(balanceIndex, account.openingBalance)
                hasString(currencyIndex, account.currency)
            }
        }
        val values = account.contentValues
        values.put(DatabaseConstants.KEY_ROWID, accountId)
        assertThrows(Exception::class.java) {
            resolver.insert(TransactionProvider.ACCOUNTS_URI, values)
        }
    }

    @Test
    fun testDeletes() {
        val columns = DatabaseConstants.KEY_LABEL + " = " + "?"
        val args = arrayOf("Account 0")
        assertThat(resolver.delete(
            TransactionProvider.ACCOUNTS_URI,
            columns,
            args
        )).isEqualTo(0)
        insertData()
        assertThat(resolver.delete(
            TransactionProvider.ACCOUNTS_URI,
            columns,
            args
        )).isEqualTo(1)
        resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            null,
            columns,
            args,
            null
        )!!.use {
            assertThat(it).hasCount(0)
        }
    }

    @Test
    fun testUpdates() {
        val columns = DatabaseConstants.KEY_LABEL + " = " + "?"
        val selectionArgs = arrayOf("Account 1")
        val values = ContentValues().apply {
            put(DatabaseConstants.KEY_LABEL, "Testing an update with this string")
        }

        assertThat(resolver.update(
            TransactionProvider.ACCOUNTS_URI,
            values,
            columns,
            selectionArgs
        )).isEqualTo(0)
        insertData()
        assertThat(resolver.update(
            TransactionProvider.ACCOUNTS_URI,
            values,
            columns,
            selectionArgs
        )).isEqualTo(1)
    }

    @Test
    fun testGrouping() {
        val projection = arrayOf(
            DatabaseConstants.KEY_ROWID,
            DatabaseConstants.KEY_GROUPING
        )
        insertData()
        val id = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        )!!.use {
            with(assertThat(it)) {
                hasCount(testAccounts.size)
                moveToFirst()
                hasString(1, Grouping.NONE.name)
            }
            it.getLong(0)
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
            arrayOf(DbUtils.fqcn(DatabaseConstants.TABLE_ACCOUNTS, DatabaseConstants.KEY_GROUPING)),
            null,
            null,
            null
        )!!.use {
            with(assertThat(it)) {
                moveToFirst()
                hasString(0, Grouping.YEAR.name)
            }
        }
    }

    @Test
    fun testSortDirection() {
        val projection = arrayOf(
            DatabaseConstants.KEY_ROWID,
            DatabaseConstants.KEY_SORT_DIRECTION
        )
        insertData()
        val id = resolver.query(
            TransactionProvider.ACCOUNTS_URI,
            projection,
            null,
            null,
            null
        )!!.use {
            with(assertThat(it)) {
                hasCount(testAccounts.size)
                moveToFirst()
                hasString(1, SortDirection.DESC.name)
            }
            it.getLong(0)
        }

        val accountIdUri =
            ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, id)

        resolver.update(
            accountIdUri.buildUpon().appendPath("sortDirection").appendPath(SortDirection.ASC.name)
                .build(),
            null, null, null
        )
        resolver.query(
            accountIdUri, arrayOf(DatabaseConstants.KEY_SORT_DIRECTION),
            null,
            null,
            null
        )!!.use {
            with(assertThat(it)) {
                moveToFirst()
                hasString(0, SortDirection.ASC.name)
            }
        }
    }

    @Test
    fun testQueryWithSum() {
        val id = resolver.insert(
            TransactionProvider.ACCOUNTS_URI,
            AccountInfo("Account 0", AccountType.CASH, 0).contentValues
        ).let {
            ContentUris.parseId(it!!)
        }
        resolver.query(
            TransactionProvider.ACCOUNTS_FULL_URI,
            null,
            "${DatabaseConstants.KEY_ROWID} = ?",
            arrayOf(id.toString()),
            null
        )!!.use {
            //TODO insert transactions and test calculated sum
            with(assertThat(it)) {
                hasCount(1)
                moveToFirst()
                hasLong(it.getColumnIndex(KEY_SUM_EXPENSES), 0)
            }
        }
    }
    
    //TODO test query with aggregate accounts
}