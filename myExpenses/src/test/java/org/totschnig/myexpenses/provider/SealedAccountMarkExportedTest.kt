package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import java.util.*


@RunWith(RobolectricTestRunner::class)
class SealedAccountMarkExportedTest {

    @Test
    fun allowExportOnSealedAccount() {
        val currency = CurrencyUnit(Currency.getInstance("EUR"))
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val sealedAccount = Account("EUR-Account", CurrencyUnit(Currency.getInstance("EUR")), 0L, null, AccountType.CASH, Account.DEFAULT_COLOR)
        sealedAccount.save()
        val sealed = Transaction.getNewInstance(sealedAccount.id)
        sealed.amount = Money(currency, 500L)
        sealed.save()
        val openAccount = Account("EUR-Account", CurrencyUnit(Currency.getInstance("EUR")), 0L, null, AccountType.CASH, Account.DEFAULT_COLOR)
        openAccount.save()
        val open = Transaction.getNewInstance(openAccount.id)
        open.amount = Money(currency, 500L)
        open.save()
        val values = ContentValues(1)
        values.put(DatabaseConstants.KEY_SEALED, true)
        resolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, sealedAccount.id), values, null, null)
        sealedAccount.markAsExported(null)
        val cursor = resolver.query(TransactionProvider.TRANSACTIONS_URI, arrayOf("count(*)"), "${DatabaseConstants.KEY_STATUS} = ${DatabaseConstants.STATUS_EXPORTED}", null, null)!!
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1)
        cursor.close()
    }
}