package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.findParty
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.epoch2LocalDate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

// Concrete implementation for testing
class TestImportDataViewModel(
    application: Application
) : ImportDataViewModel(application) {
    override val format: String = "TestFormat"
}

@RunWith(AndroidJUnit4::class)
class ImportDataViewModelTest : BaseTestWithRepository() {

    private lateinit var viewModel: ImportDataViewModel

    private val uri = "file:///data.qif".toUri()
    private val testCurrency = CurrencyUnit.DebugInstance

    @Before
    fun setUp() {
        // Provide a concrete implementation of the ViewModel with mocked dependencies
        viewModel = TestImportDataViewModel(ApplicationProvider.getApplicationContext())
        application.appComponent.inject(viewModel)
    }

    @Test
    fun `insertAccounts creates new account when it does not exist`() {
        // Arrange
        val newAccountName = "New Bank Account"
        val importAccounts = listOf(ImportAccount(memo = newAccountName))

        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(1)
    }

    @Test
    fun `insertAccounts reuses existing account if found`() {
        // Arrange
        val existingAccountName = "Existing Account"
        insertAccount(existingAccountName)
        val importAccounts = listOf(ImportAccount(memo = existingAccountName))
        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(0)
    }

    @Test
    fun `insertAccounts uses default name from URI if import account label is empty`() {
        // Arrange
        val importAccounts = listOf(ImportAccount(memo = "")) // Empty label


        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(1)
        assertThat(repository.findAnyOpenByLabel("data")).isNotNull()
    }

    @Test
    fun insertTransaction() {
        // Arrange
        val importAccounts = listOf(
            ImportAccount(
                memo = "test",
                transactions = listOf(
                    ImportTransaction(
                        date = Date(),
                        valueDate = null,
                        amount = BigDecimal.TEN,
                        payee = "Joe",
                        memo = "Comment",
                        category = "Grocery",
                        categoryClass = null,
                        toAccount = null,
                        toAmount = null,
                        status = "X",
                        number = "1",
                        method = null,
                        tags = null,
                        splits = null
                    )
                )
            )
        )


        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(1)
        val account = repository.findAnyOpenByLabel("test")
        assertThat(account).isNotNull()
        runBlocking {
            viewModel.insertCategories(setOf(CategoryInfo("Grocery")), false)
            viewModel.insertPayees(setOf("Joe"))
            viewModel.insertTransactions(importAccounts, testCurrency, false)
            val transactions = repository.loadTransactions(account!!)
            assertThat(transactions).hasSize(1)
            val party = repository.findParty("Joe")
            val category = repository.findCategory("Grocery")
            assertThat(party).isNotNull()
            assertThat(category).isNotNull()
            val transaction = transactions[0]
            assertThat(transaction.payeeId).isEqualTo(party)
            assertThat(transaction.comment).isEqualTo("Comment")
            assertThat(transaction.categoryId).isEqualTo(category)
            assertThat(transaction.amount).isEqualTo(1000)
            assertThat(transaction.crStatus).isEqualTo(CrStatus.RECONCILED)
            assertThat(epoch2LocalDate(transaction.date)).isEqualTo(LocalDate.now())
            assertThat(transaction.referenceNumber).isEqualTo("1")
        }
    }

    @Test
    fun insertTransfer() {
        // Arrange
        val date = Date()
        val importAccounts = listOf(
            ImportAccount(memo = "test2",
                transactions = listOf(
                    ImportTransaction(
                        date = date,
                        valueDate = null,
                        amount = BigDecimal.TEN.negate(),
                        payee = null,
                        memo = "Comment",
                        category = null,
                        categoryClass = null,
                        toAccount = "test",
                        toAmount = null,
                        status = "X",
                        number = null,
                        method = null,
                        tags = null,
                        splits = null
                    ))
            ),
            ImportAccount(
                memo = "test",
                transactions = listOf(
                    ImportTransaction(
                        date = date,
                        valueDate = null,
                        amount = BigDecimal.TEN,
                        payee = null,
                        memo = "Comment",
                        category = null,
                        categoryClass = null,
                        toAccount = "test2",
                        toAmount = null,
                        status = "X",
                        number = null,
                        method = null,
                        tags = null,
                        splits = null
                    )
                )
            )
        )


        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(2)
        val account = repository.findAnyOpenByLabel("test")
        val transferAccount = repository.findAnyOpenByLabel("test2")
        assertThat(account).isNotNull()
        assertThat(transferAccount).isNotNull()
        runBlocking {
            viewModel.insertTransactions(importAccounts, testCurrency, false)
            val transactions = repository.loadTransactions(account!!)
            assertThat(transactions).hasSize(1)
            val transaction = transactions[0]
            assertThat(transaction.comment).isEqualTo("Comment")
            assertThat(transaction.amount).isEqualTo(1000)
            assertThat(transaction.crStatus).isEqualTo(CrStatus.RECONCILED)
            assertThat(epoch2LocalDate(transaction.date)).isEqualTo(LocalDate.now())
            assertThat(transaction.transferAccountId).isEqualTo(transferAccount)
            assertThat(transaction.transferPeerId).isNotNull()
        }
    }

    @Test
    fun insertSplitTransaction() {
        // Arrange
        val importAccounts = listOf(
            ImportAccount(
                memo = "test",
                transactions = listOf(
                    ImportTransaction(
                        date = Date(),
                        valueDate = null,
                        amount = BigDecimal.TEN,
                        payee = "Joe",
                        memo = "Comment",
                        category = null,
                        categoryClass = null,
                        toAccount = null,
                        toAmount = null,
                        status = "X",
                        number = "1",
                        method = null,
                        tags = null,
                        splits = listOf(
                            ImportTransaction(
                                date = Date(),
                                valueDate = null,
                                amount = BigDecimal(5),
                                payee = null,
                                memo = "Comment 1",
                                category = "Grocery",
                                categoryClass = null,
                                toAccount = null,
                                toAmount = null,
                                status = "X",
                                number = "1",
                                method = null,
                                tags = null,
                                splits = null
                            ),
                            ImportTransaction(
                                date = Date(),
                                valueDate = null,
                                amount = BigDecimal(5),
                                payee = null,
                                memo = "Comment 2",
                                category = "Sports",
                                categoryClass = null,
                                toAccount = null,
                                toAmount = null,
                                status = "X",
                                number = "1",
                                method = null,
                                tags = null,
                                splits = null
                            )
                        )
                    )
                )
            )
        )


        // Act
        val importCount = viewModel.insertAccounts(importAccounts, testCurrency, uri)

        // Assert
        assertThat(importCount).isEqualTo(1)
        val account = repository.findAnyOpenByLabel("test")
        assertThat(account).isNotNull()
        runBlocking {
            viewModel.insertCategories(setOf(CategoryInfo("Grocery"), CategoryInfo("Sports")), false)
            viewModel.insertPayees(setOf("Joe"))
            viewModel.insertTransactions(importAccounts, testCurrency, false)
            val transactions = repository.loadTransactions(account!!)
            assertThat(transactions).hasSize(1)
            val party = repository.findParty("Joe")
            val category1 = repository.findCategory("Grocery")
            val category2 = repository.findCategory("Sports")
            assertThat(party).isNotNull()
            assertThat(category1).isNotNull()
            assertThat(category2).isNotNull()
            val transaction = transactions[0]
            assertThat(transaction.payeeId).isEqualTo(party)
            assertThat(transaction.comment).isEqualTo("Comment")
            assertThat(transaction.isSplit).isTrue()
            assertThat(transaction.amount).isEqualTo(1000)
            assertThat(transaction.crStatus).isEqualTo(CrStatus.RECONCILED)
            assertThat(epoch2LocalDate(transaction.date)).isEqualTo(LocalDate.now())
            assertThat(transaction.referenceNumber).isEqualTo("1")
            val splitTransaction = repository.loadTransaction(transaction.id)
            val parts = splitTransaction.splitParts
            assertThat(parts).hasSize(2)
            assertThat(parts!!.count { (data, _, _) ->
                data.amount == 500L && data.comment == "Comment 1" && data.categoryId == category1
            }).isEqualTo(1)
            assertThat(parts.count { (data, _, _) ->
                data.amount == 500L && data.comment == "Comment 2" && data.categoryId == category2
            }).isEqualTo(1)
        }
    }
}
