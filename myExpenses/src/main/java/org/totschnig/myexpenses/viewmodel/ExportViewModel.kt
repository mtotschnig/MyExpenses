package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
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
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.markAsExported
import org.totschnig.myexpenses.export.CsvExporter
import org.totschnig.myexpenses.export.JSONExporter
import org.totschnig.myexpenses.export.QifExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.export.pdf.PdfPrinter
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_EXPORTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.BaseCriterion
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        const val EXPORT_HANDLE_DELETED_DO_NOTHING = -1
        const val EXPORT_HANDLE_DELETED_UPDATE_BALANCE = 0
        const val EXPORT_HANDLE_DELETED_CREATE_HELPER = 1
    }

    private val _publishProgress: MutableSharedFlow<String?> = MutableSharedFlow()
    private val _result: MutableStateFlow<Pair<ExportFormat, List<DocumentFile>>?> = MutableStateFlow(null)
    private val _pdfResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    val publishProgress: SharedFlow<String?> = _publishProgress
    val result: StateFlow<Pair<ExportFormat, List<DocumentFile>>?> = _result
    val pdfResult: StateFlow<Result<Pair<Uri, String>>?> = _pdfResult

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
                    val accountId = args.getLong(KEY_ROWID)
                    val currency = args.getString(KEY_CURRENCY)
                    val encoding = args.getString(KEY_ENCODING)!!
                    val handleDelete = args.getInt(KEY_EXPORT_HANDLE_DELETED)
                    val filter =
                        WhereFilter(args.getParcelableArrayList(KEY_FILTER)!!)
                    val fileName = args.getString(KEY_FILE_NAME)!!
                    val delimiter = args.getChar(KEY_DELIMITER)

                    val accountIds= if (accountId > 0L) {
                        listOf(accountId)
                    } else {
                        var selection = "${DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS} = 0"
                        var selectionArgs: Array<String>? = null
                        if (currency != null) {
                            selection += " AND $KEY_CURRENCY = ?"
                            selectionArgs = arrayOf(currency)
                        }
                        //noinspection Recycle
                        application.contentResolver.query(
                            TransactionProvider.ACCOUNTS_URI,
                            arrayOf(KEY_ROWID),
                            selection,
                            selectionArgs,
                            null
                        )?.useAndMapToList {
                            it.getLong(KEY_ROWID)
                        } ?: throw IOException("Cursor was null")
                    }
                    AppDirHelper.getAppDir(application).onFailure {
                        publishProgress(localizedContext.getString(R.string.io_error_appdir_null))
                    }.onSuccess { appDir ->
                        var account: Account?
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
                                account = repository.loadAccount(accountIds[i])
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
                                            currencyContext,
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
                                            timeFormat,
                                            prefHandler.getBoolean(PrefKey.CSV_EXPORT_ORIGINAL_EQUIVALENT_AMOUNTS, false),
                                            prefHandler.requireString(PrefKey.CSV_EXPORT_CATEGORY_SEPARATOR, " > ")
                                        )
                                        ExportFormat.QIF -> QifExporter(
                                            account,
                                            currencyContext,
                                            filter,
                                            notYetExportedP,
                                            dateFormat,
                                            decimalSeparator,
                                            encoding
                                        )
                                        ExportFormat.JSON -> JSONExporter(
                                            account,
                                            currencyContext,
                                            filter,
                                            notYetExportedP,
                                            dateFormat,
                                            decimalSeparator,
                                            encoding,
                                            preamble = if (mergeP && i == 0) "[" else "",
                                            appendix = if (mergeP) if (i < accountIds.size - 1) "," else "]" else ""
                                        )
                                    }
                                    val result = exporter.export(localizedContext, lazy {
                                        AppDirHelper.buildFile(
                                            destDir,
                                            "$fileNameForAccount.${format.extension}",
                                            format.mimeType,
                                            append
                                        )?.let {
                                            Result.success(it)
                                        } ?: Result.failure(
                                            createFileFailure(
                                                localizedContext,
                                                destDir,
                                                fileName
                                            )
                                        )
                                    }, append)
                                    result.onSuccess {
                                        if (!append) {
                                            add(it)
                                        }
                                        successfullyExported.add(account)
                                        publishProgress(
                                            "..." + localizedContext.getString(
                                                R.string.export_sdcard_success,
                                                it.displayName
                                            )
                                        )
                                    }.onFailure {
                                        publishProgress("... " + it.message)
                                    }
                                } catch (e: IOException) {
                                    publishProgress(
                                        "... " + localizedContext.getString(
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
                                        if (a.isSealed) {
                                            publishProgress(getString(R.string.object_sealed))
                                        } else {
                                            reset(a, filter, handleDelete, fileName)
                                        }
                                    } else {
                                        repository.markAsExported(a.id, filter)
                                    }
                                } catch (e: Exception) {
                                    publishProgress("ERROR: " + e.message)
                                    CrashHandler.report(e)
                                }
                            }
                        } else {
                            publishProgress(
                                "ERROR: " + createFileFailure(
                                    localizedContext,
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

    fun pdfResultProcessed() {
        _pdfResult.update {
            null
        }
    }

    fun checkAppDir() = liveData(coroutineDispatcher) {
        emit(AppDirHelper.checkAppDir(getApplication()))
    }

    fun hasExported(account: DataBaseAccount) = liveData(coroutineDispatcher) {
        contentResolver.query(
            account.uriForTransactionList(extended = false),
            arrayOf("count(*)"),
            "$KEY_STATUS = $STATUS_EXPORTED",
            null,
            null
        )?.use {
            it.moveToFirst()
            emit(it.getInt(0) > 0)
        }
    }

    fun print(account: FullAccount, whereFilter: BaseCriterion?) {
        viewModelScope.launch(coroutineContext()) {
            _pdfResult.update {
                AppDirHelper.checkAppDir(getApplication()).mapCatching {
                    PdfPrinter.print(localizedContext, account, it, whereFilter)
                }
            }
        }
    }
}