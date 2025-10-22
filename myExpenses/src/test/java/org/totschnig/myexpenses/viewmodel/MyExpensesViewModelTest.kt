package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_CREATE_HELPER
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE

@RunWith(AndroidJUnit4::class)
class MyExpensesViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: MyExpensesViewModel

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
        val accountTypeCash = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        account1 = Account(
            label = "Account 1",
            openingBalance = openingBalance,
            currency = CurrencyUnit.DebugInstance.code,
            type = accountTypeCash
        )
            .createIn(repository)
        val account2 = Account(
            label = "Account 2",
            openingBalance = openingBalance,
            currency = CurrencyUnit.DebugInstance.code,
            type = accountTypeCash
        )
            .createIn(repository)
        categoryId = writeCategory(TEST_CAT, null)
        repository.insertTransaction(
            accountId = account1.id,
            amount = -expense1,
            crStatus = CrStatus.CLEARED
        )
        repository.insertTransaction(
            accountId = account1.id,
            amount = -expense2,
            crStatus = CrStatus.CLEARED
        )
        repository.insertTransaction(
            accountId = account1.id,
            amount = income1,
            crStatus = CrStatus.CLEARED
        )
        repository.insertTransaction(
            accountId = account1.id,
            amount = income2,
            crStatus = CrStatus.CLEARED,
            categoryId = categoryId
        )
        repository.insertTransfer(
            account1.id, account2.id, transferP
        )
        repository.insertTransfer(
            account1.id, account2.id, -transferN
        )
    }

    @Before
    fun setupViewModel() {
        viewModel =
            MyExpensesViewModel(ApplicationProvider.getApplicationContext(), SavedStateHandle())
        application.appComponent.inject(viewModel)
    }

    private fun getTotalAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(account)

    private fun getReconciledAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(
                    account,
                    CrStatusCriterion(listOf(CrStatus.RECONCILED))
                )

    private fun getClearedAccountBalance(account: Account) =
        repository.loadAccount(account.id)!!.openingBalance +
                repository.getTransactionSum(
                    account,
                    CrStatusCriterion(listOf(CrStatus.CLEARED))
                )

    @Test
    fun testReset() {
        insertData()
        val initialTotalBalance = getTotalAccountBalance(account1)
        assertThat(count()).isEqualTo(6)
        viewModel.reset(account1, null, EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null)
        assertThat(count()).isEqualTo(0)
        assertThat(getTotalAccountBalance(account1)).isEqualTo(initialTotalBalance)
    }


    @Test
    fun testResetWithFilterUpdateBalance() {
        insertData()
        val initialTotalBalance = getTotalAccountBalance(account1)
        assertThat(count()).isEqualTo(6)
        val filter = CategoryCriterion(TEST_CAT, categoryId)
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

        val filter = CategoryCriterion(TEST_CAT, categoryId)
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
        assertThat(
            viewModel.balanceAccount(account1.id, false).getOrAwaitValue().isSuccess
        ).isTrue()
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
        assertThat(count(condition = "$KEY_CR_STATUS != '${CrStatus.UNRECONCILED.name}'")).isEqualTo(
            0
        )
        assertThat(count(condition = "$KEY_CR_STATUS = '${CrStatus.UNRECONCILED.name}'")).isEqualTo(
            2
        )
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

    @Test
    fun ungroupSplit() {
        prefHandler.putString(PrefKey.HOME_CURRENCY, "USD")
        val account = Account(
            label = "Account 2",
            openingBalance = openingBalance,
            currency = CurrencyUnit.DebugInstance.code,
            type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        )
            .createIn(repository)
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account.id,
                amount = 10000,
                categoryId = DatabaseConstants.SPLIT_CATID,
                equivalentAmount = 5000
            ), listOf(
                Transaction(
                    accountId = account.id,
                    amount = 6000
                ),
                Transaction(
                    accountId = account.id,
                    amount = 4000
                )
            )
        )

        val splitRestored = repository.loadTransaction(split.data.id)
        assertThat(splitRestored.data.equivalentAmount).isEqualTo(5000)
        assertThat(viewModel.revokeSplit(split.data.id).getOrAwaitValue().isSuccess).isTrue()
        val part1Restored = repository.loadTransaction(split.splitParts!![0].data.id)
        assertThat(part1Restored.data.equivalentAmount).isEqualTo(3000)
        val part2Restored = repository.loadTransaction(split.splitParts[1].data.id)
        assertThat(part2Restored.data.equivalentAmount).isEqualTo(2000)
    }

    companion object {
        const val TEST_CAT = "TestCat"
    }
}