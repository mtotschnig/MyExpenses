package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider


@RunWith(RobolectricTestRunner::class)
class SealedAccountMarkExportedTest: BaseTestWithRepository() {

    @Test
    fun allowExportOnSealedAccount() {
        val currency = CurrencyUnit.DebugInstance
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val sealedAccount = insertAccount(label = "EUR-Account", currency = currency.code)
        val sealed = Transaction.getNewInstance(sealedAccount, currency)
        sealed.amount = Money(currency, 500L)
        sealed.save(contentResolver)
        val openAccount = insertAccount(label = "EUR-Account", currency = currency.code)
        val open = Transaction.getNewInstance(openAccount, currency)
        open.amount = Money(currency, 500L)
        open.save(contentResolver)
        val values = ContentValues(1)
        values.put(DatabaseConstants.KEY_SEALED, true)
        resolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, sealedAccount), values, null, null)
        repository.markAsExported(sealedAccount, null)
        val cursor = resolver.query(TransactionProvider.TRANSACTIONS_URI, arrayOf("count(*)"), "${DatabaseConstants.KEY_STATUS} = ${DatabaseConstants.STATUS_EXPORTED}", null, null)!!
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1)
        cursor.close()
    }
}