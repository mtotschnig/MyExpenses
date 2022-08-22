package org.totschnig.myexpenses.viewmodel

import android.content.ContentUris
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.CategoryCriteria
import org.totschnig.myexpenses.provider.filter.CrStatusCriteria
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Category

@RunWith(AndroidJUnit4::class)
class MyExpensesViewModelTest: BaseViewModelTest() {

    private lateinit var viewModel: MyExpensesViewModel

    private val application: MyApplication
        get() = ApplicationProvider.getApplicationContext()

    private val repository: Repository
        get() = Repository(
            application,
            Mockito.mock(CurrencyContext::class.java),
            Mockito.mock(CurrencyFormatter::class.java),
            Mockito.mock(PrefHandler::class.java)
        )

    private lateinit var account1: Account
    private lateinit var account2: Account
    var openingBalance = 100L
    var expense1 = 10L
    var expense2 = 20L
    var income1 = 30L
    var income2 = 40L
    var transferP = 50L
    var transferN = 60L
    private var categoryId: Long = 0

    private fun writeCategory(label: String, parentId: Long?) =
        ContentUris.parseId(repository.saveCategory(Category(label = label, parentId = parentId))!!)

    private fun insertData() {
        account1 = Account("Account 1", openingBalance, "Account 1")
        account1.save()
        account2 = Account("Account 2", openingBalance, "Account 2")
        account2.save()
        categoryId = writeCategory(TEST_CAT, null)
        Transaction.getNewInstance(account1.id).apply {
            amount = Money(account1.currencyUnit, -expense1)
            crStatus = CrStatus.CLEARED
            save()
            amount = Money(account1.currencyUnit, -expense2)
            saveAsNew()
            amount = Money(account1.currencyUnit, income1)
            saveAsNew()
            amount = Money(account1.currencyUnit, income2)
            this.catId = categoryId
            saveAsNew()
        }

        Transfer.getNewInstance(account1.id, account2.id).apply {
            setAmount(Money(account1.currencyUnit, transferP))
            save()
            setAmount(Money(account1.currencyUnit, -transferN))
            saveAsNew()
        }
    }

    @Before
    fun setupViewModel() {
        viewModel = MyExpensesViewModel(ApplicationProvider.getApplicationContext())
        application.appComponent.inject(viewModel)
    }

    @Test
    fun testReset() {
        insertData()
        val initialTotalBalance = account1.totalBalance
        Truth.assertThat(count()).isEqualTo(6)
        viewModel.reset(account1,null, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null)
        Truth.assertThat(count()).isEqualTo(0)
        val resetAccount = Account.getInstanceFromDb(account1.id)
        Truth.assertThat(resetAccount.totalBalance).isEqualTo(initialTotalBalance)
    }


    @Test
    fun testResetWithFilterUpdateBalance() {
        insertData()
        val initialTotalBalance = account1.totalBalance
        Truth.assertThat(count()).isEqualTo(6)
        val filter = WhereFilter.empty().apply {
            put(CategoryCriteria(TEST_CAT, categoryId))
        }
        viewModel.reset(account1, filter, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null)
        Truth.assertThat(count()).isEqualTo(5) //1 Transaction deleted
        val resetAccount = Account.getInstanceFromDb(account1.id)
        Truth.assertThat(resetAccount.totalBalance).isEqualTo(initialTotalBalance)
    }

    @Test
    fun testResetWithFilterCreateHelper() {
        insertData()
        val initialTotalBalance = account1.totalBalance
        Truth.assertThat(count()).isEqualTo(6)
        Truth.assertThat(count(condition = "$KEY_CATID=$categoryId")).isEqualTo(1)
        Truth.assertThat(count(condition = "$KEY_STATUS=$STATUS_HELPER")).isEqualTo(0)

        val filter = WhereFilter.empty().apply {
            put(CategoryCriteria(TEST_CAT, categoryId))
        }
        viewModel.reset(account1, filter, Account.EXPORT_HANDLE_DELETED_CREATE_HELPER, null)

        Truth.assertThat(count()).isEqualTo(6) //-1 Transaction deleted;+1 helper
        Truth.assertThat(count(condition = "$KEY_CATID=$categoryId")).isEqualTo(0)
        Truth.assertThat(count(condition = "$KEY_STATUS=$STATUS_HELPER")).isEqualTo(1)
        val resetAccount = Account.getInstanceFromDb(account1.id)
        Truth.assertThat(resetAccount.totalBalance).isEqualTo(initialTotalBalance)
    }

    @Test
    fun testBalanceWithoutReset() {
        insertData()
        val initialCleared = account1.clearedBalance
        Truth.assertThat(initialCleared).isNotEqualTo(account1.reconciledBalance)
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(4)
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(0)
        Truth.assertThat(viewModel.balanceAccount(account1.id, false).getOrAwaitValue().isSuccess).isTrue()
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(0)
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(4)
        val balanceAccount = Account.getInstanceFromDb(account1.id)
        Truth.assertThat(balanceAccount.reconciledBalance).isEqualTo(initialCleared)
    }

    @Test
    fun testBalanceWithReset() {
        insertData()
        val initialCleared = account1.clearedBalance
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(4)
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(0)
        Truth.assertThat(viewModel.balanceAccount(account1.id, true).getOrAwaitValue().isSuccess).isTrue()
        Truth.assertThat(count(condition = "$KEY_CR_STATUS != '${CrStatus.UNRECONCILED.name}'")).isEqualTo(0)
        Truth.assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.UNRECONCILED.name}'")).isEqualTo(2)
        val balanceAccount = Account.getInstanceFromDb(account1.id)
        Truth.assertThat(balanceAccount.reconciledBalance).isEqualTo(initialCleared)
    }

    private fun count(id: Long = account1.id, condition: String? = null): Int {
        val selection = "$KEY_ACCOUNTID = ? ${condition?.let { " AND $it" } ?: ""}"
        return viewModel.contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf("count(*)"),
            selection,
            arrayOf(id.toString()),
            null
        )!!.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    /**
     * @return the sum of opening balance and all cleared and reconciled transactions for the account
     */
    private val Account.clearedBalance
        get() = Money(
            currencyUnit,
            openingBalance.amountMinor +
                    getTransactionSum(WhereFilter.empty().apply {
                        put(CrStatusCriteria(CrStatus.RECONCILED.name, CrStatus.CLEARED.name))
                    })
        )

    /**
     * @return the sum of opening balance and all reconciled transactions for the account
     */
    private val Account.reconciledBalance: Money
        get() = Money(
            currencyUnit,
            openingBalance.amountMinor +
                    getTransactionSum(WhereFilter.empty().apply {
                        put(CrStatusCriteria(CrStatus.RECONCILED.name))
                    })
        )

    companion object {
        const val TEST_CAT = "TestCat"
    }
}