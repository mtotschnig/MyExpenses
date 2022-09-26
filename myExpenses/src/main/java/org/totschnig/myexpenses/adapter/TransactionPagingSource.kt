package org.totschnig.myexpenses.adapter

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber

class TransactionPagingSource(val context: Context, val account: FullAccount) :
    PagingSource<Int, Transaction2>() {

    val contentResolver: ContentResolver
        get() = context.contentResolver
    private val uri: Uri
    private val projection: Array<String>
    private val selection: String
    private val selectionArgs: Array<String>?

    init {
        account.loadingInfo(context).also {
            uri = it.first
            projection = it.second
            selection = it.third
            selectionArgs = it.fourth
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                invalidate()
                contentResolver.unregisterContentObserver(this)
            }
        }
        contentResolver.registerContentObserver(
            TransactionProvider.TRANSACTIONS_URI,
            true,
            observer
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction2>): Int? {
        return null
    }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        val pageNumber = params.key ?: 0
        Timber.i("Requesting pageNumber %d", pageNumber)
        val data = withContext(Dispatchers.IO) {
            contentResolver.query(
                uri.buildUpon()
                    .appendQueryParameter(
                        ContentResolver.QUERY_ARG_LIMIT,
                        params.loadSize.toString()
                    )
                    .appendQueryParameter(
                        ContentResolver.QUERY_ARG_OFFSET,
                        (pageNumber * params.loadSize).toString()
                    )
                    .build(),
                projection,
                "$selection AND ${DatabaseConstants.KEY_PARENTID} is null",
                selectionArgs,
                "${DatabaseConstants.KEY_DATE} ${account.sortDirection}", null
            )?.use { cursor ->
                Timber.i("Cursor size %d", cursor.count)
                cursor.asSequence.map {
                    Transaction2.fromCursor(context, it, (context.applicationContext as MyApplication).appComponent.currencyContext())
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