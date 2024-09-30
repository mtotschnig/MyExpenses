package org.totschnig.myexpenses.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadAttributes
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.useAndMapToOne
import org.totschnig.myexpenses.util.ui.attachmentInfoMap
import org.totschnig.myexpenses.viewmodel.data.Transaction
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.projection
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.readTransaction

class TransactionDetailViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    private val attachmentInfo by lazy {
        attachmentInfoMap(getApplication<MyApplication>())
    }

    @SuppressLint("Recycle")
    fun transaction(transactionId: Long): LiveData<Transaction> =
        liveData(context = coroutineContext()) {
            contentResolver.query(
                TransactionProvider.EXTENDED_URI.buildUpon()
                    .appendQueryParameter(KEY_TRANSACTIONID, transactionId.toString()).build(),
                projection(localizedContext),
                null,
                null,
                null
            )?.useAndMapToOne {
                it.readTransaction(
                    getApplication(),
                    currencyContext,
                    currencyContext.homeCurrencyUnit
                )
            }?.let { emit(it) }
        }

    @SuppressLint("Recycle")
    fun parts(transactionId: Long, sortOrder: String?, filter: WhereFilter? = null): LiveData<List<Transaction>> =
        liveData(context = coroutineContext()) {
            contentResolver.query(
                TransactionProvider.EXTENDED_URI.buildUpon()
                    .appendQueryParameter(KEY_PARENTID, transactionId.toString()).build(),
                projection(localizedContext),
                filter?.getSelectionForParents(),
                filter?.getSelectionArgsIfNotEmpty(false),
                sortOrder
            )?.useAndMapToList {
                it.readTransaction(
                    getApplication(),
                    currencyContext,
                    currencyContext.homeCurrencyUnit
                )
            }?.let { emit(it) }
        }

    fun attributes(id: Long) = liveData(context = coroutineContext()) {
        emit(repository.loadAttributes(id).groupBy { it.first.context })
    }

    fun attachments(id: Long) = liveData(context = coroutineContext()) {
        emit(repository.loadAttachments(id).map { it to attachmentInfo.getValue(it) })
    }
}