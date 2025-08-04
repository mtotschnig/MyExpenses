@file:Suppress("DEPRECATION")

package org.totschnig.myexpenses.testutils

import androidx.sqlite.db.SupportSQLiteDatabase
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTACHMENTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES
import org.totschnig.myexpenses.provider.insert

open class BaseDbTest : BaseProviderTest() {
    // Contains an SQLite database, used as test data
    lateinit var mDb: SupportSQLiteDatabase

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mDb = provider.openHelperForTest.writableDatabase
    }

    fun setupTestAccount(): Long = mDb.insert(
        TABLE_ACCOUNTS,
        AccountInfo("Test account", cashAccount.id, 0).contentValues
    )

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        cleanup {
            mDb.delete(TABLE_DEBTS, null, emptyArray())
            mDb.delete(TABLE_ACCOUNTS, null, emptyArray())
            mDb.delete(TABLE_PAYEES, null, emptyArray())
            mDb.delete(TABLE_CATEGORIES, DatabaseConstants.KEY_ROWID + " != ?", arrayOf(DatabaseConstants.SPLIT_CATID.toString()))
            mDb.delete(TABLE_TEMPLATES, null, emptyArray())
            mDb.delete(TABLE_ATTACHMENTS, null, emptyArray())
        }
    }
}
