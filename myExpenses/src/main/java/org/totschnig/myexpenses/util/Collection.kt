package org.totschnig.myexpenses.util

/**
 * true if element was added, false if it was removed
 */
fun <T> MutableCollection<T>.toggle(element: T) = if (contains(element)) {
    remove(element)
    false
} else {
    add(element)
    true
}