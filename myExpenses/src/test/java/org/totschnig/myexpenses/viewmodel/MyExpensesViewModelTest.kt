package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_CREATE_HELPER
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE

@RunWith(AndroidJUnit4::class)
class MyExpensesViewModelTest: BaseViewModelTest() {

    private lateinit var viewModel: MyExpensesViewModel

    private val application: MyApplication
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var account1: Account
    private val openingBalance = 100L
    private val expense1 = 10L
    private val expense2 = 20L
    private val income1 = 30L
    private val income2 = 40L
    private val transferP = 50L
    private val transferN = 60L
    private var categoryId: Long = 0

    private fun insertData() {
        account1 = Account(label = "Account 1", openingBalance = openingBalance, currency = CurrencyUnit.DebugInstance.code)
            .createIn(repository)
        val account2 = Account(label = "Account 2", openingBalance = openingBalance, currency = CurrencyUnit.DebugInstance.code)
            .createIn(repository)
        categoryId = writeCategory(TEST_CAT, null)
        Transaction.getNewInstance(account1.id, CurrencyUnit.DebugInstance).apply {
            amount = Money(CurrencyUnit.DebugInstance, -expense1)
            crStatus = CrStatus.CLEARED
            save(contentResolver)
            amount = Money(CurrencyUnit.DebugInstance, -expense2)
            saveAsNew(contentResolver)
            amount = Money(CurrencyUnit.DebugInstance, income1)
            saveAsNew(contentResolver)
            amount = Money(CurrencyUnit.DebugInstance, income2)
            this.catId = categoryId
            saveAsNew(contentResolver)
        }

        Transfer.getNewInstance(account1.id, CurrencyUnit.DebugInstance, account2.id).apply {
            setAmount(Money(CurrencyUnit.DebugInstance, transferP))
            save(contentResolver)
            setAmount(Money(CurrencyUnit.DebugInstance, -transferN))
            saveAsNew(contentResolver)
        }
    }

    @Before
    fun setupViewModel() {
        viewModel = MyExpensesViewModel(ApplicationProvider.getApplicationContext(), SavedStateHandle())
        application.appComponent.inject(viewModel)
    }

    private fun getTotalAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(account)

    private fun getReconciledAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(account,
                    WhereFilter.empty().put(CrStatusCriterion(arrayOf(CrStatus.RECONCILED)))
                )

    private fun getClearedAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(account,
                    WhereFilter.empty().put(CrStatusCriterion(arrayOf(CrStatus.CLEARED)))
                )

    @Test
    fun testReset() {
        insertData()
        val initialTotalBalance = getTotalAccountBalance(account1)
        assertThat(count()).isEqualTo(6)
        viewModel.reset(account1,null, EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null)
        assertThat(count()).isEqualTo(0)
        assertThat(getTotalAccountBalance(account1)).isEqualTo(initialTotalBalance)
    }


    @Test
    fun testResetWithFilterUpdateBalance() {
        insertData()
        val initialTotalBalance = getTotalAccountBalance(account1)
        assertThat(count()).isEqualTo(6)
        val filter = WhereFilter.empty().put(CategoryCriterion(TEST_CAT, categoryId))
        viewModel.reset(account1, filter, EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null)
        assertThat(count()).isEqualTo(5) //1 Transaction deleted
        assertThat(getTotalAccountBalance(account1)).isEqualTo(initialTotalBalance)
    }

    @Test
    fun testResetWithFilterCreateHelper() {
        insertData()
        val initialTotalBalance = getTotalAccountBalance(account1)
        assertThat(count()).isEqualTo(6)
        assertThat(count(condition = "$KEY_CATID=$categoryId")).isEqualTo(1)
        assertThat(count(condition = "$KEY_STATUS=$STATUS_HELPER")).isEqualTo(0)

        val filter = WhereFilter.empty().put(CategoryCriterion(TEST_CAT, categoryId))
        viewModel.reset(account1, filter, EXPORT_HANDLE_DELETED_CREATE_HELPER, null)

        assertThat(count()).isEqualTo(6) //-1 Transaction deleted;+1 helper
        assertThat(count(condition = "$KEY_CATID=$categoryId")).isEqualTo(0)
        assertThat(count(condition = "$KEY_STATUS=$STATUS_HELPER")).isEqualTo(1)
        assertThat(getTotalAccountBalance(account1)).isEqualTo(initialTotalBalance)
    }

    @Test
    fun testBalanceWithoutReset() {
        insertData()
        val initialCleared = getClearedAccountBalance(account1)
        assertThat(initialCleared).isNotEqualTo(getReconciledAccountBalance(account1))
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(4)
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(0)
        assertThat(viewModel.balanceAccount(account1.id, false).getOrAwaitValue().isSuccess).isTrue()
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(0)
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(4)
        assertThat(getReconciledAccountBalance(account1)).isEqualTo(initialCleared)
    }

    @Test
    fun testBalanceWithReset() {
        insertData()
        val initialCleared = getClearedAccountBalance(account1)
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.CLEARED.name}'")).isEqualTo(4)
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.RECONCILED.name}'")).isEqualTo(0)
        assertThat(viewModel.balanceAccount(account1.id, true).getOrAwaitValue().isSuccess).isTrue()
        assertThat(count(condition = "$KEY_CR_STATUS != '${CrStatus.UNRECONCILED.name}'")).isEqualTo(0)
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.UNRECONCILED.name}'")).isEqualTo(2)
        assertThat(getReconciledAccountBalance(account1)).isEqualTo(initialCleared)
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

    companion object {
        const val TEST_CAT = "TestCat"
    }
}