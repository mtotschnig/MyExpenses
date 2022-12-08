package org.totschnig.myexpenses.adapter

import androidx.paging.PagingSource

abstract class ClearingPagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    abstract fun clear()
}

class ClearingLastPagingSourceFactory<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> ClearingPagingSource<Key, Value>
) : () -> PagingSource<Key, Value> {

    private var lastPagingSource: ClearingPagingSource<Key, Value>? = null

    override fun invoke(): PagingSource<Key, Value> {
        return pagingSourceFactory().also {
            lastPagingSource = it
        }
    }

    fun clear() {
        lastPagingSource?.clear()
    }
}