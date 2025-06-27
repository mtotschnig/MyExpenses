package org.totschnig.myexpenses.provider

import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.calculateNearestCommonAncestor

@RunWith(RobolectricTestRunner::class)

class NearestCommonAncestorTest  : BaseTestWithRepository() {
    private var testAccountId: Long = 0
    private var main1: Long = 0
    private var main2: Long = 0
    private var sub1: Long = 0
    private var sub2: Long = 0

    @Before
    fun setup() {
        testAccountId = insertAccount("Test account")
        main1 = writeCategory("Main", icon = "icon1")
        sub1 = writeCategory("Sub", main1)
        sub2 = writeCategory("Sub2", main1)
        main2 = writeCategory("Main2")
    }

    @Test
    fun calculateNearestCommonAncestorForSiblings() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub2)
        val nca = repository.calculateNearestCommonAncestor(splitId)!!
        Truth.assertThat(nca.first).isEqualTo("Main")
        Truth.assertThat(nca.second).isEqualTo("icon1")
    }

    @Test
    fun calculateNearestCommonAncestorForParentAndChild() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = main1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub2)
        val nca = repository.calculateNearestCommonAncestor(splitId)!!
        Truth.assertThat(nca.first).isEqualTo("Main")
        Truth.assertThat(nca.second).isEqualTo("icon1")
    }

    @Test
    fun calculateNoNearestCommonAncestor() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = main1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = main2)
        val nca = repository.calculateNearestCommonAncestor(splitId)
        Truth.assertThat(nca).isNull()
    }

    @Test
    fun shouldReturnNullForNotSplit() {
        val (transactionId, _) = insertTransaction(testAccountId, 100)
        val nca = repository.calculateNearestCommonAncestor(transactionId)
        Truth.assertThat(nca).isNull()
    }
}