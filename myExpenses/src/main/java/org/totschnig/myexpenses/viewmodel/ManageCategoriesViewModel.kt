package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.AbstractCategoryList.CAT_TREE_WHERE_CLAUSE
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException
import java.io.OutputStreamWriter

class ManageCategoriesViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    var importCatResult: LiveData<Pair<Int, Int>>? = null
    var exportCatResult: LiveData<Result<Pair<Uri, String>>>? = null
    var deleteResult: LiveData<Result<String>>? = null

    fun importCats() {
        importCatResult = liveData(context = coroutineContext()) {
            emit(
                contentResolver.call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_SETUP_CATEGORIES,
                    null,
                    null
                )?.getSerializable(TransactionProvider.KEY_RESULT) as? Pair<Int, Int> ?: 0 to 0
            )
        }
    }

    suspend fun <T> LiveDataScope<Result<T>>.failure(
        @StringRes resId: Int,
        vararg formatArgs: Any?
    ) =
        emit(Result.failure(Throwable(getString(resId, *formatArgs))))

    fun exportCats(encoding: String) {
        exportCatResult = liveData(context = coroutineContext()) {
            val appDir = AppDirHelper.getAppDir(getApplication())
            if (appDir == null) {
                failure<Pair<Uri, String>>(R.string.external_storage_unavailable)
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
                        failure<Pair<Uri, String>>(R.string.no_categories)
                    } else {
                        val outputFile = AppDirHelper.timeStampedFile(
                            appDir,
                            fileName,
                            ExportFormat.QIF.mimeType, null
                        )
                        if (outputFile == null) {
                            failure<Pair<Uri, String>>(R.string.external_storage_unavailable)
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
                                    Result.success<Pair<Uri, String>>(
                                        outputFile.uri to FileUtils.getPath(
                                            getApplication(),
                                            outputFile.uri
                                        )
                                    )
                                )
                            } catch (e: IOException) {
                                failure<Pair<Uri, String>>(
                                    R.string.export_sdcard_failure,
                                    appDir.name,
                                    e.message
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun deleteCategories(ids: LongArray) {
        deleteResult = liveData(context = coroutineContext()) {
            try {
                contentResolver.query(
                    TransactionProvider.CATEGORIES_URI,
                    arrayOf(
                        KEY_ROWID,
                        "(select 1 FROM $TABLE_TRANSACTIONS WHERE $CAT_TREE_WHERE_CLAUSE) AS $KEY_MAPPED_TRANSACTIONS",
                        "(select 1 FROM $TABLE_TEMPLATES WHERE $CAT_TREE_WHERE_CLAUSE) AS $KEY_MAPPED_TEMPLATES"
                    ),
                    "$KEY_ROWID IN (${ids.joinToString()})",
                    null, null
                ).use { cursor ->
                    if (cursor == null) {
                        failure<String>(R.string.db_error_cursor_null)
                    } else {
                        var deleted = 0
                        var mappedToTransaction = 0
                        var mappedToTemplate = 0
                        if (cursor.moveToFirst()) {
                            while (!cursor.isAfterLast) {
                                var deletable = true
                                if (cursor.getInt(1) > 0) {
                                    deletable = false
                                    mappedToTransaction++
                                }
                                if (cursor.getInt(2) > 0) {
                                    deletable = false
                                    mappedToTemplate++
                                }
                                if (deletable) {
                                    Category.delete(cursor.getLong(0))
                                    deleted++
                                }
                                cursor.moveToNext()
                            }
                            val messages: MutableList<String> =
                                ArrayList()
                            if (deleted > 0) {
                                messages.add(
                                    getQuantityString(
                                        R.plurals.delete_success,
                                        deleted,
                                        deleted
                                    )
                                )
                            }
                            if (mappedToTransaction > 0) {
                                messages.add(
                                    getQuantityString(
                                        R.plurals.not_deletable_mapped_transactions,
                                        mappedToTransaction,
                                        mappedToTransaction
                                    )
                                )
                            }
                            if (mappedToTemplate > 0) {
                                messages.add(
                                    getQuantityString(
                                        R.plurals.not_deletable_mapped_templates,
                                        mappedToTemplate,
                                        mappedToTemplate
                                    )
                                )
                            }
                            emit(Result.success(messages.joinToString(" ")))
                        } else {
                            failure<String>(R.string.db_error_cursor_empty)
                        }
                    }
                }
            } catch (e: SQLiteConstraintException) {
                CrashHandler.reportWithDbSchema(e)
                emit(Result.failure(e))
            }
        }
    }
}