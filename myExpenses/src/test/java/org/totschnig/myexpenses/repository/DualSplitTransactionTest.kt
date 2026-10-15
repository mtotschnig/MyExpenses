package org.totschnig.myexpenses.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.findSiblingParentId
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.transactionExists
import org.totschnig.myexpenses.db2.updateTransaction
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.shared_test.TransactionData
import org.totschnig.shared_test.assertTransaction

@RunWith(RobolectricTestRunner::class)
class DualSplitTransactionTest : BaseTestWithRepository() {
    private var accountA: Long = 0
    private var accountB: Long = 0

    @Before
    fun setUp() {
        accountA = insertAccount("Account A", 1000)
        accountB = insertAccount("Account B", 1000)
    }

    @Test
    fun testPromotionToDualSplit() {
        val uuid1 = generateUuid()
        val uuid2 = generateUuid()
        val parentUuid = generateUuid()

        val split = RepositoryTransaction(
            data = Transaction(
                accountId = accountA,
                amount = 300L,
                categoryId = SPLIT_CATID,
                uuid = parentUuid
            ),
            splitParts = listOf(
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 100L,
                        transferAccountId = accountB,
                        uuid = uuid1
                    ),
                    transferPeer = Transaction(
                        accountId = accountB,
                        amount = -100L,
                        transferAccountId = accountA,
                        uuid = uuid1
                    )
                ),
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 200L,
                        transferAccountId = accountB,
                        uuid = uuid2
                    ),
                    transferPeer = Transaction(
                        accountId = accountB,
                        amount = -200L,
                        transferAccountId = accountA,
                        uuid = uuid2
                    )
                )
            )
        )

        val result = repository.createTransaction(split)
        val idA = result.id

        // Get the generated peer IDs for the assertion
        val partsA = result.splitParts!!
        val peers1 = partsA.find { it.data.amount == 100L }!!.data.let { it.id to it.transferPeerId }
        val peers2 = partsA.find { it.data.amount == 200L }!!.data.let { it.id to it.transferPeerId }

        // Verify Account A
        repository.assertTransaction(
            idA,
            TransactionData(
                accountId = accountA,
                amount = 300L,
                splitParts = listOf(
                    TransactionData(accountId = accountA, amount = 100L, transferAccount = accountB, transferPeer = peers1.second),
                    TransactionData(accountId = accountA, amount = 200L, transferAccount = accountB, transferPeer = peers2.second)
                )
            )
        )

        // Verify that a sibling parent was created in Account B
        val idB = repository.findSiblingParentId(idA)
        assertThat(idB).isNotNull()
        assertThat(idB).isNotEqualTo(0L)

        repository.assertTransaction(
            idB!!,
            TransactionData(
                accountId = accountB,
                amount = -300L,
                splitParts = listOf(
                    TransactionData(accountId = accountB, amount = -100L, transferAccount = accountA, transferPeer = peers1.first),
                    TransactionData(accountId = accountB, amount = -200L, transferAccount = accountA, transferPeer = peers2.first)
                )
            )
        )

        // Verify peers are linked correctly
        val restoredA = repository.loadTransaction(idA)
        val restoredB = repository.loadTransaction(idB)

        assertThat(restoredA.splitParts).hasSize(2)
        assertThat(restoredB.splitParts).hasSize(2)

        restoredA.splitParts!!.forEach { partA ->
            val partB = restoredB.splitParts!!.find { it.data.uuid == partA.data.uuid }
            assertThat(partB).isNotNull()
            assertThat(partA.data.transferPeerId).isEqualTo(partB!!.id)
            assertThat(partB.data.transferPeerId).isEqualTo(partA.id)
        }
    }

    @Test
    fun testDemotionFromDualSplit() {
        val now = System.currentTimeMillis() / 1000
        // 1. Create a dual split
        val uuid1 = generateUuid()
        val parentUuid = generateUuid()
        val split = RepositoryTransaction(
            data = Transaction(
                accountId = accountA,
                amount = 100L,
                categoryId = SPLIT_CATID,
                uuid = parentUuid,
                date = now
            ),
            splitParts = listOf(
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 100L,
                        transferAccountId = accountB,
                        uuid = uuid1,
                        date = now
                    ),
                    transferPeer = Transaction(
                        accountId = accountB,
                        amount = -100L,
                        transferAccountId = accountA,
                        uuid = uuid1,
                        date = now
                    )
                )
            )
        )
        val idA = repository.createTransaction(split).id
        val idB = repository.findSiblingParentId(idA)!!

        assertThat(repository.transactionExists(idB)).isTrue()

        // 2. Update by adding a non-transfer part (Demotion)
        val restoredA = repository.loadTransaction(idA)
        val updatedSplit = restoredA.copy(
            data = restoredA.data.copy(amount = 150L),
            splitParts = (restoredA.splitParts ?: emptyList()) + RepositoryTransaction(
                data = Transaction(
                    accountId = accountA,
                    amount = 50L,
                    uuid = generateUuid(),
                    date = now
                )
            )
        )

        repository.updateTransaction(updatedSplit)

        // 3. Verify
        assertThat(repository.transactionExists(idB)).isFalse()

        val finalA = repository.loadTransaction(idA)
        assertThat(finalA.splitParts).hasSize(2)
        assertThat(finalA.splitParts!!.any { it.data.transferPeerId != null }).isTrue() // The transfer part stays linked

        // The transfer part in account B should still exist but have no parent
        val transferPartB = finalA.splitParts.first { it.data.transferPeerId != null }.data.transferPeerId!!
        val restoredPartB = repository.loadTransaction(transferPartB).data
        assertThat(restoredPartB.parentId).isNull()
        assertThat(restoredPartB.accountId).isEqualTo(accountB)
    }

    @Test
    fun testUpdateToDualSplit() {
        val now = System.currentTimeMillis() / 1000
        val uuid1 = generateUuid()
        val uuid2 = generateUuid()
        val parentUuid = generateUuid()

        // 1. Create a regular split (one transfer, one expense)
        val split = RepositoryTransaction(
            data = Transaction(
                accountId = accountA,
                amount = 150L,
                categoryId = SPLIT_CATID,
                uuid = parentUuid,
                date = now
            ),
            splitParts = listOf(
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 100L,
                        transferAccountId = accountB,
                        uuid = uuid1,
                        date = now
                    ),
                    transferPeer = Transaction(
                        accountId = accountB,
                        amount = -100L,
                        transferAccountId = accountA,
                        uuid = uuid1,
                        date = now
                    )
                ),
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 50L,
                        uuid = uuid2,
                        date = now
                    )
                )
            )
        )
        val idA = repository.createTransaction(split).id
        assertThat(repository.findSiblingParentId(idA)).isNull()

        // 2. Update to a dual split (change expense to transfer to accountB)
        val restoredA = repository.loadTransaction(idA)
        val updatedSplit = restoredA.copy(
            data = restoredA.data.copy(amount = 200L),
            splitParts = listOf(
                restoredA.splitParts!!.first { it.data.amount == 100L },
                RepositoryTransaction(
                    data = Transaction(
                        accountId = accountA,
                        amount = 100L,
                        transferAccountId = accountB,
                        uuid = uuid2,
                        date = now
                    ),
                    transferPeer = Transaction(
                        accountId = accountB,
                        amount = -100L,
                        transferAccountId = accountA,
                        uuid = uuid2,
                        date = now
                    )
                )
            )
        )
        repository.updateTransaction(updatedSplit)

        // 3. Verify promotion
        val idB = repository.findSiblingParentId(idA)
        assertThat(idB).isNotNull()

        val finalA = repository.loadTransaction(idA)
        val partsA = finalA.splitParts!!
        val peers1 = partsA.find { it.data.uuid == uuid1 }!!.data.let { it.id to it.transferPeerId }
        val peers2 = partsA.find { it.data.uuid == uuid2 }!!.data.let { it.id to it.transferPeerId }

        repository.assertTransaction(
            idB!!,
            TransactionData(
                accountId = accountB,
                amount = -200L,
                splitParts = listOf(
                    TransactionData(accountId = accountB, amount = -100L, transferAccount = accountA, transferPeer = peers1.first),
                    TransactionData(accountId = accountB, amount = -100L, transferAccount = accountA, transferPeer = peers2.first)
                )
            )
        )
    }
}
