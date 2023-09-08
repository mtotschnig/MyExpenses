package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import java.util.*


@RunWith(RobolectricTestRunner::class)
class SealedAccountMarkExportedTest: BaseTestWithRepository() {

    @Test
    fun allowExportOnSealedAccount() {
        val currency = CurrencyUnit.DebugInstance
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val sealedAccount = Account(label = "EUR-Account", currency = currency.code).createIn(repository)
        val sealed = Transaction.getNewInstance(sealedAccount.id, currency)
        sealed.amount = Money(currency, 500L)
        sealed.save(contentResolver)
        val openAccount = Account(label = "EUR-Account", currency = currency.code).createIn(repository)
        val open = Transaction.getNewInstance(openAccount.id, currency)
        open.amount = Money(currency, 500L)
        open.save(contentResolver)
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