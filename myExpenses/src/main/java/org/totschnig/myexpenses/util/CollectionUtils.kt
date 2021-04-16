package org.totschnig.myexpenses.util

import android.util.SparseBooleanArray
import androidx.core.util.forEach

/**
 * returns a sequence of the keys which are mapped to value true
 */
fun SparseBooleanArray.asTrueSequence() = sequence {
    forEach { key, value -> if (value) yield(key) }
}

/**
 * replace any occurrence of other with new
 */
fun <T> Collection<T>.replace(other: Iterable<T>, new: T): Set<T> =
        map { if (other.contains(it)) new else it }.toSet()
