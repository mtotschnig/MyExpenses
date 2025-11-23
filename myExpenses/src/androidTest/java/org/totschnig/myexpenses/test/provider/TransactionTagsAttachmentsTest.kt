package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.KEY_ATTACHMENT_COUNT
import org.totschnig.myexpenses.provider.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_TAGID
import org.totschnig.myexpenses.provider.KEY_TAGLIST
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_URI
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.TABLE_ATTACHMENTS
import org.totschnig.myexpenses.provider.TABLE_TAGS
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS_TAGS
import org.totschnig.myexpenses.provider.TABLE_TRANSACTION_ATTACHMENTS
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

class TransactionTagsAttachmentsTest: BaseDbTest() {
    private var testAccountId: Long = 0

    @Deprecated("Deprecated in Java")
    override fun setUp() {
        super.setUp()
        testAccountId = setupTestAccount()
    }


    /**
     * Bug https://github.com/mtotschnig/MyExpenses/issues/1325
     */
    fun testAggregateJoinedTables() {
        val transactionId = mDb.insert(
            TABLE_TRANSACTIONS,
            TransactionInfo(
                testAccountId, 1000,
                comment = "Transaction with associated data"
            ).contentValues
        )
        verifyAggregateJoinValuesHelper(null, 0)
        val tagA = insertTag("A", transactionId)
        insertAttachment("content:://one", transactionId)
        verifyAggregateJoinValuesHelper(tagA.toString(), 1)
        val tagB = insertTag("B", transactionId)
        insertAttachment("content:://two", transactionId)
        verifyAggregateJoinValuesHelper("$tagA\u001F$tagB", 2)
    }

    private fun verifyAggregateJoinValuesHelper(
        tagList: String?,
        attachmentCount: Int
    ) {
        val projection = arrayOf(
            KEY_TAGLIST,
            KEY_ATTACHMENT_COUNT
        )
        mockContentResolver.query(
            TransactionProvider.EXTENDED_URI,
            projection,
            null,
            null,
            null
        ).useAndAssert {
            movesToFirst()
            if (tagList == null) {
                isNull(0)
            } else {
                hasString(0, tagList)
            }
            hasInt(1, attachmentCount)
        }
    }

    private fun insertTag(tag: String, transactionId: Long) = mDb.insert(
        TABLE_TRANSACTIONS_TAGS,
        ContentValues(2).apply {
            put(
                KEY_TAGID, mDb.insert(
                    TABLE_TAGS,
                ContentValues(1).apply {
                    put(KEY_LABEL, tag)
                }
            ))
            put(KEY_TRANSACTIONID, transactionId)
        }
    )

    private fun insertAttachment(uri: String, transactionId: Long) = mDb.insert(
        TABLE_TRANSACTION_ATTACHMENTS,
        ContentValues(2).apply {
            put(
                KEY_ATTACHMENT_ID, mDb.insert(
                    TABLE_ATTACHMENTS,
                ContentValues(1).apply {
                    put(KEY_URI, uri)
                    put(KEY_UUID, generateUuid())
                }
            ))
            put(KEY_TRANSACTIONID, transactionId)
        }
    )
}