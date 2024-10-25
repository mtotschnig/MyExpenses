package org.totschnig.myexpenses.util

/**
 * replace any occurrence of other with new
 */
fun <T> Collection<T>.replace(other: Iterable<T>, new: T): Set<T> =
        map { if (other.contains(it)) new else it }.toSet()
