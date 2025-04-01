package org.totschnig.myexpenses.test.espresso

import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

@Ignore("TODO")
class MyExpensesMethodFilterTest: BaseMyExpensesTest() {
    private lateinit var account: Account

    @Before
    fun fixture() {
/*        val currency = CurrencyUnit.DebugInstance
        account =  buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.save(contentResolver)
        op.methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.CHEQUE.name)
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
        launch(account.id)*/
    }

    @Test
    fun methodFilter() {
        //TODO
    }

    private fun methodIsDisplayed(payee: String, position: Int) {
        assertTextAtPosition(payee, position)
    }

}