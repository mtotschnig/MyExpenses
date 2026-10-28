package org.totschnig.myexpenses.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells that transactions were imported from the bank or a file, e.g. so that the new UI applies
 * its rules to them right away and not only when it is shown next.
 */
object TransactionImports {
    private val _imported = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val imported: SharedFlow<Unit> = _imported.asSharedFlow()

    fun notifyImported() {
        _imported.tryEmit(Unit)
    }
}
