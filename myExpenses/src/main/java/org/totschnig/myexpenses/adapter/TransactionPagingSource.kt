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
import org.totschnig.myexpenses.provider.KEY_PARENTID
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
        else state.anchorPosition?.let { anchorPosition ->
            (anchorPosition - state.config.pageSize / 2).coerceAtLeast(0)
        }.also {
            Timber.i("Calculating refreshKey for anchorPosition %d: %d", state.anchorPosition, it)
        }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        try {
            val position = params.key ?: 0
            //if the previous page was loaded from an offset between 0 and loadSize,
            //we must take care to load only the missing items before the offset
            val loadSize = if (position < 0) params.loadSize + position else params.loadSize
            val fetchSize = loadSize + 1 // We'll fetch one more item as a lookahead.
            Timber.i("Requesting data for account %d at position %d", account.id, position)
            var selection = "$KEY_PARENTID IS NULL"
            var selectionArgs: Array<String>? = null
            whereFilter.value?.let { filter ->
                val selectionForParents = filter.getSelectionForParents()
                if (selectionForParents.isNotEmpty()) {
                    selection += " AND $selectionForParents"
                    selectionArgs = filter.getSelectionArgs(false).takeIf { it.isNotEmpty() }
                }
            }
            val startTime = if (BuildConfig.DEBUG) Instant.now() else null
            val fullList = withContext(Dispatchers.IO) {
                contentResolver.query(
                    uri.withLimit(fetchSize, position.coerceAtLeast(0)),
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

            val itemsForThisPage = fullList.take(loadSize) // The items that actually belong to this page.
            val lookaheadItem = fullList.getOrNull(loadSize) // The (N+1)th item.

            var dropHalfTransfer = false
            var mergedList = if (account.isAggregate) {
                itemsForThisPage.mergeTransfers(account, currencyContext.homeCurrencyString)
            } else itemsForThisPage

            // Handle the boundary condition.
            // Check if the VERY LAST item in merged list is a half-transfer.
            val lastItemTransferPeer = mergedList.lastOrNull()?.transferPeer
            val lookAheadItemId = lookaheadItem?.id
            if (lastItemTransferPeer != null && lookAheadItemId != null && lastItemTransferPeer == lookAheadItemId ) {
                // The other half is on the next page. For consistency,
                // we drop the half-transfer from this page. It will be
                // correctly merged at the top of the next page.
                mergedList = mergedList.dropLast(1)
                dropHalfTransfer = true
            }


            val prevKey = if (position > 0) (position - params.loadSize) else null
            val nextKey = if (fullList.size < fetchSize) null else
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