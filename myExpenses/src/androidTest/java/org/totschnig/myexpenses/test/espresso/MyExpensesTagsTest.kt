package org.totschnig.myexpenses.test.espresso

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteAllTags
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup

@TestShard4
class MyExpensesTagsTest: BaseMyExpensesTest() {

    private lateinit var account: Account

    @Before
    fun fixture() {
        account = buildAccount("Test account 1")
        val id = repository.insertTransaction(
            accountId = account.id,
            amount = -1200L
        ).id
        val tagId = repository.writeTag("Good Tag")
        repository.saveTagsForTransaction(
            longArrayOf(tagId),
            id
        )
        launch(account.id)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            repository.deleteAllTags()
        }
    }

    @Test
    fun tagIsDisplayed() {
        assertTextAtPosition("Good Tag", 0)
    }
}