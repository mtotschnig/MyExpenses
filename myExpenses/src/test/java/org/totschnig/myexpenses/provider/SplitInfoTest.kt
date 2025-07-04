package org.totschnig.myexpenses.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.calculateSplitSummary

@RunWith(RobolectricTestRunner::class)

class SplitInfoTest  : BaseTestWithRepository() {
    private var testAccountId: Long = 0
    private var main1: Long = 0
    private var main2: Long = 0
    private var sub1: Long = 0
    private var sub2: Long = 0
    private var sub3: Long = 0

    @Before
    fun setup() {
        testAccountId = insertAccount("Test account")
        main1 = writeCategory("Main", icon = "icon1")
        sub1 = writeCategory("Sub", main1, icon = "icon2")
        sub2 = writeCategory("Sub2", main1, icon = "icon3")
        main2 = writeCategory("Main2", icon = "icon4")
        sub3 = writeCategory("Sub3", main2, icon = "icon5")
    }

    @Test
    fun calculateForSiblings() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub2)
        val list = repository.calculateSplitSummary(splitId)!!
        assertThat(list.map { it.first }).containsExactly("Sub", "Sub2")
        assertThat(list.map { it.second }).containsExactly("icon2", "icon3")
    }

    @Test
    fun calculateForParentAndChild() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = main1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub2)
        val list = repository.calculateSplitSummary(splitId)!!
        assertThat(list.map { it.first }).containsExactly("Main", "Sub2")
        assertThat(list.map { it.second }).containsExactly("icon1", "icon3")
    }

    @Test
    fun calculateUnrelated() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub1)
        insertTransaction(testAccountId, 50, parentId = splitId, categoryId = sub3)
        val list = repository.calculateSplitSummary(splitId)!!
        assertThat(list.map { it.first }).containsExactly("Sub", "Sub3")
        assertThat(list.map { it.second }).containsExactly("icon2", "icon5")
    }

    @Test
    fun shouldReturnNullForNoIcon() {
        val (splitId, _) = insertTransaction(testAccountId, 100)
        insertTransaction(testAccountId, 100, parentId = splitId, categoryId = writeCategory("noIcon"))
        val list = repository.calculateSplitSummary(splitId)!!
        assertThat(list.map { it.first }).containsExactly("noIcon")
        assertThat(list.map { it.second }).containsExactly(null)
    }

    @Test
    fun shouldReturnNullForNotSplit() {
        val (transactionId, _) = insertTransaction(testAccountId, 100)
        assertThat(repository.calculateSplitSummary(transactionId)).isNull()
    }
}