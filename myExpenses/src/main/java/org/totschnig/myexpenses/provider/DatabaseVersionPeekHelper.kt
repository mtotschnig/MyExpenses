package org.totschnig.myexpenses.provider

/**
 * open a database file and return its version
 */
fun interface DatabaseVersionPeekHelper {
    fun peekVersion(path: String): Int
}