package org.totschnig.myexpenses.adapter

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Transaction.EXTENDED_URI
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.viewmodel.data.Transaction
import timber.log.Timber

class TransactionPagingSource(val context: MyApplication, val accountId: Long) :
    PagingSource<Int, Transaction>() {

    val contentResolver: ContentResolver
        get() = context.contentResolver

    init {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                invalidate()
                contentResolver.unregisterContentObserver(this)
            }
        }

        contentResolver.registerContentObserver(TransactionProvider.TRANSACTIONS_URI, false, observer)
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        return null
    }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        val pageNumber = params.key ?: 0
        Timber.i("Requesting pageNumber %d", pageNumber)
        val data = withContext(Dispatchers.IO) {
            contentResolver.query(
                EXTENDED_URI.buildUpon()
                    .appendQueryParameter(ContentResolver.QUERY_ARG_LIMIT, params.loadSize.toString())
                    .appendQueryParameter(ContentResolver.QUERY_ARG_OFFSET, (pageNumber * params.loadSize).toString())
                    .build(),
                Transaction.projection(context),
                "${DatabaseConstants.KEY_ACCOUNTID} = ?",
                arrayOf(accountId.toString()),
                DatabaseConstants.KEY_ROWID, null
            )?.use { cursor ->
                Timber.i("Cursor size %d", cursor.count)
                cursor.asSequence.map {
                    Transaction.fromCursor(context, it, context.appComponent.currencyContext())
                }.toList()
            } ?: emptyList()
        }
        val prevKey = if (pageNumber > 0) pageNumber - 1 else null
        val nextKey = if (data.isEmpty()) null else pageNumber + 1
        Timber.i("Setting prevKey %d, nextKey %d", prevKey, nextKey)
        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey
        )
    }
}