package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException
import java.io.OutputStreamWriter

class ManageCategoriesViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    fun importCats() = liveData(context = coroutineContext()) {
        emit(
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_SETUP_CATEGORIES,
                null,
                null
            )?.getInt(TransactionProvider.KEY_RESULT) ?: 0
        )
    }

    suspend fun LiveDataScope<Result<Pair<Uri, String>>>.failure(@StringRes resId: Int, vararg formatArgs: Any?) =
        emit(Result.failure(Throwable(getString(resId, *formatArgs))))

    fun exportCats(encoding: String): LiveData<Result<Pair<Uri, String>>> =
        liveData(context = coroutineContext()) {
            val appDir = AppDirHelper.getAppDir(getApplication())
            if (appDir == null) {
                failure(R.string.external_storage_unavailable)
            } else {
                val mainLabel =
                    "CASE WHEN $KEY_PARENTID THEN (SELECT $KEY_LABEL FROM $TABLE_CATEGORIES parent WHERE parent.$KEY_ROWID = $TABLE_CATEGORIES.$KEY_PARENTID) ELSE $KEY_LABEL END"
                val subLabel = "CASE WHEN $KEY_PARENTID THEN $KEY_LABEL END"

                //sort sub categories immediately after their main category
                val sort = "CASE WHEN parent_id then parent_id else _id END"
                val fileName = "categories"
                contentResolver.query(
                    Category.CONTENT_URI, arrayOf(mainLabel, subLabel),
                    null, null, sort
                )?.use { c ->
                    if (c.count == 0) {
                        failure(R.string.no_categories)
                    } else {
                        val outputFile = AppDirHelper.timeStampedFile(
                            appDir,
                            fileName,
                            ExportFormat.QIF.mimeType, null
                        )
                        if (outputFile == null) {
                            failure(R.string.external_storage_unavailable)
                        } else {
                            try {
                                @Suppress("BlockingMethodInNonBlockingContext")
                                OutputStreamWriter(
                                    contentResolver.openOutputStream(outputFile.uri),
                                    encoding
                                ).use { out ->
                                    out.write("!Type:Cat")
                                    c.moveToFirst()
                                    while (c.position < c.count) {
                                        val sb = StringBuilder()
                                        sb.append("\nN")
                                            .append(
                                                TextUtils.formatQifCategory(
                                                    c.getString(0),
                                                    c.getString(1)
                                                )
                                            )
                                            .append("\n^")
                                        out.write(sb.toString())
                                        c.moveToNext()
                                    }
                                }
                                emit(
                                    Result.success(
                                        outputFile.uri to FileUtils.getPath(
                                            getApplication(),
                                            outputFile.uri
                                        )
                                    )
                                )
                            } catch (e: IOException) {
                                failure(R.string.export_sdcard_failure, appDir.name, e.message)
                            }
                        }
                    }
                }
            }
        }
}