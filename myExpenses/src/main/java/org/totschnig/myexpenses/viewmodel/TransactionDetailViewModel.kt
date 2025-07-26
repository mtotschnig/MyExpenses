package org.totschnig.myexpenses.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.loadAccountType
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadAttributes
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_SEARCH
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.useAndMapToOne
import org.totschnig.myexpenses.util.ui.attachmentInfoMap
import org.totschnig.myexpenses.viewmodel.data.Transaction
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.projection
import org.totschnig.myexpenses.viewmodel.data.Transaction.Companion.readTransaction

data class LoadResult(val transaction: Transaction?)

class TransactionDetailViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    private val attachmentInfo by lazy {
        attachmentInfoMap(getApplication<MyApplication>())
    }

    private val projection by lazy {
        projection(localizedContext, prefHandler)
    }

    @SuppressLint("Recycle")
    fun transaction(transactionId: Long): LiveData<LoadResult> =
        liveData(context = coroutineContext()) {
            contentResolver.query(
                TransactionProvider.EXTENDED_URI.buildUpon()
                    .appendQueryParameter(KEY_TRANSACTIONID, transactionId.toString()).build(),
                projection,
                null,
                null,
                null
            )?.useAndMapToOne {
                val accountType = repository.loadAccountType(it.getLong(KEY_ACCOUNT_TYPE))
                it.readTransaction(
                    getApplication(),
                    currencyContext,
                    currencyContext.homeCurrencyUnit,
                    accountType
                )
            }.let { emit(LoadResult(it)) }
        }

    fun sums(archiveId: Long): LiveData<Triple<Long, Long, Long>> = liveData(context = coroutineContext()) {
        contentResolver.query(
            TransactionProvider.ARCHIVE_SUMS_URI(archiveId),
            null, null, null, null
        )?.useAndMapToOne {
            Triple(it.getLong(KEY_SUM_INCOME), it.getLong(KEY_SUM_EXPENSES), it.getLong(KEY_SUM_TRANSFERS))
        }?.let { emit(it) }
    }


    @SuppressLint("Recycle")
    fun parts(transactionId: Long, sortOrder: String?, filter: Criterion? = null): LiveData<List<Transaction>> =
        liveData(context = coroutineContext()) {
            contentResolver.query(
                TransactionProvider.EXTENDED_URI.buildUpon()
                    .appendQueryParameter(KEY_PARENTID, transactionId.toString())
                    .appendQueryParameter(QUERY_PARAMETER_SEARCH, "1")
                    .build()
                ,
                projection,
                filter?.getSelectionForParents(),
                filter?.getSelectionArgs(false)?.takeIf { it.isNotEmpty() },
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