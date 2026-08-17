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
import org.totschnig.myexpenses.db2.loadTrades
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.viewmodel.data.Trade

class TradePagingSource(
    context: Context,
    private val repository: Repository,
    private val account: DataBaseAccount,
    private val pageSize: Int,
) : ClearingPagingSource<Int, Trade, TradePagingSource>() {

    private val contentResolver = context.contentResolver
    private val uri: Uri =
        TRANSACTIONS_URI.buildUpon().appendQueryParameter(KEY_ACCOUNTID, account.id.toString())
            .build()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            invalidate()
        }
    }

    init {
        contentResolver.registerContentObserver(uri, true, observer)
        registerInvalidatedCallback {
            clear()
        }
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

        val isAmountSort = account.sortBy == KEY_AMOUNT
        val actualLoadSize = if (isAmountSort) Int.MAX_VALUE else loadSize
        val actualOffset = if (isAmountSort) 0 else offset

        val (totalCount, trades) = withContext(Dispatchers.IO) {
            val count = contentResolver.query(
                uri, arrayOf("count(*)"), WHERE_NOT_SPLIT_PART, null, null
            )!!.use {
                it.moveToFirst()
                it.getInt(0)
            }

            // 2. Get IDs for parent transactions for this page
            val ids = contentResolver.query(
                uri.withLimit(actualLoadSize, actualOffset),
                arrayOf(KEY_ROWID),
                WHERE_NOT_SPLIT_PART, null,
                if (account.sortBy == KEY_DATE) account.sortOrder else null,
                null
            )!!.use { cursor ->
                val list = mutableListOf<Long>()
                while (cursor.moveToNext()) {
                    list.add(cursor.getLong(0))
                }
                list
            }

            // 3. Bulk load trades
            count to repository.loadTrades(ids, account.sortBy to account.sortDirection)
        }

        val prevKey = if (isAmountSort || position == 0) null else (position - pageSize).coerceAtLeast(0)
        val nextKey = if (isAmountSort || trades.size < loadSize) null else position + trades.size

        LoadResult.Page(
            data = trades,
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = if (isAmountSort) 0 else actualOffset,
            itemsAfter = if (isAmountSort) 0 else (totalCount - (actualOffset + trades.size)).coerceAtLeast(0)
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}
