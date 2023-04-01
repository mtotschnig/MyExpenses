package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.TransactionProvider

@RunWith(RobolectricTestRunner::class)
class AccountTest: BaseTestWithRepository() {

    @Test(expected = SQLiteConstraintException::class)
    fun deleteTransactionInSealedAccount() {
        val currencyUnit = CurrencyUnit.DebugInstance
        val account = repository.createAccount(Account(label= "Account 1", currency = currencyUnit.code, openingBalance = 100L))
        val transaction = Transaction(account.id, Money(currencyUnit, 100L))
        transaction.save()
        closeAccount(account.id)
        Transaction.delete(transaction.id, false)
    }

    @Test
    fun deleteAccountWithTransferLinkedToSealedAccount() {
        val currencyUnit = CurrencyUnit.DebugInstance
        val account1 = repository.createAccount(Account(label= "Account 1", currency = currencyUnit.code, openingBalance = 100L))
        val account2 = repository.createAccount(Account(label= "Account 2", currency = currencyUnit.code, openingBalance = 100L))
        val transfer = Transfer(account1.id, Money(currencyUnit, 100L), account2.id)
        transfer.save()
        closeAccount(account2.id)
        repository.deleteAccount(account1.id)
    }


    private fun closeAccount(accountId: Long) {
        val values = ContentValues().apply {
            put(KEY_SEALED, true)
        }
        ApplicationProvider.getApplicationContext<MyApplication>().contentResolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId), values, null, null)
    }
}