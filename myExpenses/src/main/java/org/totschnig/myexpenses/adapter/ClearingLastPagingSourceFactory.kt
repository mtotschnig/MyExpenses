package org.totschnig.myexpenses.adapter

import androidx.paging.PagingSource

abstract class ClearingPagingSource<Key : Any, Value : Any, Source: ClearingPagingSource<Key, Value, Source>> : PagingSource<Key, Value>() {
    abstract fun clear()
    abstract fun compareWithLast(lastPagingSource: Source?)
}

class ClearingLastPagingSourceFactory<Key : Any, Value : Any, Source: ClearingPagingSource<Key, Value, Source>> (
    private val pagingSourceFactory: () -> Source
) : () -> Source {

    private var lastPagingSource: Source? = null

    override fun invoke(): Source {
        return pagingSourceFactory().also {
            it.compareWithLast(lastPagingSource)
            lastPagingSource = it
        }
    }

    fun clear() {
        lastPagingSource?.clear()
    }
}