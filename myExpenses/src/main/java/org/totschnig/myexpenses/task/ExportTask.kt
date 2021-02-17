package org.totschnig.myexpenses.task

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CsvExporter
import org.totschnig.myexpenses.export.QifExporter
import org.totschnig.myexpenses.fragment.BaseTransactionList.KEY_FILTER
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExportTask(private val taskExecutionFragment: TaskExecutionFragment<*>, extras: Bundle) : AsyncTask<Void, String, Pair<ExportFormat, List<Uri>>?>() {
    private val result = ArrayList<Uri>()
    private var format: ExportFormat = try {
        ExportFormat.valueOf(extras.getString(TaskExecutionFragment.KEY_FORMAT)!!)
    } catch (e: IllegalArgumentException) {
        ExportFormat.QIF
    }
    private val deleteP = extras.getBoolean(KEY_DELETE_P)
    private val notYetExportedP = extras.getBoolean(KEY_NOT_YET_EXPORTED_P)
    private val mergeP = extras.getBoolean(KEY_MERGE_P)
    private val dateFormat = extras.getString(TaskExecutionFragment.KEY_DATE_FORMAT)!!
    private val decimalSeparator: Char = extras.getChar(KEY_DECIMAL_SEPARATOR)
    private val accountId = extras.getLong(DatabaseConstants.KEY_ROWID)
    private val currency = extras.getString(DatabaseConstants.KEY_CURRENCY)
    private val encoding = extras.getString(TaskExecutionFragment.KEY_ENCODING)!!
    private val handleDelete = extras.getInt(KEY_EXPORT_HANDLE_DELETED)
    private val filter = WhereFilter(extras.getParcelableArrayList(KEY_FILTER)!!)
    private val fileName = extras.getString(KEY_FILE_NAME)!!
    private val delimiter = extras.getChar(KEY_DELIMITER)

    init {
        check(!(deleteP && notYetExportedP)) { "Deleting exported transactions is only allowed when all transactions are exported" }
    }

    override fun onProgressUpdate(vararg values: String) {
        if (taskExecutionFragment.mCallbacks != null) {
            for (progress in values) {
                taskExecutionFragment.mCallbacks.onProgressUpdate(progress)
            }
        }
    }

    override fun onPostExecute(result: Pair<ExportFormat, List<Uri>>?) {
        if (taskExecutionFragment.mCallbacks != null) {
            taskExecutionFragment.mCallbacks.onPostExecute(
                    TaskExecutionFragment.TASK_EXPORT, result)
        }
    }

    private fun createFileFailure(context: Context, parent: DocumentFile, fileName: String) =
            IOException(context.getString(R.string.io_error_unable_to_create_file,
                    fileName, FileUtils.getPath(context, parent.uri)))

    override fun doInBackground(vararg ignored: Void): Pair<ExportFormat, List<Uri>>? {
        val accountIds: Array<Long>
        val application = MyApplication.getInstance()
        if (accountId > 0L) {
            accountIds = arrayOf(accountId)
        } else {
            var selection: String? = null
            var selectionArgs: Array<String>? = null
            if (currency != null) {
                selection = DatabaseConstants.KEY_CURRENCY + " = ?"
                selectionArgs = arrayOf(currency)
            }
            val c = application.contentResolver.query(TransactionProvider.ACCOUNTS_URI, arrayOf(DatabaseConstants.KEY_ROWID), selection, selectionArgs, null)
            accountIds = DbUtils.getLongArrayFromCursor(c, DatabaseConstants.KEY_ROWID)
            c!!.close()
        }
        var account: Account?
        val destDir: DocumentFile
        val appDir = AppDirHelper.getAppDir(application)
        val context = ContextHelper.wrap(application, application.appComponent.userLocaleProvider().getUserPreferredLocale())
        if (appDir == null) {
            publishProgress(context.getString(R.string.external_storage_unavailable))
            return null
        }
        val oneFile = accountIds.size == 1 || mergeP
        destDir = if (oneFile) {
            appDir
        } else {
            AppDirHelper.newDirectory(appDir, fileName) ?: throw createFileFailure(context, appDir, fileName)
        }
        val successfullyExported = ArrayList<Account>()
        val simpleDateFormat = SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        val now = Date()
        for (i in accountIds.indices) {
            account = Account.getInstanceFromDb(accountIds[i])
            if (account == null) continue
            publishProgress(account.label + " ...")
            try {
                val append = mergeP && i > 0
                val fileNameForAccount = if (oneFile) fileName else String.format("%s-%s", Utils.escapeForFileName(account.label),
                        simpleDateFormat.format(now))
                val exporter = if (format === ExportFormat.CSV)
                    CsvExporter(account, filter, notYetExportedP, dateFormat, decimalSeparator, encoding, !append, delimiter, mergeP) else
                    QifExporter(account, filter, notYetExportedP, dateFormat, decimalSeparator, encoding)
                val result = exporter.export(context, lazy {
                    Result.success(AppDirHelper.buildFile(destDir, fileNameForAccount, format.mimeType,
                            append, true) ?: throw createFileFailure(context, destDir, fileName))
                }, append)
                result.onSuccess {
                    if (PrefKey.PERFORM_SHARE.getBoolean(false)) {
                        addResult(it)
                    }
                    successfullyExported.add(account)
                    publishProgress("..." + context.getString(R.string.export_sdcard_success, FileUtils.getPath(context, it)))
                }.onFailure {
                    publishProgress("... " + it.message)
                }
            } catch (e: IOException) {
                publishProgress("... " + context.getString(
                        R.string.export_sdcard_failure,
                        appDir.name,
                        e.message))
            }
        }
        for (a in successfullyExported) {
            try {
                if (deleteP) {
                    check(!a.isSealed) { "Trying to reset account that is sealed" }
                    a.reset(filter, handleDelete, fileName)
                } else {
                    a.markAsExported(filter)
                }
            } catch (e: Exception) {
                publishProgress("ERROR: " + e.message)
                CrashHandler.report(e)
            }
        }
        return Pair.create(format, getResult())
    }

    private fun getResult(): List<Uri> {
        return result
    }

    private fun addResult(fileUri: Uri) {
        result.add(fileUri)
    }

    companion object {
        const val KEY_DECIMAL_SEPARATOR = "export_decimal_separator"
        const val KEY_NOT_YET_EXPORTED_P = "notYetExportedP"
        const val KEY_DELETE_P = "deleteP"
        const val KEY_EXPORT_HANDLE_DELETED = "export_handle_deleted"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_DELIMITER = "export_delimiter"
        const val KEY_MERGE_P = "export_merge_accounts"
    }
}