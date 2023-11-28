@file:Suppress("DEPRECATION")

package org.totschnig.myexpenses.testutils

import androidx.sqlite.db.SupportSQLiteDatabase
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.test.provider.AccountInfo


open class BaseDbTest : BaseProviderTest() {
    // Contains an SQLite database, used as test data
    lateinit var mDb: SupportSQLiteDatabase

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mDb = provider.openHelperForTest.writableDatabase
    }

    fun setupTestAccount(): Long = mDb.insert(
        DatabaseConstants.TABLE_ACCOUNTS,
        AccountInfo("Test account", AccountType.CASH, 0).contentValues
    )

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        mDb.delete(DatabaseConstants.TABLE_DEBTS, null, null)
        mDb.delete(DatabaseConstants.TABLE_ACCOUNTS, null, null)
        mDb.delete(DatabaseConstants.TABLE_PAYEES, null, null)
        mDb.delete(DatabaseConstants.TABLE_CATEGORIES, DatabaseConstants.KEY_ROWID + " != ?", arrayOf(DatabaseConstants.SPLIT_CATID.toString()))
        mDb.delete(DatabaseConstants.TABLE_TEMPLATES, null, null)
    }
}
