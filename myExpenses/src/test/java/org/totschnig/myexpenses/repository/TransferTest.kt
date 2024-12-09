package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class TransferTest : BaseTestWithRepository() {
    private var testAccount1: Long = 0
    private var testAccount2: Long = 0

    @Before
    fun setup() {
        testAccount1 = insertAccount("Test account 1")
        testAccount2 = insertAccount("Test account 2")
    }

    private fun verifyTransaction(
        id: Long,
        expectedTransferPeer: Long?,
        expectedAccountId: Long,
        expectedTransferAccount: Long?
    ) {
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, id),
            arrayOf(KEY_TRANSFER_PEER, KEY_ACCOUNTID, KEY_TRANSFER_ACCOUNT),
            null, null, null
        )!!.useAndAssert {
            movesToFirst()
            if (expectedTransferPeer != null) {
                hasLong(0, expectedTransferPeer)
            } else {
                isNull(0)
            }
            hasLong(1, expectedAccountId)
            if (expectedTransferAccount != null) {
                hasLong(2, expectedTransferAccount)
            } else {
                isNull(2)
            }
        }
    }

    @Test
    fun linkTransfer() {
        val (id1, uuid1) = insertTransaction(testAccount1, 100)
        val (id2, uuid2) = insertTransaction(testAccount2, -100)
        contentResolver.update(
            TransactionProvider.TRANSACTIONS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                .appendPath(uuid1)
                .build(),
            ContentValues(1).apply {
                put(KEY_UUID, uuid2)
            }, null, null
        )
        verifyTransaction(id1, id2, testAccount1, testAccount2)
        verifyTransaction(id2, id1, testAccount2, testAccount1)
    }

    @Test
    fun unlinkTransfer() {
        val transfer = Transfer.getNewInstance(testAccount1, CurrencyUnit.DebugInstance, testAccount2)
        transfer.setAmount(Money(CurrencyUnit.DebugInstance, 500))
        transfer.save(contentResolver)
        val transferPeer = (Transaction.getInstanceFromDb(contentResolver, transfer.id, null) as Transfer).transferPeer!!
        verifyTransaction(transfer.id, transferPeer, testAccount1, testAccount2)
        verifyTransaction(transferPeer, transfer.id, testAccount2, testAccount1)
        contentResolver.update(
            ContentUris.appendId(
                TransactionProvider.TRANSACTIONS_URI.buildUpon().appendPath(TransactionProvider.URI_SEGMENT_UNLINK_TRANSFER),
                transfer.id
            ).build(), null, null, null
        )
        verifyTransaction(transfer.id, null, testAccount1, null)
        verifyTransaction(transferPeer, null, testAccount2, null)
    }

    @Test
    fun transformToTransfer() {
        val id = insertTransaction(testAccount1, 100).first
        val transferPeer = ContentUris.parseId(contentResolver.insert(
            ContentUris.appendId(
                ContentUris.appendId(TransactionProvider.TRANSACTIONS_URI.buildUpon(), id)
                    .appendPath(TransactionProvider.URI_SEGMENT_TRANSFORM_TO_TRANSFER), testAccount2)
                .build(), null
        )!!)
        verifyTransaction(id, transferPeer, testAccount1, testAccount2)
        verifyTransaction(transferPeer, id, testAccount2, testAccount1)
    }
}