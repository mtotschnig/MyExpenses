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
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class TemplateTest: BaseTestWithRepository() {

    private var mAccount1: Long = 0
    private var mAccount2: Long = 0
    private var categoryId: Long = 0
    private var payeeId: Long = 0

    @Before
    fun setUp() {
        mAccount1 = insertAccount(
            label = "TestAccount 1",
            openingBalance = 100,
            currency = CurrencyUnit.DebugInstance.code
        )
        mAccount2 = insertAccount(
            label = "TestAccount 2",
            currency = CurrencyUnit.DebugInstance.code,
            openingBalance = 100,
            description = "Secondary account"
        )
        categoryId = writeCategory("TestCategory", null)
        payeeId = repository.requireParty("N.N")!!
    }

    @Test
    fun testTemplateFromTransaction() {
        val start = repository.getTransactionSum(mAccount1)
        val amount = 100.toLong()
        val op1 = Transaction.getNewInstance(mAccount1, CurrencyUnit.DebugInstance)!!
        op1.amount = Money(CurrencyUnit.DebugInstance, amount)
        op1.comment = "test transaction"
        op1.save(contentResolver)
        assertThat(repository.getTransactionSum(mAccount1)).isEqualTo(start + amount)
        val t = Template(contentResolver, op1, "Template")
        t.save(contentResolver)
        val op2: Transaction = Transaction.getInstanceFromTemplate(contentResolver, t.id)
        op2.save(contentResolver)
        assertThat(repository.getTransactionSum(mAccount1)).isEqualTo(start + 2 * amount)
        val restored: Template? = Template.getInstanceFromDb(contentResolver, t.id)
        assertThat(restored).isEqualTo(t)
        Template.delete(contentResolver, t.id, false)
        assertWithMessage("Template deleted, but can still be retrieved").that(Template.getInstanceFromDb(contentResolver, t.id)).isNull()
    }

    @Test
    fun testTemplate() {
        val template = buildTransactionTemplate()
        val restored = Template.getInstanceFromDb(contentResolver, template.id)
        assertThat(restored).isEqualTo(template)
    }

    @Test
    fun testTransactionFromTemplate() {
        val template = buildTransactionTemplate()
        val transaction = Transaction.getInstanceFromTemplate(contentResolver, template)
        assertThat(transaction.catId).isEqualTo(template.catId)
        assertThat(transaction.accountId).isEqualTo(template.accountId)
        assertThat(transaction.party).isEqualTo(template.party)
        assertThat(transaction.methodId).isEqualTo(template.methodId)
        assertThat(transaction.comment).isEqualTo(template.comment)
    }

    @Test
    fun testTransferFromTemplate() {
        val template = buildTransferTemplate()
        val transaction = Transaction.getInstanceFromTemplate(contentResolver, template)
        assertThat(transaction).isInstanceOf(Transfer::class.java)
        assertThat(transaction.accountId).isEqualTo(template.accountId)
        assertThat(transaction.comment).isEqualTo(template.comment)
        assertThat(transaction.transferAccountId).isEqualTo(template.transferAccountId)
    }

    @Test
    fun testSplitFromTemplate() {
        val template = buildSplitTemplate()
        val transaction = Transaction.getInstanceFromTemplate(contentResolver, template)
        assertThat(transaction).isInstanceOf(SplitTransaction::class.java)
        assertThat(transaction.accountId).isEqualTo(template.accountId)
        assertThat(transaction.comment).isEqualTo(template.comment)

        contentResolver.query(
            TransactionProvider.UNCOMMITTED_URI,
            null,
            "$KEY_PARENTID=?",
            arrayOf(transaction.id.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_CATID, categoryId)
        }
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
        val t: Template = Template.getTypedNewInstance(contentResolver, type, mAccount1, CurrencyUnit.DebugInstance, false, null)!!.apply {
            title = "Template"
            if (type == Transactions.TYPE_TRANSFER) {
                setTransferAccountId(mAccount2)
            }
            save(contentResolver)
        }
        assertThat(t.operationType()).isEqualTo(type)
        assertThat(Template.getInstanceFromDb(contentResolver, t.id)).isEqualTo(t)
    }

    private fun buildTransactionTemplate() = Template(contentResolver, mAccount1, CurrencyUnit.DebugInstance, Transactions.TYPE_TRANSACTION, null).apply {
        catId = this@TemplateTest.categoryId
        party = DisplayParty(payeeId, "N.N")
        comment = "Some comment"
        repository.findPaymentMethod(PreDefinedPaymentMethod.CHEQUE.name).let {
            assertThat(it).isGreaterThan(-1L)
            methodId = it
        }
        save(contentResolver)
    }

    private fun buildTransferTemplate() = Template(contentResolver, mAccount1, CurrencyUnit.DebugInstance, Transactions.TYPE_TRANSFER, null).apply {
        comment = "Some comment"
        setTransferAccountId(mAccount2)
        save(contentResolver)
    }

    private fun buildSplitTemplate() = Template(contentResolver, mAccount1, CurrencyUnit.DebugInstance, Transactions.TYPE_SPLIT, null).apply {
        comment = "Some comment"
        val parentId = ContentUris.parseId(save(contentResolver)!!)
        val part = Template(contentResolver, mAccount1, CurrencyUnit.DebugInstance, Transactions.TYPE_SPLIT, parentId)
        part.catId = this@TemplateTest.categoryId
        part.save(contentResolver, true)
    }
}