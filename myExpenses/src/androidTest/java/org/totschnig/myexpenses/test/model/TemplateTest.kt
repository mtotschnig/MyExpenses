/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.test.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.*

class TemplateTest : ModelTest() {
    private lateinit var mAccount1: Account
    private lateinit var mAccount2: Account
    private var categoryId: Long = 0
    private var payeeId: Long = 0

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mAccount1 = Account("TestAccount 1", 100, "Main account")
        mAccount1.save()
        mAccount2 = Account("TestAccount 2", 100, "Secondary account")
        mAccount2.save()
        categoryId = writeCategory("TestCategory", null)
        payeeId = Payee.maybeWrite("N.N")
    }

    fun testTemplateFromTransaction() {
        val start = mAccount1.totalBalance.amountMinor
        val amount = 100.toLong()
        val op1 = Transaction.getNewInstance(mAccount1.id)!!
        op1.amount = Money(mAccount1.currencyUnit, amount)
        op1.comment = "test transaction"
        op1.save()
        assertThat(mAccount1.totalBalance.amountMinor).isEqualTo(start + amount)
        val t = Template(op1, "Template")
        t.save()
        val op2: Transaction = Transaction.getInstanceFromTemplate(t.id)
        op2.save()
        assertThat(mAccount1.totalBalance.amountMinor).isEqualTo(start + 2 * amount)
        val restored: Template? = Template.getInstanceFromDb(t.id)
        assertThat(restored).isEqualTo(t)
        Template.delete(t.id, false)
        assertWithMessage("Template deleted, but can still be retrieved").that(Template.getInstanceFromDb(t.id)).isNull()
    }

    fun testTemplate() {
        val template = buildTemplate()
        val restored = Template.getInstanceFromDb(template.id)
        assertThat(restored).isEqualTo(template)
    }

    fun testTransactionFromTemplate() {
        val template = buildTemplate()
        val transaction = Transaction.getInstanceFromTemplate(template)
        assertThat(transaction.catId).isEqualTo(template.catId)
        assertThat(transaction.accountId).isEqualTo(template.accountId)
        assertThat(transaction.payeeId).isEqualTo(template.payeeId)
        assertThat(transaction.methodId).isEqualTo(template.methodId)
        assertThat(transaction.comment).isEqualTo(template.comment)
    }

    fun testGetTypedNewInstanceTransaction() {
        newInstanceTestHelper(Transactions.TYPE_TRANSACTION)
    }

    fun testGetTypedNewInstanceTransfer() {
        newInstanceTestHelper(Transactions.TYPE_TRANSFER)
    }

    fun testGetTypedNewInstanceSplit() {
        newInstanceTestHelper(Transactions.TYPE_SPLIT)
    }

    private fun newInstanceTestHelper(type: Int) {
        val t: Template = Template.getTypedNewInstance(type, mAccount1.id, false, null)!!.apply {
            title = "Template"
            save()
        }
        assertThat(t.operationType()).isEqualTo(type)
        assertThat(Template.getInstanceFromDb(t.id)).isEqualTo(t)
    }

    private fun buildTemplate() = Template(mAccount1, Transactions.TYPE_TRANSACTION, null).apply {
        catId = this@TemplateTest.categoryId
        payeeId = this@TemplateTest.payeeId
        comment = "Some comment"
        PaymentMethod.find(PaymentMethod.PreDefined.CHEQUE.name).let {
            assertThat(it).isGreaterThan(-1)
            methodId = it
        }
        save()
    }
}