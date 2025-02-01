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
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.viewmodel.data.mergeTransfers
import timber.log.Timber
import java.time.Duration
import java.time.Instant

open class TransactionPagingSource(
    val context: Context,
    val account: PageAccount,
    val whereFilter: StateFlow<Criterion?>,
    val tags: StateFlow<Map<String, Pair<String, Int?>>>,
    val currencyContext: CurrencyContext,
    coroutineScope: CoroutineScope,
    prefHandler: PrefHandler,
) : ClearingPagingSource<Int, Transaction2, TransactionPagingSource>() {

    val contentResolver: ContentResolver
        get() = context.contentResolver
    private val uri: Uri
    private val projection: Array<String>
    private val observer: ContentObserver
    private var criterion: Criterion? = null
    private var hasNewCriterion: Boolean = false

    init {
        account.loadingInfo(prefHandler).also {
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
        criterion = whereFilter.value
        coroutineScope.launch {
            whereFilter.drop(1).collect {
                invalidate()
            }
        }
        coroutineScope.launch {
            tags.drop(1).collect {
                invalidate()
            }
        }
    }

    override fun clear() {
        contentResolver.unregisterContentObserver(observer)
    }

    override fun compareWithLast(lastPagingSource: TransactionPagingSource?) {
        hasNewCriterion = criterion != lastPagingSource?.criterion
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction2>) =
        if (hasNewCriterion) null
        else state.anchorPosition?.let<Int, Int> { anchorPosition ->
            (anchorPosition - state.config.pageSize / 2).coerceAtLeast(0)
        }.also<Int?> {
            Timber.i("Calculating refreshKey for anchorPosition %d: %d", state.anchorPosition, it)
        }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        try {
            val position = params.key ?: 0
            //if the previous page was loaded from an offset between 0 and loadSize,
            //we must take care to load only the missing items before the offset
            val loadSize = if (position < 0) params.loadSize + position else params.loadSize
            Timber.i("Requesting data for account %d at position %d", account.id, position)
            var selection = "$KEY_PARENTID is null"
            var selectionArgs: Array<String>? = null
            whereFilter.value?.let { filter ->
                val selectionForParents = filter.getSelectionForParents()
                if (selectionForParents.isNotEmpty()) {
                    selection += " AND $selectionForParents"
                    selectionArgs = filter.getSelectionArgs(false).takeIf { it.isNotEmpty() }
                }
            }
            val startTime = if (BuildConfig.DEBUG) Instant.now() else null
            val origList = withContext(Dispatchers.IO) {
                contentResolver.query(
                    uri.withLimit(loadSize, position.coerceAtLeast(0)),
                    projection,
                    selection,
                    selectionArgs,
                    account.sortOrder,
                    null
                )?.use { cursor ->
                    if (BuildConfig.DEBUG) {
                        val endTime = Instant.now()
                        val duration = Duration.between(startTime, endTime)
                        Timber.i("Cursor delivered %d rows after %s", cursor.count, duration)
                    }
                    cursor.asSequence.map {
                        Transaction2.fromCursor(
                            currencyContext,
                            it,
                            tags.value,
                            accountCurrency = account.currencyUnit
                        )
                    }.toList()
                }
            } ?: emptyList()
            val (dropHalfTransfer, mergedList) = if (account.isAggregate) {
                val mergeResult =
                    origList.mergeTransfers(account, currencyContext.homeCurrencyString)
                //if the two halves of a transfer are split between two pages, we
                //drop the half at the end of the list, and reduce the offset for the next load by 1
                val dropHalfTransfer = mergeResult.lastOrNull()?.let {
                    it.transferPeer != null && it.type != FLAG_NEUTRAL
                } == true
                dropHalfTransfer to if (dropHalfTransfer) mergeResult.dropLast(1) else mergeResult
            } else false to origList

            val prevKey = if (position > 0) (position - params.loadSize) else null
            val nextKey = if (origList.size < params.loadSize) null else
                position + params.loadSize - if (dropHalfTransfer) 1 else 0
            Timber.i("Setting prevKey %d, nextKey %d", prevKey, nextKey)
            return LoadResult.Page(
                data = mergedList,
                prevKey = prevKey,
                nextKey = nextKey,
                itemsBefore = position.coerceAtLeast(0)
            )
        } finally {
            onLoadFinished()
        }
    }

    open fun onLoadFinished() {}
}