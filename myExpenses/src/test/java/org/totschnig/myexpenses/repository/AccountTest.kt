package org.totschnig.myexpenses.repository

import android.database.sqlite.SQLiteConstraintException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.setAccountProperty
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_SEALED

@RunWith(RobolectricTestRunner::class)
class AccountTest: BaseTestWithRepository() {

    @Test(expected = SQLiteConstraintException::class)
    fun deleteTransactionInSealedAccount() {
        val currencyUnit = CurrencyUnit.DebugInstance
        val account = insertAccount(label= "Account 1", currency = currencyUnit.code, openingBalance = 100L)
        val transaction = repository.insertTransaction(accountId = account, amount = 100L)
        closeAccount(account)
        repository.deleteTransaction(transaction.id)
    }

    @Test
    fun deleteAccountWithTransferLinkedToSealedAccount() {
        val currencyUnit = CurrencyUnit.DebugInstance
        val account1 = insertAccount(label= "Account 1", currency = currencyUnit.code, openingBalance = 100L)
        val account2 = insertAccount(label= "Account 2", currency = currencyUnit.code, openingBalance = 100L)
        repository.insertTransfer(account1, account2, 100L)
        closeAccount(account2)
        repository.deleteAccount(account1)
    }


    private fun closeAccount(accountId: Long) {
        repository.setAccountProperty(accountId, KEY_SEALED, true)
    }
}