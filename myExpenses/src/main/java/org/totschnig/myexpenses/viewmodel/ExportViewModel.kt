package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CsvExporter
import org.totschnig.myexpenses.export.JSONExporter
import org.totschnig.myexpenses.export.QifExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.fragment.BaseTransactionList
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Transaction
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

class ExportViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    companion object {
        const val KEY_FORMAT = "format"
        const val KEY_DATE_FORMAT = "dateFormat"
        const val KEY_TIME_FORMAT = "timeFormat"
        const val KEY_ENCODING = "encoding"
        const val KEY_DECIMAL_SEPARATOR = "export_decimal_separator"
        const val KEY_NOT_YET_EXPORTED_P = "notYetExportedP"
        const val KEY_DELETE_P = "deleteP"
        const val KEY_EXPORT_HANDLE_DELETED = "export_handle_deleted"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_DELIMITER = "export_delimiter"
        const val KEY_MERGE_P = "export_merge_accounts"
    }

    private val _publishProgress: MutableSharedFlow<String?> = MutableSharedFlow()
    private val _result: MutableStateFlow<Pair<ExportFormat, List<Uri>>?> = MutableStateFlow(null)
    val publishProgress: SharedFlow<String?> = _publishProgress
    val result: StateFlow<Pair<ExportFormat, List<Uri>>?> = _result

    fun startExport(args: Bundle) {
        viewModelScope.launch(coroutineDispatcher) {
            val format: ExportFormat = args.getSerializable(KEY_FORMAT) as ExportFormat
            _result.update {
                format to buildList {

                    val application = getApplication<MyApplication>()
                    val deleteP = args.getBoolean(KEY_DELETE_P)
                    val notYetExportedP = args.getBoolean(KEY_NOT_YET_EXPORTED_P)
                    val mergeP = args.getBoolean(KEY_MERGE_P)
                    val dateFormat = args.getString(KEY_DATE_FORMAT)!!
                    val timeFormat = args.getString(KEY_TIME_FORMAT)
                    val decimalSeparator: Char = args.getChar(KEY_DECIMAL_SEPARATOR)
                    val accountId = args.getLong(DatabaseConstants.KEY_ROWID)
                    val currency = args.getString(DatabaseConstants.KEY_CURRENCY)
                    val encoding = args.getString(KEY_ENCODING)!!
                    val handleDelete = args.getInt(KEY_EXPORT_HANDLE_DELETED)
                    val filter =
                        WhereFilter(args.getParcelableArrayList(BaseTransactionList.KEY_FILTER)!!)
                    val fileName = args.getString(KEY_FILE_NAME)!!
                    val delimiter = args.getChar(KEY_DELIMITER)

                    val accountIds: Array<Long> = if (accountId > 0L) {
                        arrayOf(accountId)
                    } else {
                        var selection = "${DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS} = 0"
                        var selectionArgs: Array<String>? = null
                        if (currency != null && currency != AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE) {
                            selection += " AND ${DatabaseConstants.KEY_CURRENCY} = ?"
                            selectionArgs = arrayOf(currency)
                        }
                        application.contentResolver.query(
                            TransactionProvider.ACCOUNTS_URI,
                            arrayOf(DatabaseConstants.KEY_ROWID),
                            selection,
                            selectionArgs,
                            null
                        )?.use {
                            DbUtils.getLongArrayFromCursor(it, DatabaseConstants.KEY_ROWID)
                        } ?: throw IOException("Cursor was null")
                    }
                    var account: Account?
                    val appDir = AppDirHelper.getAppDir(application)
                    val context = ContextHelper.wrap(
                        application,
                        application.appComponent.userLocaleProvider().getUserPreferredLocale()
                    )
                    if (appDir == null) {
                        publishProgress(context.getString(R.string.external_storage_unavailable))

                    } else {
                        val oneFile = accountIds.size == 1 || mergeP
                        val destDir = if (oneFile) {
                            appDir
                        } else {
                            AppDirHelper.newDirectory(appDir, fileName)
                        }
                        if (destDir != null) {
                            val successfullyExported = ArrayList<Account>()
                            val simpleDateFormat = SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
                            val now = Date()
                            for (i in accountIds.indices) {
                                account = Account.getInstanceFromDb(accountIds[i])
                                if (account == null) continue
                                publishProgress(account.label + " ...")
                                try {
                                    val append = mergeP && i > 0
                                    val fileNameForAccount =
                                        if (oneFile) fileName else String.format(
                                            "%s-%s", Utils.escapeForFileName(account.label),
                                            simpleDateFormat.format(now)
                                        )
                                    val exporter = when (format) {
                                        ExportFormat.CSV -> CsvExporter(
                                            account,
                                            filter,
                                            notYetExportedP,
                                            dateFormat,
                                            decimalSeparator,
                                            encoding,
                                            !append,
                                            delimiter,
                                            mergeP,
                                            prefHandler.getBoolean(PrefKey.CSV_EXPORT_SPLIT_CATEGORIES, false),
                                            prefHandler.getBoolean(PrefKey.CSV_EXPORT_SPLIT_AMOUNT, true),
                                            timeFormat
                                        )
                                        ExportFormat.QIF -> QifExporter(
                                            account,
                                            filter,
                                            notYetExportedP,
                                            dateFormat,
                                            decimalSeparator,
                                            encoding
                                        )
                                        ExportFormat.JSON -> JSONExporter(
                                            account,
                                            filter,
                                            notYetExportedP,
                                            dateFormat,
                                            decimalSeparator,
                                            encoding,
                                            preamble = if (mergeP && i == 0) "[" else "",
                                            appendix = if (mergeP) if (i < accountIds.size - 1) "," else "]" else ""
                                        )
                                    }
                                    val result = exporter.export(context, lazy {
                                        AppDirHelper.buildFile(
                                            destDir, fileNameForAccount, format.mimeType,
                                            append, true
                                        )?.let {
                                            Result.success(it)
                                        } ?: Result.failure(createFileFailure(context, destDir, fileName))
                                    }, append)
                                    result.onSuccess {
                                        if (!append && prefHandler.getBoolean(
                                                PrefKey.PERFORM_SHARE,
                                                false
                                            )
                                        ) {
                                            add(it)
                                        }
                                        successfullyExported.add(account)
                                        publishProgress(
                                            "..." + context.getString(
                                                R.string.export_sdcard_success,
                                                FileUtils.getPath(context, it)
                                            )
                                        )
                                    }.onFailure {
                                        publishProgress("... " + it.message)
                                    }
                                } catch (e: IOException) {
                                    publishProgress(
                                        "... " + context.getString(
                                            R.string.export_sdcard_failure,
                                            appDir.name,
                                            e.message
                                        )
                                    )
                                }
                            }
                            for (a in successfullyExported) {
                                try {
                                    if (deleteP) {
                                        check(!a.isSealed) { "Trying to reset account that is sealed" }
                                        reset(a, filter, handleDelete, fileName)
                                    } else {
                                        a.markAsExported(filter)
                                    }
                                } catch (e: Exception) {
                                    publishProgress("ERROR: " + e.message)
                                    CrashHandler.report(e)
                                }
                            }
                        } else {
                            publishProgress(
                                "ERROR: " + createFileFailure(
                                    context,
                                    appDir,
                                    fileName
                                ).message
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun publishProgress(string: String) {
        _publishProgress.emit(string)
    }

    fun resultProcessed() {
        _result.update {
            null
        }
    }

    fun checkAppDir() = liveData(coroutineDispatcher) {
        emit(AppDirHelper.checkAppDir(getApplication()))
    }

    fun hasExported(accountId: Long) = liveData(coroutineDispatcher) {
        val (selection, selectionArgs) =
            if (accountId != Account.HOME_AGGREGATE_ID) {
                if (accountId < 0L) {
                    //aggregate account
                    val aa = AggregateAccount.getInstanceFromDb(accountId)
                    "${DatabaseConstants.KEY_ACCOUNTID} IN (SELECT ${DatabaseConstants.KEY_ROWID} FROM ${DatabaseConstants.TABLE_ACCOUNTS} WHERE ${DatabaseConstants.KEY_CURRENCY} = ?)" to arrayOf(
                        aa.currencyUnit.code
                    )
                } else {
                    DatabaseConstants.KEY_ACCOUNTID + " = ?" to arrayOf(accountId.toString())
                }
            } else null to null
        contentResolver.query(
            Transaction.CONTENT_URI,
            arrayOf("max(" + DatabaseConstants.KEY_STATUS + ")"),
            selection,
            selectionArgs,
            null
        )?.use {
            it.moveToFirst()
            emit(it.getLong(0) == 1L)
        }
    }
}