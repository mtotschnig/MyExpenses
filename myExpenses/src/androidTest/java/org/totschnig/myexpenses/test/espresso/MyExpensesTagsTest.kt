package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesTagsTest: BaseMyExpensesTest() {

    @Before
    fun fixture() {
        val account = buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(CurrencyUnit.DebugInstance, -1200L)
        val id = ContentUris.parseId(op.save(contentResolver)!!)
        val tagId = repository.writeTag("Good Tag")
        contentResolver.saveTagsForTransaction(
            longArrayOf(tagId),
            id
        )
        launch(account.id)
    }

    @Test
    fun tagIsDisplayed() {
        assertTextAtPosition("Good Tag", 0)
    }
}