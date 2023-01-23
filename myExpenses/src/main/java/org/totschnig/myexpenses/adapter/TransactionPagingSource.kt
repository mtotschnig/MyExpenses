package org.totschnig.myexpenses.adapter

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.time.Duration
import java.time.Instant

open class TransactionPagingSource(
    val context: Context,
    val account: PageAccount,
    val whereFilter: StateFlow<WhereFilter>,
    coroutineScope: CoroutineScope
) :
    ClearingPagingSource<Int, Transaction2>() {

    val contentResolver: ContentResolver
        get() = context.contentResolver
    private val uri: Uri
    private val projection: Array<String>
    private var selection: String
    private var selectionArgs: Array<String>?
    private val observer: ContentObserver

    init {
        account.loadingInfo().also {
            uri = it.first
            projection = it.second
            selection = it.third
            selectionArgs = it.fourth
        }
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Timber.i("Data changed for account %d, now invalidating", account.id)
                invalidate()
                contentResolver.unregisterContentObserver(this)
            }
        }
        contentResolver.registerContentObserver(
            TransactionProvider.TRANSACTIONS_URI,
            true,
            observer
        )
        coroutineScope.launch {
            whereFilter.drop(1).collect {
                invalidate()
            }
        }
    }

    override fun clear() {
        contentResolver.unregisterContentObserver(observer)
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction2>): Int? {
        val result = state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                Timber.i("Calculating refreshKey for anchorPosition %d: page %s", anchorPosition, page)
                page.itemsBefore
            }
        }
        Timber.i("Calculating refreshKey for anchorPosition %d: %d", state.anchorPosition, result)
        return result

    }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        val position = params.key ?: 0
        Timber.i("Requesting data for account %d at position %d", account.id, position)
        if (!whereFilter.value.isEmpty) {
            val selectionForParents =
                whereFilter.value.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED)
            if (selectionForParents.isNotEmpty()) {
                selection += " AND "
                selection += selectionForParents
                selectionArgs = buildList {
                    selectionArgs?.let { addAll(it) }
                    whereFilter.value.getSelectionArgsIfNotEmpty(false)?.let { addAll(it) }
                }.toTypedArray()
            }
        }
        val startTime = if (BuildConfig.DEBUG) Instant.now() else null
        val data = withContext(Dispatchers.IO) {
            contentResolver.query(
                uri.withLimit(params.loadSize, position),
                projection,
                "$selection AND ${DatabaseConstants.KEY_PARENTID} is null",
                selectionArgs,
                "${DatabaseConstants.KEY_DATE} ${account.sortDirection}", null
            )?.use { cursor ->
                if (BuildConfig.DEBUG) {
                    val endTime = Instant.now()
                    val duration = Duration.between(startTime, endTime)
                    Timber.i("Cursor delivered %d rows after %s", cursor.count, duration)
                }
                withContext(Dispatchers.Main) {
                    cursor.asSequence.map {
                        Transaction2.fromCursor(
                            context,
                            it,
                            (context.applicationContext as MyApplication).appComponent.currencyContext(),
                            if (account.isHomeAggregate) Utils.getHomeCurrency() else null
                        )
                    }.toList()
                }
            } ?: emptyList()
        }
        onLoadFinished()
        val prevKey = if (position > 0) (position - params.loadSize).coerceAtLeast(0) else null
        val nextKey = if (data.size < params.loadSize) null else position + params.loadSize
        Timber.i("Setting prevKey %d, nextKey %d", prevKey, nextKey)
        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = position
        )
    }

    open fun onLoadFinished() {}
}