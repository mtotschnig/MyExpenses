package org.totschnig.myexpenses.provider

/**
 * open a database file and return its version
 */
interface DatabaseVersionPeekHelper {
    fun peekVersion(path: String): Int
}