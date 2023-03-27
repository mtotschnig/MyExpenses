package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.CurrencyFormatter
import java.util.*


@RunWith(RobolectricTestRunner::class)
class SealedAccountMarkExportedTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val currencyContext
        get() =  Mockito.mock(CurrencyContext::class.java)

    private val repository: Repository
        get() = Repository(
            context,
            currencyContext,
            Mockito.mock(CurrencyFormatter::class.java),
            Mockito.mock(PrefHandler::class.java)
        )

    @Test
    fun allowExportOnSealedAccount() {
        val currency = CurrencyUnit.DebugInstance
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val sealedAccount = Account("EUR-Account", currency, 0L, null, AccountType.CASH, Account.DEFAULT_COLOR)
        sealedAccount.save(currency)
        val sealed = Transaction.getNewInstance(sealedAccount)
        sealed.amount = Money(currency, 500L)
        sealed.save()
        val openAccount = Account("EUR-Account", currency, 0L, null, AccountType.CASH, Account.DEFAULT_COLOR)
        openAccount.save(currency)
        val open = Transaction.getNewInstance(openAccount)
        open.amount = Money(currency, 500L)
        open.save()
        val values = ContentValues(1)
        values.put(DatabaseConstants.KEY_SEALED, true)
        resolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, sealedAccount.id), values, null, null)
        repository.markAsExported(sealedAccount.id, null)
        val cursor = resolver.query(TransactionProvider.TRANSACTIONS_URI, arrayOf("count(*)"), "${DatabaseConstants.KEY_STATUS} = ${DatabaseConstants.STATUS_EXPORTED}", null, null)!!
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1)
        cursor.close()
    }
}