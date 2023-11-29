package org.totschnig.myexpenses.export

import android.content.Context
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.failure
import java.io.IOException
import java.io.OutputStreamWriter

object CategoryExporter {
    @Throws(IOException::class)
    fun export(
        context: Context,
        encoding: String,
        outputStream: Lazy<Result<DocumentFile>>
    ): Result<DocumentFile> {
        fun <T> failure(
            @StringRes resId: Int,
            vararg formatArgs: Any?
        ) = Result.failure<T>(context, resId, formatArgs)

        return context.contentResolver.query(
            BaseTransactionProvider.CATEGORY_TREE_URI.buildUpon()
                .appendQueryParameter(
                    TransactionProvider.QUERY_PARAMETER_CATEGORY_SEPARATOR,
                    ":"
                ).build(), arrayOf(KEY_PATH, KEY_TYPE, KEY_LEVEL),
            null, null, null
        )?.use { c ->
            if (c.count == 0) {
                failure(R.string.no_categories)
            } else {
                outputStream.value.mapCatching { documentFile ->
                    context.contentResolver.openOutputStream(documentFile.uri, "w").use {
                        OutputStreamWriter(it, encoding).use { out ->
                            out.write("!Type:Cat")
                            c.moveToFirst()
                            while (c.position < c.count) {
                                val sb = buildString {
                                    append("\nN")
                                    append(c.getString(0))
                                    val level = c.getInt(2)
                                    //we only manage type at root level, so we only export it there
                                    if (level == 1) {
                                        val type = c.getInt(1).toByte()
                                        if (type == FLAG_EXPENSE) {
                                            append(" \nE")
                                        } else if (type == FLAG_INCOME) {
                                            append(" \nI")
                                        }
                                    }
                                    append("\n^")
                                }
                                out.write(sb)
                                c.moveToNext()
                            }
                        }
                    }
                    documentFile
                }
            }
        } ?: failure(R.string.db_error_cursor_null)
    }
}