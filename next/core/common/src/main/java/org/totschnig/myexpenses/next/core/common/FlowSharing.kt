package org.totschnig.myexpenses.next.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/**
 * Keeps the flow running while it is collected and five seconds longer, so that it survives a
 * configuration change. Used for everything the view models of the new UI offer.
 */
private val WhileShown = SharingStarted.WhileSubscribed(5000)

/** A state for screens, see [WhileShown] */
fun <T> Flow<T>.stateWhileShown(scope: CoroutineScope, initial: T): StateFlow<T> = stateIn(scope, WhileShown, initial)

/** Computed once for all collectors, e.g. facts other features read, see [WhileShown] */
fun <T> Flow<T>.shareWhileShown(scope: CoroutineScope): SharedFlow<T> = shareIn(scope, WhileShown, replay = 1)
