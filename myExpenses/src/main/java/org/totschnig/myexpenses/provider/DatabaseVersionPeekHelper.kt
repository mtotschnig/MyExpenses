package org.totschnig.myexpenses.provider

import android.content.Context

/**
 * open a database file and return its version
 */
fun interface DatabaseVersionPeekHelper {
    fun checkVersion(context: Context, path: String): Result<Unit>
}