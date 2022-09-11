package org.totschnig.myexpenses.adapter

import android.annotation.SuppressLint
import android.content.ContentResolver
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getLong
import timber.log.Timber

class TransactionPagingSource(val contentResolver: ContentResolver, val accountId: Long) :
    PagingSource<Int, Transaction>() {
    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        return null
    }

    @SuppressLint("InlinedApi")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        val pageNumber = params.key ?: 0
        val data = withContext(Dispatchers.IO) {
            contentResolver.query(
                TransactionProvider.TRANSACTIONS_URI.buildUpon()
                    .appendQueryParameter(ContentResolver.QUERY_ARG_LIMIT, params.loadSize.toString())
                    .appendQueryParameter(ContentResolver.QUERY_ARG_OFFSET, (pageNumber * params.loadSize).toString())
                    .build(),
                emptyArray(),
                "${DatabaseConstants.KEY_ACCOUNTID} = ?",
                arrayOf(accountId.toString()),
                null, null
            )?.use {
                Timber.i("Cursor size %d", it.count)
                it.asSequence.map {
                    Transaction(
                        it.getLong(DatabaseConstants.KEY_ROWID),
                        it.getLong(DatabaseConstants.KEY_AMOUNT)
                    )
                }.toList()
            } ?: emptyList()
        }
        return LoadResult.Page(
            data = data,
            prevKey = if (pageNumber > 0) pageNumber - 1 else null,
            nextKey = if (data.isEmpty()) null else pageNumber + 1
        )
    }
}