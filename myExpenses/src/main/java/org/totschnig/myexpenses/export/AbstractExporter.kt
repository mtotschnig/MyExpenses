package org.totschnig.myexpenses.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.io.FileUtils
import timber.log.Timber
import java.io.IOException
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

abstract class AbstractExporter
/**
 * @param account          Account to print
 * @param filter           only transactions matched by filter will be considered
 * @param destDir          destination directory
 * @param fileName         Filename for exported file
 * @param notYetExportedP  if true only transactions not marked as exported will be handled
 * @param dateFormat       format that can be parsed by SimpleDateFormat class
 * @param decimalSeparator , or .
 * @param encoding         the string describing the desired character encoding.
 * @param delimiter   , or ; or \t
 * @param append          append to file
 * @param withAccountColumn put account in column
 */(val account: Account, private val filter: WhereFilter?, private val destDir: DocumentFile, private val fileName: String,
    private val notYetExportedP: Boolean, private val dateFormat: String,
    private val decimalSeparator: Char, private val encoding: String, val append: Boolean) {
    val nfFormat = Utils.getDecimalFormat(account.currencyUnit, decimalSeparator)
    abstract val format: ExportFormat
    abstract fun header(context: Context): String?
    abstract fun line(isSplit: Boolean, dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, methodLabel: String?, status: CrStatus, referenceNumber: String, pictureFileName: String): String
    abstract fun split(dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, pictureFileName: String): String

    @Throws(IOException::class)
    fun export(context: Context): Result<Uri> {
        Timber.i("now starting export")
        //first we check if there are any exportable transactions
        var selection = DatabaseConstants.KEY_ACCOUNTID + " = ? AND " + DatabaseConstants.KEY_PARENTID + " is null"
        var selectionArgs: Array<String?>? = arrayOf(account.id.toString())
        if (notYetExportedP) selection += " AND " + DatabaseConstants.KEY_STATUS + " = " + DatabaseConstants.STATUS_NONE
        if (filter != null && !filter.isEmpty) {
            selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED)
            selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false))
        }
        return Model.cr().query(
                Transaction.EXTENDED_URI,
                null, selection, selectionArgs, DatabaseConstants.KEY_DATE)?.use { cursor ->

            if (cursor.count == 0) {
                Result.ofFailure(R.string.no_exportable_expenses)
            } else {
                AppDirHelper.buildFile(
                        destDir,
                        fileName,
                        format.mimeType,
                        format.mimeType.split("/").toTypedArray()[1],
                        append)?.let { outputFile ->
                    Model.cr().openOutputStream(outputFile.uri, if (append) "wa" else "w")?.let {
                        OutputStreamWriter(it, encoding)
                    }?.let { out ->
                        cursor.moveToFirst()
                        val formatter = SimpleDateFormat(dateFormat, Locale.US)
                        header(context)?.let { out.write(it) }
                        while (cursor.position < cursor.count) {
                            var comment = DbUtils.getString(cursor, DatabaseConstants.KEY_COMMENT)
                            var fullLabel = ""
                            var labelSub = ""
                            var labelMain: String
                            val catId = DbUtils.getLongOrNull(cursor, DatabaseConstants.KEY_CATID)
                            var splits: Cursor? = null
                            var readCat: Cursor
                            val isSplit = DatabaseConstants.SPLIT_CATID == catId
                            if (isSplit) {
                                //split transactions take their full_label from the first split part
                                splits = Model.cr().query(Transaction.CONTENT_URI, null,
                                        DatabaseConstants.KEY_PARENTID + " = " + cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_ROWID)), null, null)
                                readCat = if (splits != null && splits.moveToFirst()) {
                                    splits
                                } else {
                                    cursor
                                }
                            } else {
                                readCat = cursor
                            }
                            var transferPeer = DbUtils.getLongOrNull(readCat, DatabaseConstants.KEY_TRANSFER_PEER)
                            labelMain = DbUtils.getString(readCat, DatabaseConstants.KEY_LABEL_MAIN)
                            if (labelMain.isNotEmpty()) {
                                if (transferPeer != null) {
                                    fullLabel = "[$labelMain]"
                                    labelMain = context.getString(R.string.transfer)
                                    labelSub = fullLabel
                                } else {
                                    labelSub = DbUtils.getString(readCat, DatabaseConstants.KEY_LABEL_SUB)
                                    fullLabel = TextUtils.formatQifCategory(labelMain, labelSub)
                                }
                            }
                            val payee = DbUtils.getString(cursor, DatabaseConstants.KEY_PAYEE_NAME)
                            val dateStr = formatter.format(Date(cursor.getLong(
                                    cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_DATE)) * 1000))
                            var amount = cursor.getLong(
                                    cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_AMOUNT))
                            var bdAmount = Money(account.currencyUnit, amount).amountMajor
                            val status = try {
                                CrStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_CR_STATUS)))
                            } catch (ex: IllegalArgumentException) {
                                CrStatus.UNRECONCILED
                            }
                            val referenceNumber = DbUtils.getString(cursor, DatabaseConstants.KEY_REFERENCE_NUMBER)
                            val methodLabel = cursor.getString(cursor.getColumnIndex(DatabaseConstants.KEY_METHOD_LABEL))
                            val pictureFileName = StringUtils.substringAfterLast(DbUtils.getString(cursor, DatabaseConstants.KEY_PICTURE_URI), "/")
                            out.write(line(isSplit, dateStr, payee, bdAmount, labelMain, labelSub, fullLabel, comment,
                                    methodLabel, status, referenceNumber, pictureFileName))
                            out.write("\n")
                            splits?.use {
                                while (splits.position < splits.count) {
                                    transferPeer = DbUtils.getLongOrNull(splits, DatabaseConstants.KEY_TRANSFER_PEER)
                                    comment = DbUtils.getString(splits, DatabaseConstants.KEY_COMMENT)
                                    labelMain = DbUtils.getString(splits, DatabaseConstants.KEY_LABEL_MAIN)
                                    if (labelMain.isNotEmpty()) {
                                        if (transferPeer != null) {
                                            fullLabel = "[$labelMain]"
                                            labelMain = context.getString(R.string.transfer)
                                            labelSub = fullLabel
                                        } else {
                                            labelSub = DbUtils.getString(splits, DatabaseConstants.KEY_LABEL_SUB)
                                            fullLabel = TextUtils.formatQifCategory(labelMain, labelSub)
                                        }
                                    } else {
                                        fullLabel = Category.NO_CATEGORY_ASSIGNED_LABEL
                                        labelMain = fullLabel
                                        labelSub = ""
                                    }
                                    amount = splits.getLong(
                                            splits.getColumnIndexOrThrow(DatabaseConstants.KEY_AMOUNT))
                                    bdAmount = Money(account.currencyUnit, amount).amountMajor
                                    out.write(split(dateStr, payee, bdAmount, labelMain, labelSub, fullLabel, comment, pictureFileName))
                                    out.write("\n")
                                    splits.moveToNext()
                                }
                            }

                            recordDelimiter()?.let { out.write(it) }
                            cursor.moveToNext()
                        }
                        out.close()
                        Result.ofSuccess(R.string.export_sdcard_success, outputFile.uri, FileUtils.getPath(context, outputFile.uri))
                    }
                } ?: Result.ofFailure(
                        R.string.io_error_unable_to_create_file,
                        fileName, FileUtils.getPath(context, destDir.uri))
            }
        } ?: Result.ofFailure("Cursor is null")
    }

    open fun recordDelimiter(): String? = null
}