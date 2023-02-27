package org.totschnig.myexpenses.export

import android.content.Context
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
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
                ).build(), arrayOf(DatabaseConstants.KEY_PATH),
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
                                val sb = StringBuilder()
                                sb.append("\nN")
                                    .append(c.getString(0))
                                    .append("\n^")
                                out.write(sb.toString())
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