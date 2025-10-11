package org.totschnig.myexpenses.repository

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.createSplitTemplate
import org.totschnig.myexpenses.db2.createTemplate
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.deleteTemplate
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTemplate
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod

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
            currency = CurrencyUnit.Companion.DebugInstance.code
        )
        mAccount2 = insertAccount(
            label = "TestAccount 2",
            currency = CurrencyUnit.Companion.DebugInstance.code,
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
        val op1 = repository.insertTransaction(
            accountId = mAccount1,
            amount = amount,
            comment = "test transaction"
        )
        assertThat(repository.getTransactionSum(mAccount1)).isEqualTo(start + amount)
        val t = repository.createTemplate(RepositoryTemplate.Companion.fromTransaction(op1, "Test Transaction"))
        repository.createTransaction(t.instantiate())
        assertThat(repository.getTransactionSum(mAccount1)).isEqualTo(start + 2 * amount)
        val restored = repository.loadTemplate(t.id)
        assertThat(restored).isEqualTo(t)
        repository.deleteTemplate(t.id)
        Truth.assertWithMessage("Template deleted, but can still be retrieved").that(repository.loadTemplate(t.id)).isNull()
    }

    @Test
    fun testTemplate() {
        val template = buildTransactionTemplate()
        val restored = repository.loadTemplate(template.id)
        assertThat(restored).isEqualTo(template)
    }

    @Test
    fun testTransactionFromTemplate() {
        val template = buildTransactionTemplate()
        val transaction = template.instantiate().data
        assertThat(transaction.categoryId).isEqualTo(template.data.categoryId)
        assertThat(transaction.accountId).isEqualTo(template.data.accountId)
        assertThat(transaction.payeeId).isEqualTo(template.data.payeeId)
        assertThat(transaction.methodId).isEqualTo(template.data.methodId)
        assertThat(transaction.comment).isEqualTo(template.data.comment)
    }

    @Test
    fun testTransferFromTemplate() {
        val template = buildTransferTemplate()
        val transaction = template.instantiate().data
        assertThat(transaction.isTransfer).isTrue()
        assertThat(transaction.accountId).isEqualTo(template.data.accountId)
        assertThat(transaction.comment).isEqualTo(template.data.comment)
        assertThat(transaction.transferAccountId).isEqualTo(template.data.transferAccountId)
    }

    @Test
    fun testSplitFromTemplate() {
        val template = buildSplitTemplate()
        val transaction = template.instantiate().data
        assertThat(transaction.isSplit).isTrue()
        assertThat(transaction.accountId).isEqualTo(template.data.accountId)
        assertThat(transaction.comment).isEqualTo(template.data.comment)

        //TODO check split parts
 /*       contentResolver.query(
            TransactionProvider.UNCOMMITTED_URI,
            null,
            "${DatabaseConstants.KEY_PARENTID}=?",
            arrayOf(transaction.id.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(DatabaseConstants.KEY_CATID, categoryId)
        }*/
    }

    private fun buildTransactionTemplate() = repository.createTemplate(
        Template(
            accountId = mAccount1,
            categoryId = categoryId,
            payeeId = payeeId,
            comment = "Some comment",
            methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.CHEQUE.name).also {
                assertThat(it).isGreaterThan(-1L)
            },
            title = "Template"
        )
    )

    private fun buildTransferTemplate() = repository.createTemplate(
        Template(
            accountId = mAccount1,
            transferAccountId = mAccount2,
            comment = "Some comment",
            title = "Template"
        )
    )

    private fun buildSplitTemplate() = repository.createSplitTemplate(
        Template(
            accountId = mAccount1,
            comment = "Some comment",
            title = "Template"
        ), listOf(
            Template(
                accountId = mAccount1,
                comment = "Some comment",
                title = "",
                categoryId = categoryId
            )
        )
    )
}