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
package org.totschnig.myexpenses.model

import android.content.ContentUris
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.viewmodel.data.Category

@RunWith(RobolectricTestRunner::class)
class TemplateTest: BaseTestWithRepository() {

    private lateinit var mAccount1: org.totschnig.myexpenses.model2.Account
    private lateinit var mAccount2: org.totschnig.myexpenses.model2.Account
    private var categoryId: Long = 0
    private var payeeId: Long = 0

    private fun writeCategory(label: String, parentId: Long?) =
        ContentUris.parseId(repository.saveCategory(Category(label = label, parentId = parentId))!!)

    @Before
    fun setUp() {
        mAccount1 = org.totschnig.myexpenses.model2.Account(
            label = "TestAccount 1",
            currency = CurrencyUnit.DebugInstance.code,
            openingBalance = 100,
            description = "Main account"
        ).createIn(repository)
        mAccount2 = org.totschnig.myexpenses.model2.Account(
            label = "TestAccount 2",
            currency = CurrencyUnit.DebugInstance.code,
            openingBalance = 100,
            description = "Secondary account"
        ).createIn(repository)
        categoryId = writeCategory("TestCategory", null)
        payeeId = Payee.maybeWrite("N.N")
    }

    @Test
    fun testTemplateFromTransaction() {
        val start = repository.getTransactionSum(mAccount1.id)
        val amount = 100.toLong()
        val op1 = Transaction.getNewInstance(mAccount1.id, CurrencyUnit.DebugInstance)!!
        op1.amount = Money(CurrencyUnit.DebugInstance, amount)
        op1.comment = "test transaction"
        op1.save()
        assertThat(repository.getTransactionSum(mAccount1.id)).isEqualTo(start + amount)
        val t = Template(op1, "Template")
        t.save()
        val op2: Transaction = Transaction.getInstanceFromTemplate(t.id)
        op2.save()
        assertThat(repository.getTransactionSum(mAccount1.id)).isEqualTo(start + 2 * amount)
        val restored: Template? = Template.getInstanceFromDb(t.id)
        assertThat(restored).isEqualTo(t)
        Template.delete(t.id, false)
        assertWithMessage("Template deleted, but can still be retrieved").that(Template.getInstanceFromDb(t.id)).isNull()
    }

    @Test
    fun testTemplate() {
        val template = buildTemplate()
        val restored = Template.getInstanceFromDb(template.id)
        assertThat(restored).isEqualTo(template)
    }

    @Test
    fun testTransactionFromTemplate() {
        val template = buildTemplate()
        val transaction = Transaction.getInstanceFromTemplate(template)
        assertThat(transaction.catId).isEqualTo(template.catId)
        assertThat(transaction.accountId).isEqualTo(template.accountId)
        assertThat(transaction.payeeId).isEqualTo(template.payeeId)
        assertThat(transaction.methodId).isEqualTo(template.methodId)
        assertThat(transaction.comment).isEqualTo(template.comment)
    }

    @Test
    fun testGetTypedNewInstanceTransaction() {
        newInstanceTestHelper(Transactions.TYPE_TRANSACTION)
    }

    @Test
    fun testGetTypedNewInstanceTransfer() {
        newInstanceTestHelper(Transactions.TYPE_TRANSFER)
    }

    @Test
    fun testGetTypedNewInstanceSplit() {
        newInstanceTestHelper(Transactions.TYPE_SPLIT)
    }

    private fun newInstanceTestHelper(type: Int) {
        val t: Template = Template.getTypedNewInstance(type, mAccount1.id, CurrencyUnit.DebugInstance, false, null)!!.apply {
            title = "Template"
            if (type == Transactions.TYPE_TRANSFER) {
                setTransferAccountId(mAccount2.id)
            }
            save()
        }
        assertThat(t.operationType()).isEqualTo(type)
        assertThat(Template.getInstanceFromDb(t.id)).isEqualTo(t)
    }

    private fun buildTemplate() = Template(mAccount1.id, CurrencyUnit.DebugInstance, Transactions.TYPE_TRANSACTION, null).apply {
        catId = this@TemplateTest.categoryId
        payeeId = this@TemplateTest.payeeId
        comment = "Some comment"
        PaymentMethod.find(PreDefinedPaymentMethod.CHEQUE.name).let {
            assertThat(it).isGreaterThan(-1)
            methodId = it
        }
        save()
    }
}