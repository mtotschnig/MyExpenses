package org.totschnig.myexpenses.model

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils

@RunWith(RobolectricTestRunner::class)
class AccountTest {

    @Test(expected = SQLiteConstraintException::class)
    fun deleteTransactionInSealedAccount() {
        val currencyUnit = Utils.getHomeCurrency()
        val account = Account("Account 1", currencyUnit, 100L, AccountType.CASH)
        account.save()
        val transaction = Transaction(account.id, Money(currencyUnit, 100L))
        transaction.save()
        closeAccount(account.id)
        Transaction.delete(transaction.id, false)
    }

    @Test
    fun deleteAccountWithTransferLinkedToSealedAccount() {
        val currencyUnit = Utils.getHomeCurrency()
        val account1 = Account("Account 1", currencyUnit, 100L, AccountType.CASH)
        account1.save()
        val account2 = Account("Account 1", currencyUnit, 100L, AccountType.CASH)
        account2.save()
        val transfer = Transfer(account1.id, Money(currencyUnit, 100L), account2.id)
        transfer.save()
        closeAccount(account2.id)
        Account.delete(account1.id)
    }


    private fun closeAccount(accountId: Long) {
        val values = ContentValues().apply {
            put(KEY_SEALED, true)
        }
        ApplicationProvider.getApplicationContext<MyApplication>().contentResolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId), values, null, null)
    }
}