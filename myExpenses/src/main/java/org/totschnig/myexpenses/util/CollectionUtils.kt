package org.totschnig.myexpenses.util

/**
 * replace any occurrence of other with new
 */
fun <T> Collection<T>.replace(other: Iterable<T>, new: T): Set<T> =
        map { if (other.contains(it)) new else it }.toSet()

fun <T> MutableList<T>.removeNthOccurrence(element: T, n: Int): Boolean {
        if (n < 0) return false
        var count = 0
        for (i in indices) {
                if (this[i] == element) {
                        if (count == n) {
                                removeAt(i)
                                return true
                        }
                        count++
                }
        }
        return false
}

fun <T> List<T>.indexOfFrom(element: T, startIndex: Int): Int {
        if (startIndex >= this.size) return -1
        val subIndex = this.subList(startIndex, this.size).indexOf(element)
        return if (subIndex == -1) -1 else startIndex + subIndex
}

fun <T> List<T>.lastIndexOfFrom(element: T, startIndex: Int): Int {
        if (startIndex >= this.size) return -1
        for (i in startIndex downTo 0) {
                if (this[i] == element) return i
        }
        return -1
}