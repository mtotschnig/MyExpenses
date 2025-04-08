package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import com.google.common.truth.Truth
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.PayeeInfo
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.provider.query
import org.totschnig.myexpenses.provider.update
import org.totschnig.myexpenses.testutils.BaseDbTest

class PartyTest: BaseDbTest() {

    /**
     * When a party that has duplicates is merged into another party as duplicate,
     * all its duplicates must be updated to refer to the new parent.
     * Test if this requirement is addressed via triggers
     */
    fun testFlattenDuplicateHierarchy() {
        val payeeId1 = mDb.insert(
            TABLE_PAYEES,
            PayeeInfo("A.A.").contentValues
        )
        val payeeId2 = mDb.insert(
            TABLE_PAYEES,
            PayeeInfo("Aa.Aa.").contentValues
        )
        val payeeId3 = mDb.insert(
            TABLE_PAYEES,
            PayeeInfo("Aaa.Aaa.", payeeId2).contentValues
        )
        mDb.update(TABLE_PAYEES, ContentValues(1).apply { put (KEY_PARENTID, payeeId1) }, "$KEY_ROWID = ?", arrayOf(payeeId2.toString()))
        Truth.assertThat(
            mDb.query(TABLE_PAYEES, arrayOf(KEY_PARENTID),  "$KEY_ROWID = ?", arrayOf(payeeId3.toString()))
                .use {
                    it.moveToFirst()
                    it.getLong(0)
                }
        ).isEqualTo(payeeId1)
    }

}