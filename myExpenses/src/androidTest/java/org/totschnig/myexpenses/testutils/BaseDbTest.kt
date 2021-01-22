@file:Suppress("DEPRECATION")

package org.totschnig.myexpenses.testutils

import android.database.sqlite.SQLiteDatabase
import org.totschnig.myexpenses.provider.DatabaseConstants


open class BaseDbTest : BaseProviderTest() {
    // Contains an SQLite database, used as test data
    lateinit var mDb: SQLiteDatabase

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mDb = provider.openHelperForTest.writableDatabase
    }

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        mDb.delete(DatabaseConstants.TABLE_ACCOUNTS, null, null)
        mDb.delete(DatabaseConstants.TABLE_PAYEES, null, null)
        mDb.delete(DatabaseConstants.TABLE_CATEGORIES, DatabaseConstants.KEY_ROWID + " != ?", arrayOf(DatabaseConstants.SPLIT_CATID.toString()))
        mDb.delete(DatabaseConstants.TABLE_TEMPLATES, null, null)
    }
}
