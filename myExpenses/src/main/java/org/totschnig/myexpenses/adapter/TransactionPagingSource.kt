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
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.locale.HomeCurrencyProvider
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.time.Duration
import java.time.Instant

open class TransactionPagingSource(
    val context: Context,
    val account: PageAccount,
    val whereFilter: StateFlow<WhereFilter>,
    val homeCurrencyProvider: HomeCurrencyProvider,
    val currencyContext: CurrencyContext,
    coroutineScope: CoroutineScope,
    prefHandler: PrefHandler
) :
    ClearingPagingSource<Int, Transaction2>() {

    val contentResolver: ContentResolver
        get() = context.contentResolver
    private val uri: Uri
    private val projection: Array<String>
    private val observer: ContentObserver

    init {
        account.loadingInfo(homeCurrencyProvider.homeCurrencyString, prefHandler).also {
            uri = it.first
            projection = it.second
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
            (anchorPosition - state.config.pageSize / 2).coerceAtLeast(0)
        }
        Timber.i("Calculating refreshKey for anchorPosition %d: %d", state.anchorPosition, result)
        return result

    }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        val position = params.key ?: 0
        //if the previous page was loaded from an offset between 0 and loadsize,
        //we must take care to load only the missing items before the offset
        val loadSize = if (position < 0) params.loadSize + position else params.loadSize
        Timber.i("Requesting data for account %d at position %d", account.id, position)
        var selection = "$KEY_PARENTID is null"
        var selectionArgs: Array<String>? = null
        if (!whereFilter.value.isEmpty) {
            val selectionForParents =
                whereFilter.value.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED)
            if (selectionForParents.isNotEmpty()) {
                selection += " AND $selectionForParents"
                selectionArgs = whereFilter.value.getSelectionArgsIfNotEmpty(false)
            }
        }
        val startTime = if (BuildConfig.DEBUG) Instant.now() else null
        val sortBy = when(account.sortBy) {
            KEY_AMOUNT -> "abs($KEY_AMOUNT)"
            else -> account.sortBy
        }
        val data = withContext(Dispatchers.IO) {
            contentResolver.query(
                uri.withLimit(loadSize, position.coerceAtLeast(0)),
                projection,
                selection,
                selectionArgs,
                "$sortBy ${account.sortDirection}", null
            )?.use { cursor ->
                if (BuildConfig.DEBUG) {
                    val endTime = Instant.now()
                    val duration = Duration.between(startTime, endTime)
                    Timber.i("Cursor delivered %d rows after %s", cursor.count, duration)
                }
                withContext(Dispatchers.Main) {
                    cursor.asSequence.map {
                        Transaction2.fromCursor(
                            it,
                            account.currencyUnit
                        )
                    }.toList()
                }
            } ?: emptyList()
        }
        onLoadFinished()
        val prevKey = if (position > 0) (position - params.loadSize) else null
        val nextKey = if (data.size < params.loadSize) null else position + params.loadSize
        Timber.i("Setting prevKey %d, nextKey %d", prevKey, nextKey)
        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = position.coerceAtLeast(0)
        )
    }

    open fun onLoadFinished() {}
}