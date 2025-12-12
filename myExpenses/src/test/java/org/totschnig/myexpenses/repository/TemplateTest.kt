package org.totschnig.myexpenses.repository

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.createTemplate
import org.totschnig.myexpenses.db2.deleteTemplate
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.instantiateTemplate
import org.totschnig.myexpenses.db2.loadTemplate
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.writeTag
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.util.toEpochMillis
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.shared_test.TransactionData
import org.totschnig.shared_test.assertTransaction
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class TemplateTest : BaseTestWithRepository() {

    private var account1: Long = 0
    private var account2: Long = 0
    private var categoryId: Long = 0
    private var payeeId: Long = 0
    private var tagId: Long = 0

    @Before
    fun setUp() {
        account1 = insertAccount(
            label = "TestAccount 1",
            openingBalance = 100,
            currency = CurrencyUnit.DebugInstance.code
        )
        account2 = insertAccount(
            label = "TestAccount 2",
            currency = CurrencyUnit.DebugInstance.code,
            openingBalance = 100,
            description = "Secondary account"
        )
        categoryId = writeCategory("TestCategory", null)
        payeeId = repository.requireParty("N.N")!!
        tagId = repository.writeTag("Tag")
    }

    private fun RepositoryTemplate.instantiate(date: Long? = null) = runBlocking {
        repository.instantiateTemplate(
            exchangeRateHandler,
            PlanInstanceInfo(templateId = data.id, date = date),
            currencyContext
        )!!
    }


    @Test
    fun testTemplateFromTransaction() {
        val start = repository.getTransactionSum(account1)
        val amount = 100.toLong()
        val op1 = repository.insertTransaction(
            accountId = account1,
            amount = amount,
            comment = "test transaction"
        )
        assertThat(repository.getTransactionSum(account1)).isEqualTo(start + amount)
        val t =
            repository.createTemplate(RepositoryTemplate.fromTransaction(op1, "Test Transaction"))
        runBlocking {

        }
        t.instantiate()
        assertThat(repository.getTransactionSum(account1)).isEqualTo(start + 2 * amount)
        repository.deleteTemplate(t.id)
        Truth.assertWithMessage("Template deleted, but can still be retrieved")
            .that(repository.loadTemplate(t.id, require = false)).isNull()
    }

    @Test
    fun testTemplate() {
        val template = buildTransactionTemplate()
        with(repository.loadTemplate(template.id)!!) {
            assertThat(title).isEqualTo(template.title)
            assertThat(data.categoryId).isEqualTo(template.data.categoryId)
            assertThat(data.accountId).isEqualTo(template.data.accountId)
            assertThat(data.payeeId).isEqualTo(template.data.payeeId)
            assertThat(data.methodId).isEqualTo(template.data.methodId)
            assertThat(data.comment).isEqualTo(template.data.comment)
            assertThat(data.isSplit).isFalse()
            assertThat(data.isTransfer).isFalse()
        }
    }

    @Test
    fun testSplitTemplate() {
        val template = buildSplitTemplate()
        with(repository.loadTemplate(template.id)!!) {
            assertThat(title).isEqualTo(template.title)
            assertThat(data.comment).isEqualTo("Some comment parent")
            assertThat(splitParts).hasSize(1)
            assertThat(splitParts!!.first().data.comment).isEqualTo("Some comment part 0")
        }
    }

    @Test
    fun testTransactionFromTemplate() {
        val template = buildTransactionTemplate()
        val transaction = template.instantiate().id
        with(repository.loadTransaction(transaction).data) {
            assertThat(categoryId).isEqualTo(template.data.categoryId)
            assertThat(accountId).isEqualTo(template.data.accountId)
            assertThat(payeeId).isEqualTo(template.data.payeeId)
            assertThat(methodId).isEqualTo(template.data.methodId)
            assertThat(comment).isEqualTo(template.data.comment)
        }
    }

    @Test
    fun testTransferFromTemplate() {
        val template = buildTransferTemplate()
        val transaction = template.instantiate().id
        with(repository.loadTransaction(transaction)) {
            assertThat(isTransfer).isTrue()
            assertThat(data.accountId).isEqualTo(template.data.accountId)
            assertThat(data.comment).isEqualTo(template.data.comment)
            assertThat(data.transferAccountId).isEqualTo(template.data.transferAccountId)
        }
    }

    @Test
    fun testSplitFromTemplate() {
        val template = buildSplitTemplate()
        val transaction = template.instantiate().id
        repository.assertTransaction(
            transaction,
            TransactionData(
                accountId = template.data.accountId,
                amount = template.data.amount,
                comment = template.data.comment,
                splitParts = listOf(
                    TransactionData(
                        comment = template.splitParts!!.first().data.comment,
                        category = template.splitParts.first().data.categoryId,
                        tags = listOf(tagId),
                        accountId = template.data.accountId,
                        amount = template.splitParts.first().data.amount
                    )
                )
            )
        )
    }

    @Test
    fun testSplitFromTemplateWithTransferPart() = runTest {
        val template = buildSplitTemplate(withTransferPart = true, splitPartCount = 2)
        val transaction = template.instantiate().id
        val peers = repository.loadTransactions(account2).sortedBy { it.id }
        repository.assertTransaction(
            transaction,
            TransactionData(
                accountId = template.data.accountId,
                amount = template.data.amount,
                comment = template.data.comment,
                splitParts = List(2) {
                    val templatePart = template.splitParts!![it]
                    TransactionData(
                        comment = templatePart.data.comment,
                        category = templatePart.data.categoryId,
                        tags = listOf(tagId),
                        accountId = template.data.accountId,
                        amount = templatePart.data.amount,
                        transferAccount = account2,
                        transferPeer = peers[it].id
                    )
                }
            )
        )
    }

    @Test
    fun instantiateWithDate() {
        val template = buildTransactionTemplate()
        val dateInThePast = LocalDateTime.now().minusDays(30)
        val transaction = template.instantiate(dateInThePast.toEpochMillis()).data
        assertThat(transaction.date).isEqualTo(dateInThePast.toEpoch())
    }

    @Test
    fun instantiateTransferWithDate() {
        val template = buildTransferTemplate()
        val dateInThePast = LocalDateTime.now().minusDays(30)
        val transaction = template.instantiate(dateInThePast.toEpochMillis()).data
        assertThat(transaction.date).isEqualTo(dateInThePast.toEpoch())
    }

    @Test
    fun instantiateSplitWithDate() {
        val template = buildSplitTemplate()
        val dateInThePast = LocalDateTime.now().minusDays(30)
        val transaction = template.instantiate(dateInThePast.toEpochMillis()).data
        assertThat(transaction.date).isEqualTo(dateInThePast.toEpoch())
    }

    private fun buildTransactionTemplate() = repository.insertTemplate(
        accountId = account1,
        categoryId = categoryId,
        payeeId = payeeId,
        comment = "Some comment",
        methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.CHEQUE.name).also {
            assertThat(it).isGreaterThan(-1L)
        },
        title = "Template"
    )

    private fun buildTransferTemplate() = repository.insertTemplate(
        accountId = account1,
        transferAccountId = account2,
        comment = "Some comment",
        title = "Template"
    )

    private fun buildSplitTemplate(
        withTransferPart: Boolean = false,
        splitPartCount: Int = 1, // Add a parameter for how many parts to create
    ) = repository.createTemplate(
        RepositoryTemplate(
            data = Template(
                amount = splitPartCount * 50L,
                accountId = account1,
                comment = "Some comment parent",
                title = "Template",
                categoryId = SPLIT_CATID,
                uuid = generateUuid()
            ),
            splitParts = List(splitPartCount) { index -> // <-- THE SO
                RepositoryTemplate(
                    data = Template(
                        amount = 50L,
                        accountId = account1,
                        comment = "Some comment part $index",
                        title = "",
                        categoryId = if (withTransferPart) prefHandler.defaultTransferCategory else categoryId,
                        transferAccountId = if (withTransferPart) account2 else null,
                        uuid = generateUuid(),
                        tagList = listOf(tagId)
                    )
                )
            }
        )
    )
}