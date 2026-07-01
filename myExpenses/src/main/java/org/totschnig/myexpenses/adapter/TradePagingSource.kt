package org.totschnig.myexpenses.adapter

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.viewmodel.data.Trade
import org.totschnig.myexpenses.db2.loadTrades
import org.totschnig.myexpenses.provider.withLimit
import timber.log.Timber

class TradePagingSource(
    private val context: Context,
    private val repository: Repository,
    private val account: DataBaseAccount,
    private val pageSize: Int
) : ClearingPagingSource<Int, Trade, TradePagingSource>() {

    private val contentResolver = context.contentResolver
    private val uri: Uri = TRANSACTIONS_URI

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            invalidate()
        }
    }

    init {
        contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun clear() {
        contentResolver.unregisterContentObserver(observer)
    }

    override fun compareWithLast(lastPagingSource: TradePagingSource?) {
        // Simple implementation, could be more complex if needed
    }

    override fun getRefreshKey(state: PagingState<Int, Trade>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(pageSize)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Trade> = try {
        val position = params.key ?: 0
        val offset = position.coerceAtLeast(0)
        val loadSize = params.loadSize

        val (totalCount, trades) = withContext(Dispatchers.IO) {
            // 1. Get total count of parent transactions
            val count = contentResolver.query(
                uri,
                arrayOf("count(*)"),
                "$KEY_PARENTID IS NULL AND account_id = ?",
                arrayOf(account.id.toString()),
                null
            )!!.use {
                it.moveToFirst()
                it.getInt(0)
            }

            // 2. Get IDs for parent transactions for this page
            val ids = contentResolver.query(
                uri.withLimit(loadSize, offset),
                arrayOf(KEY_ROWID),
                "$KEY_PARENTID IS NULL AND account_id = ?",
                arrayOf(account.id.toString()),
                account.sortOrder,
                null
            )!!.use { cursor ->
                val list = mutableListOf<Long>()
                while (cursor.moveToNext()) {
                    list.add(cursor.getLong(0))
                }
                list
            }

            // 3. Bulk load trades
            count to repository.loadTrades(ids)
        }

        val prevKey = if (position > 0) (position - pageSize).coerceAtLeast(0) else null
        val nextKey = if (trades.size < loadSize) null else position + trades.size

        LoadResult.Page(
            data = trades,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = offset,
            itemsAfter = (totalCount - (offset + trades.size)).coerceAtLeast(0)
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}
