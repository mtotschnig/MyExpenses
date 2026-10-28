package org.totschnig.myexpenses.next.core.common

import android.os.Debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/** Tag of the timing logs of the new UI: `adb logcat -s NextPerf` */
const val PERF_TAG = "NextPerf"

/** Milliseconds since [start] from [System.nanoTime], which unlike SystemClock also works in unit tests */
fun millisSince(start: Long) = (System.nanoTime() - start) / 1_000_000

/**
 * CPU time of the current thread in nanoseconds, -1 in unit tests. Unlike the time passed, it does
 * not grow while the thread waits for a core, e.g. while other analyses run at the same time.
 */
fun threadCpuNanos(): Long = try {
    Debug.threadCpuTimeNanos()
} catch (_: RuntimeException) {
    -1
}

/**
 * Runs [block] and logs how long it took, also if it suspends, and the CPU time it used, if it
 * stayed on one thread
 */
inline fun <T> measured(label: String, block: () -> T): T {
    val start = System.nanoTime()
    val thread = Thread.currentThread()
    val cpuStart = threadCpuNanos()
    try {
        return block()
    } finally {
        val cpu = if (Thread.currentThread() === thread && cpuStart >= 0) "${(threadCpuNanos() - cpuStart) / 1_000_000} ms cpu" else "cpu ?"
        Timber.tag(PERF_TAG).d("%s: %d ms, %s [%s]", label, millisSince(start), cpu, thread.name)
    }
}

/**
 * Logs when the flow is started and each value with the time since the start and since the
 * previous value. Shows how long a screen waits for its data, how often it is recomputed and when
 * it is restarted, e.g. after the app was in the background.
 */
fun <T> Flow<T>.logTimings(label: String): Flow<T> = flow {
    val start = System.nanoTime()
    var previous = start
    var count = 0
    Timber.tag(PERF_TAG).d("%s: started", label)
    collect { value ->
        val now = System.nanoTime()
        count++
        Timber.tag(PERF_TAG).d(
            "%s: value #%d after %d ms (+%d ms)%s",
            label, count, (now - start) / 1_000_000, (now - previous) / 1_000_000, if (value == null) " null" else ""
        )
        previous = now
        emit(value)
    }
}
