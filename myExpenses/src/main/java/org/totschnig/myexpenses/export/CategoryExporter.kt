package org.totschnig.myexpenses.export

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException
import java.io.OutputStreamWriter

object CategoryExporter {
    @Throws(IOException::class)
    fun export(
        context: Context,
        encoding: String
    ): Result<Pair<Uri, String>>? {
        val appDir = AppDirHelper.getAppDir(context)
        fun <T> failure(
            @StringRes resId: Int,
            vararg formatArgs: Any?
        ) = Result.failure<T>(context, resId, formatArgs)

        return if (appDir == null) {
            failure(R.string.external_storage_unavailable)
        } else {
            val fileName = "categories"
            context.contentResolver.query(
                TransactionProvider.CATEGORIES_URI.buildUpon()
                    .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_CATEGORY_SEPARATOR, ":").build()
                , arrayOf(DatabaseConstants.KEY_PATH),
                null, null, null
            )?.use { c ->
                if (c.count == 0) {
                    failure(R.string.no_categories)
                } else {
                    val outputFile = AppDirHelper.timeStampedFile(
                        appDir,
                        fileName,
                        ExportFormat.QIF.mimeType, "qif"
                    )
                    if (outputFile == null) {
                        failure(R.string.external_storage_unavailable)
                    } else {
                        try {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            OutputStreamWriter(
                                context.contentResolver.openOutputStream(outputFile.uri),
                                encoding
                            ).use { out ->
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
                            Result.success<Pair<Uri, String>>(
                                outputFile.uri to FileUtils.getPath(
                                    context,
                                    outputFile.uri
                                )
                            )
                        } catch (e: IOException) {
                            failure(
                                R.string.export_sdcard_failure,
                                appDir.name,
                                e.message
                            )
                        }
                    }
                }
            } ?: failure(R.string.db_error_cursor_null)
        }
    }
}