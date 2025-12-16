package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.transactionExists
import org.totschnig.myexpenses.db2.updateTransaction
import org.totschnig.myexpenses.db2.updateTransfer
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_USAGES
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.useAndMapToOne
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.shared_test.TransactionData
import org.totschnig.shared_test.assertTransaction
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class TransactionTest : BaseTransactionTest(withChangeTriggers = false)

@RunWith(RobolectricTestRunner::class)
class TransactionTestWithChangeTriggers : BaseTransactionTest(withChangeTriggers = true)

//allows to run tests either with or without change triggers
abstract class BaseTransactionTest(val withChangeTriggers: Boolean) : BaseTestWithRepository() {
    private var account1: Long = 0
    private var account2: Long = 0
    private var account3: Long = 0

    @Before
    fun setUp() {
        account1 = insertAccount(
            "TestAccount 1", 100,
            syncAccountName = if (withChangeTriggers) "DEBUG" else null
        )
        account2 = insertAccount(
            "TestAccount 2", 100,
            syncAccountName = if (withChangeTriggers) "DEBUG" else null
        )
        account3 = insertAccount(
            "TestAccount 3", 100,
            syncAccountName = if (withChangeTriggers) "DEBUG" else null
        )
        if (withChangeTriggers) {
            repository.contentResolver.update(
                TransactionProvider.CHANGES_URI.buildUpon()
                    .appendQueryParameter(KEY_ACCOUNTID, account1.toString())
                    .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_INIT)
                    .build(),
                ContentValues(0), null, null
            )
        }
    }

    @Test
    fun testTransaction() {
        val payee = "N.N"
        val start = repository.getSequenceCount()
        val payeeId = repository.requireParty(payee)
        val op1 = repository.insertTransaction(
            accountId = account1,
            amount = 100L,
            comment = "test transaction",
            payeeId = payeeId
        ).id
        assertThat(op1).isGreaterThan(0)
        assertThat(repository.getSequenceCount()).isEqualTo(start + 1)
        val restored = repository.loadTransaction(op1)
        assertThat(restored.isSplit).isFalse()
        assertThat(restored.isTransfer).isFalse()
        with(restored.data) {
            assertThat(amount).isEqualTo(100L)
            assertThat(comment).isEqualTo("test transaction")
            assertThat(this.payeeId).isEqualTo(payeeId)
        }
        //Transaction sequence should report on the number of transactions that have been created
        repository.deleteTransaction(op1, markAsVoid = false, inBulk = false)

        assertThat(repository.getSequenceCount()).isEqualTo(start + 1)
        assertThat(repository.transactionExists(op1)).isFalse()
    }

    @Test
    fun testTransfer() {
        val (transfer, peer) = repository.insertTransfer(
            account1,
            account2,
            100L,
            comment = "test transfer"
        )
        assertThat(transfer.id).isGreaterThan(0)
        assertThat(transfer.transferPeerId).isEqualTo(peer!!.id)
        assertThat(transfer.id).isEqualTo(peer.transferPeerId)
        assertThat(transfer.accountId).isEqualTo(account1)
        assertThat(transfer.transferAccountId).isEqualTo(account2)
        val restored = repository.loadTransaction(transfer.id)
        with(restored.data) {
            assertThat(amount).isEqualTo(100L)
            assertThat(comment).isEqualTo("test transfer")
            assertThat(transferPeerId).isEqualTo(peer.id)
        }
        assertThat(restored.transferPeer!!.id).isEqualTo(restored.data.transferPeerId)
        assertThat(restored.transferPeer.transferPeerId).isEqualTo(restored.data.id)

        val restoredPeer = repository.loadTransaction(peer.id)
        with(restoredPeer.data) {
            assertThat(amount).isEqualTo(-100L)
            assertThat(comment).isEqualTo("test transfer")
            assertThat(transferPeerId).isEqualTo(transfer.id)
        }
        assertThat(restoredPeer.transferPeer!!.id).isEqualTo(restoredPeer.data.transferPeerId)
        assertThat(restoredPeer.transferPeer.transferPeerId).isEqualTo(restoredPeer.data.id)
        repository.deleteTransaction(transfer.id, markAsVoid = false, inBulk = false)
        assertThat(repository.transactionExists(transfer.id)).isFalse()
        assertThat(repository.transactionExists(peer.id)).isFalse()
    }

    @Test
    fun testTransferChangeAccounts() {
        val (transfer, peer) = repository.insertTransfer(
            account1,
            account2,
            100L,
            comment = "test transfer"
        )

        val op1 = repository.loadTransaction(transfer.id).data
        val peer1 = repository.loadTransaction(peer!!.id).data

        repository.updateTransfer(
            op1.copy(accountId = account2, transferAccountId = account3),
            peer1.copy(accountId = account3, transferAccountId = account2)
        )

        val op2 = repository.loadTransaction(transfer.id).data
        val peer2 = repository.loadTransaction(peer.id).data

        assertThat(op2.accountId).isEqualTo(account2)
        assertThat(op2.transferAccountId).isEqualTo(account3)
        assertThat(op2.uuid).isEqualTo(transfer.uuid)
        assertThat(peer2.accountId).isEqualTo(account3)
        assertThat(peer2.transferAccountId).isEqualTo(account2)
        assertThat(peer2.uuid).isEqualTo(peer.uuid)
    }


    @Test
    fun testSplit() {
        val date = LocalDateTime.now().minusDays(1).toEpoch()
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split",
                date = date,
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ), listOf(
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    date = date,
                    uuid = generateUuid()
                ),
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    date = date,
                    uuid = generateUuid()
                )
            )
        )
        val op1 = split.data.id
        assertThat(split.splitParts).hasSize(2)
        split.splitParts!!.forEach {
            assertThat(it.data.parentId).isEqualTo(op1)
            assertThat(it.transferPeer).isNull()
        }
        val restored = repository.loadTransaction(op1)
        with(restored.data) {
            assertThat(amount).isEqualTo(100L)
            assertThat(comment).isEqualTo("test split")
            assertThat(this.date).isEqualTo(date)
        }
        assertThat(restored.splitParts).hasSize(2)
        restored.splitParts!!.forEach { (transaction, _) ->
            assertThat(transaction.amount).isEqualTo(50L)
            assertThat(transaction.date).isEqualTo(date)
            assertThat(transaction.parentId).isEqualTo(op1)
        }
        repository.updateTransaction(
            transaction = restored.data.copy(
                comment = "updated comment",
                crStatus = CrStatus.CLEARED
            )
        )
        restored.splitParts.forEach {
            assertThat(repository.loadTransaction(it.id)).isNotNull()
        }
    }

    @Test
    fun testUpdateSplit() {
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ), listOf(
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    uuid = generateUuid()
                ),
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    uuid = generateUuid()
                )
            )
        )
        val originalParts = split.splitParts!!.map { it.data.uuid }
        // we update one part, remove one and add one
        val updated = split.copy(
            splitParts = listOf(
                split.splitParts.first().let {
                    it.copy(data = it.data.copy(amount = 60L))
                },
                RepositoryTransaction(
                    data = Transaction(
                        accountId = account1,
                        amount = 40L,
                        uuid = generateUuid()
                    )
                )
            )
        )
        repository.updateTransaction(updated)
        val restored = repository.loadTransaction(updated.data.id)
        assertThat(restored.splitParts).hasSize(2)
        val originalPart = restored.splitParts!!.first { it.data.uuid == originalParts.first() }
        assertThat(originalPart.data.amount).isEqualTo(60L)
        assertThat(restored.splitParts.filter { it.data.uuid == originalParts[1] }).hasSize(0)
        val newPart = restored.splitParts.first { it.data.uuid != originalParts.first() }
        assertThat(newPart.data.amount).isEqualTo(40L)
    }

    @Test
    fun testUpdateSplitRemoveTransferPart() {
        val splitTransferUuid1 = generateUuid()
        val splitTransferUuid2 = generateUuid()
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 200L,
                comment = "test split with transfer",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = account1,
                    amount = 100L,
                    transferAccountId = account2,
                    uuid = splitTransferUuid1
                ) to Transaction(
                    accountId = account2,
                    amount = -100L,
                    transferAccountId = account1,
                    uuid = splitTransferUuid1
                ),
                Transaction(
                    accountId = account1,
                    amount = 100L,
                    transferAccountId = account2,
                    uuid = splitTransferUuid2
                ) to Transaction(
                    accountId = account2,
                    amount = -100L,
                    transferAccountId = account1,
                    uuid = splitTransferUuid2
                )
            )
        )
        val expectedPeer = split.splitParts!!.first().transferPeer!!.id
        // we remove one transfer part, and add a normal part
        val updated = split.copy(
            splitParts = listOf(split.splitParts.first(),
                RepositoryTransaction(
                    data = Transaction(
                        accountId = account1,
                        amount = 100L,
                        uuid = generateUuid()
                    )
                )
            )
        )
        repository.updateTransaction(updated)
        repository.assertTransaction(
            split.data.id,
            TransactionData(
                comment = "test split with transfer",
                accountId = account1,
                amount = 200L,
                splitParts = listOf(
                    TransactionData(
                        accountId = account1,
                        amount = 100L,
                        transferAccount = account2,
                        transferPeer = expectedPeer
                    ),
                    TransactionData(
                        accountId = account1,
                        amount = 100L,
                    )
                )
            )
        )
    }

    @Test
    fun testUpdateSplitAddTransferPart() {
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split with transfer",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = account1,
                    amount = 100L,
                    uuid = generateUuid()
                )
            )
        )

        // we add one transfer part
        val splitTransferUuid = generateUuid()
        val updated = split.copy(
            splitParts = listOf(
                split.splitParts!!.first().let {
                    it.copy(
                        data = it.data.copy(amount = 50L)
                    )
                },
                RepositoryTransaction(
                    data = Transaction(
                        accountId = account1,
                        amount = 50L,
                        transferAccountId = account2,
                        uuid = splitTransferUuid
                    ),
                    transferPeer = Transaction(
                        accountId = account2,
                        amount = -50L,
                        transferAccountId = account1,
                        uuid = splitTransferUuid
                    )
                )
            )
        )
        val results = repository.updateTransaction(updated)
        //0 delete old parts //1 update parent // 2 update first part // 3 insert transfer // 4 insert peer
        val expectedPeer = ContentUris.parseId(results[4].uri!!)
        repository.assertTransaction(
            split.data.id,
            TransactionData(
                comment = "test split with transfer",
                accountId = account1,
                amount = 100L,
                splitParts = listOf(
                    TransactionData(
                        accountId = account1,
                        amount = 50L,
                        transferAccount = account2,
                        transferPeer = expectedPeer
                    ),
                    TransactionData(
                        accountId = account1,
                        amount = 50L,
                    )
                )
            )
        )
    }

    @Test
    fun testSplitWithTransfer() {
        val splitTransferUuid = generateUuid()
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split with transfer",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = account1,
                    amount = 100L,
                    transferAccountId = account2,
                    uuid = splitTransferUuid
                ) to
                        Transaction(
                            accountId = account2,
                            amount = -100L,
                            transferAccountId = account1,
                            uuid = splitTransferUuid
                        )
            )
        )
        val op1 = split.data.id
        assertThat(split.splitParts).hasSize(1)
        val (transfer, peer) = split.splitParts!!.first()
        assertThat(transfer.parentId).isEqualTo(op1)
        assertThat(transfer.transferPeerId).isNotNull()
        assertThat(transfer.id).isNotNull()
        assertThat(transfer.transferPeerId).isEqualTo(peer!!.id)
        assertThat(peer.id).isEqualTo(transfer.transferPeerId)
        val restored = repository.loadTransaction(op1)
        with(restored.data) {
            assertThat(amount).isEqualTo(100L)
            assertThat(comment).isEqualTo("test split with transfer")
        }
        val splitParts = restored.splitParts
        assertThat(splitParts).hasSize(1)
        with(splitParts!!.first().data) {
            assertThat(amount).isEqualTo(100L)
            assertThat(parentId).isEqualTo(op1)
            assertThat(isTransfer).isTrue()
        }
    }

    @Test
    fun testDeleteSplitWithPartTransfer() {
        val splitTransferUuid = generateUuid()
        val op1 = repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split with transfer",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = account1,
                    amount = 100L,
                    transferAccountId = account2,
                    uuid = splitTransferUuid
                ) to
                        Transaction(
                            accountId = account2,
                            amount = -100L,
                            transferAccountId = account1,
                            uuid = splitTransferUuid
                        )
            )
        ).data.id
        repository.deleteTransaction(op1, markAsVoid = false, inBulk = false)
        assertThat(repository.transactionExists(op1)).isFalse()
    }

    @Test
    fun testIncreaseCatUsage() {
        val catId1 = writeCategory("Test category 1", null)
        val catId2 = writeCategory("Test category 2", null)
        assertThat(getCatUsage(catId1)).isEqualTo(0)
        assertThat(getCatUsage(catId2)).isEqualTo(0)
        val transaction1 = Transaction(
            accountId = account1,
            amount = 100L,
            categoryId = catId1,
            uuid = generateUuid()
        )
        val op1 = repository.createTransaction(transaction1).data
        //saving a new transaction increases usage
        assertThat(getCatUsage(catId1)).isEqualTo(1)
        assertThat(getCatUsage(catId2)).isEqualTo(0)
        repository.updateTransaction(op1.copy(comment = "Now with comment"))
        assertThat(getCatUsage(catId1)).isEqualTo(1)
        assertThat(getCatUsage(catId2)).isEqualTo(0)
        //updating category in transaction, does increase usage of new catId
        repository.updateTransaction(op1.copy(categoryId = catId2))
        assertThat(getCatUsage(catId1)).isEqualTo(1)
        assertThat(getCatUsage(catId2)).isEqualTo(1)
        //new transaction without cat, does not increase usage
        val transaction2 = Transaction(
            accountId = account1,
            amount = 100L,
            uuid = generateUuid()
        )
        val op2 = repository.createTransaction(transaction2).data
        assertThat(getCatUsage(catId1)).isEqualTo(1)
        assertThat(getCatUsage(catId2)).isEqualTo(1)
        repository.updateTransaction(op2.copy(categoryId = catId1))
        assertThat(getCatUsage(catId1)).isEqualTo(2)
        assertThat(getCatUsage(catId2)).isEqualTo(1)
    }

    @Test
    fun testIncreaseAccountUsage() {
        assertThat(getAccountUsage(account1)).isEqualTo(0)
        assertThat(getAccountUsage(account2)).isEqualTo(0)
        val transaction = Transaction(
            accountId = account1,
            amount = 100L,
            uuid = generateUuid()
        )
        val op1 = repository.createTransaction(
            transaction
        )
        assertThat(getAccountUsage(account1)).isEqualTo(1)
        //transfer
        repository.insertTransfer(
            account1,
            account2,
            100L,
            comment = "test transfer"
        )
        assertThat(getAccountUsage(account1)).isEqualTo(2)
        assertThat(getAccountUsage(account2)).isEqualTo(1)
        repository.updateTransaction(op1.data.copy(accountId = account2))
        assertThat(getAccountUsage(account2)).isEqualTo(2)
        //split
        repository.createSplitTransaction(
            Transaction(
                accountId = account1,
                amount = 100L,
                comment = "test split",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ), listOf(
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    uuid = generateUuid()
                ),
                Transaction(
                    accountId = account1,
                    amount = 50L,
                    uuid = generateUuid()
                )
            )
        )
        assertThat(getAccountUsage(account1)).isEqualTo(3)
    }


    private fun getCatUsage(catId: Long): Long {
        return getUsage(catId, TransactionProvider.CATEGORIES_URI)
    }

    private fun getAccountUsage(accountId: Long): Long {
        return getUsage(accountId, TransactionProvider.ACCOUNTS_URI)
    }

    private fun getUsage(catId: Long, baseUri: Uri) = repository.contentResolver.query(
        baseUri.buildUpon().appendPath(catId.toString()).build(),
        arrayOf(KEY_USAGES),
        null, null, null
    )!!.useAndMapToOne {
        it.getLong(0)
    }!!
}