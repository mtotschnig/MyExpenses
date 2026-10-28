package org.totschnig.myexpenses.next.core.common.ledger

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import org.totschnig.myexpenses.next.core.common.PERF_TAG
import org.totschnig.myexpenses.next.core.common.measured
import timber.log.Timber

/**
 * The result of an analysis for the last input. An analysis is a pure function of its input, a
 * data class with everything the result depends on, so an equal input gives the same result and
 * it is not computed again. The view model keeps the memo, so that it outlives its flows, which
 * stop while no screen shows them, e.g. while the app is in the background.
 *
 * @param label of the timing logs, see [measured]
 */
class Memo<I : Any, O>(private val label: String, private val compute: (I) -> O) {

    private class Entry<I, O>(val input: I, val output: O)

    @Volatile
    private var last: Entry<I, O>? = null

    /** Number of computations, for tests */
    @Volatile
    var computations = 0
        private set

    fun of(input: I): O {
        last?.takeIf { it.input == input }?.let {
            Timber.tag(PERF_TAG).d("%s: reused", label)
            return it.output
        }
        computations++
        return measured(label) { compute(input) }.also { last = Entry(input, it) }
    }
}

/**
 * Computes [memo] for each new input, null stays null (loading or no consent). An input equal to
 * the previous one is passed over. The computation does not suspend and cannot be cancelled: a
 * newer input waits until the running one is done, and its result is dropped instead of emitted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <I : Any, O> Flow<I?>.mapMemo(memo: Memo<I, O>): Flow<O?> =
    distinctUntilChanged().mapLatest { input -> input?.let(memo::of) }
