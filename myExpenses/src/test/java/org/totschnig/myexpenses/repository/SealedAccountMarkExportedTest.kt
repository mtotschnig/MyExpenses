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
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider


@RunWith(RobolectricTestRunner::class)
class SealedAccountMarkExportedTest: BaseTestWithRepository() {

    @Test
    fun allowExportOnSealedAccount() {
        val currency = CurrencyUnit.DebugInstance
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val sealedAccount = insertAccount(label = "EUR-Account", currency = currency.code)
        repository.insertTransaction(
            accountId = sealedAccount,
            amount = 500L
        )

        val openAccount = insertAccount(label = "EUR-Account", currency = currency.code)
        repository.insertTransaction(
            accountId = openAccount,
            amount = 500L
        )

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