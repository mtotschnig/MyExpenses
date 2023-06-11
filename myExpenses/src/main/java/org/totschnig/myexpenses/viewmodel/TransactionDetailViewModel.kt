package org.totschnig.myexpenses.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.fromCursor
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.projection
import org.totschnig.myexpenses.viewmodel.data.Transaction as TData

class TransactionDetailViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    @SuppressLint("Recycle")
    fun transaction(transactionId: Long): LiveData<List<TData>> = liveData(context = coroutineContext()) {
        contentResolver.query(Transaction.EXTENDED_URI,
            projection(localizedContext, homeCurrencyProvider.homeCurrencyString),
            "$KEY_ROWID = ? OR $KEY_PARENTID = ?",
            Array(2) { transactionId.toString() },
            "$KEY_PARENTID IS NULL DESC")?.useAndMap {
                fromCursor(getApplication(), it, currencyContext, homeCurrencyProvider.homeCurrencyUnit)
        }?.let { emit(it) }
    }

    @SuppressLint("Recycle")
    fun tags(id: Long) : LiveData<List<Tag>> = liveData(context = coroutineContext()) {
        contentResolver.query(TransactionProvider.TRANSACTIONS_TAGS_URI, null, "$KEY_TRANSACTIONID = ?", arrayOf(id.toString()), null)?.useAndMap {
            Tag(it.getLong(it.getColumnIndexOrThrow(KEY_ROWID)), it.getString(it.getColumnIndexOrThrow(
                KEY_LABEL
            )))
        }?.let { emit(it) }
    }
}