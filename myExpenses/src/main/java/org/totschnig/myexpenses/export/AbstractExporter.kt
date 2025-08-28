package org.totschnig.myexpenses.export

import android.content.Context
import android.database.Cursor
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.localizedLabelForPaymentMethod
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_ATTACHMENTS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.calculateEquivalentAmount
import org.totschnig.myexpenses.provider.fileName
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.Companion.lazyMap
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

abstract class AbstractExporter
/**
 * @param account          Account to print
 * @param filter           only transactions matched by filter will be considered
 * @param notYetExportedP  if true only transactions not marked as exported will be handled
 * @param dateFormat       format that can be parsed by SimpleDateFormat class
 * @param decimalSeparator , or .
 * @param encoding         the string describing the desired character encoding.
 */
    (
    val account: Account,
    val currencyContext: CurrencyContext,
    private val filter: Criterion?,
    private val notYetExportedP: Boolean,
    private val dateFormat: String,
    private val decimalSeparator: Char,
    private val encoding: String
) {

    val currencyUnit = currencyContext[account.currency]

    val openingBalance = Money(currencyUnit, account.openingBalance).amountMajor

    val nfFormats: Map<CurrencyUnit, DecimalFormat> = lazyMap {
        Utils.getDecimalFormat(it, decimalSeparator)
    }

    val nfFormat = nfFormats.getValue(currencyUnit)

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat)

    abstract val format: ExportFormat

    abstract fun header(context: Context): String?

    abstract fun TransactionDTO.marshall(categoryPaths: Map<Long, List<String>>): String

    open val useCategoryOfFirstPartForParent = true

    private val categoryTree: MutableMap<Long, Pair<String, Long>> = mutableMapOf()
    val categoryPaths: MutableMap<Long, List<String>> = mutableMapOf()

    open val withEquivalentAmount = false

    @Throws(IOException::class)
    open fun export(
        context: Context,
        outputStream: Lazy<Result<DocumentFile>>,
        append: Boolean
    ): Result<DocumentFile> {
        context.contentResolver.query(
            TransactionProvider.CATEGORIES_URI,
            arrayOf(KEY_ROWID, KEY_LABEL, KEY_PARENTID), null, null, null
        )?.use { cursor ->
            cursor.asSequence.forEach {
                categoryTree[it.getLong(0)] = it.getString(1) to it.getLong(2)
            }
        }
        //first we check if there are any exportable transactions
        var selection = "$KEY_PARENTID is null"
        if (notYetExportedP) selection += " AND $KEY_STATUS = $STATUS_NONE"
        val selectionArgs = if (filter != null) {
            selection += " AND " + filter.getSelectionForParents()
            filter.getSelectionArgs(false)
        } else null
        val projection = arrayOf(
            KEY_UUID,
            KEY_ROWID,
            KEY_CATID,
            KEY_DATE,
            KEY_PAYEE_NAME,
            KEY_AMOUNT,
            KEY_COMMENT,
            localizedLabelForPaymentMethod(
                context,
                KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            TRANSFER_ACCOUNT_LABEL,
            KEY_EQUIVALENT_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_ORIGINAL_AMOUNT,
            KEY_EXCHANGE_RATE
        )

        fun Cursor.ingestCategoryPaths() {
            asSequence.forEach { cursor ->
                cursor.getLongOrNull(KEY_CATID)?.takeIf { it != SPLIT_CATID }?.let { categoryId ->
                    categoryPaths.computeIfAbsent(categoryId) {
                        var catId: Long? = categoryId
                        buildList {
                            while (catId != null) {
                                val pair = categoryTree[catId]
                                catId = if (pair == null) {
                                    null
                                } else {
                                    add(pair.first)
                                    pair.second
                                }
                            }
                        }.reversed()
                    }
                }
            }
        }

        fun Cursor.toDTO(isPart: Boolean = false): TransactionDTO {
            val rowId = getLong(KEY_ROWID)
            val catId = getLongOrNull(KEY_CATID)
            val isSplit = SPLIT_CATID == catId
            val splitCursor = if (isSplit) context.contentResolver.query(
                TransactionProvider.EXTENDED_URI,
                projection,
                "$KEY_PARENTID = ?",
                arrayOf(rowId.toString()),
                KEY_ROWID
            ) else null
            val readCat =
                splitCursor?.takeIf { useCategoryOfFirstPartForParent && it.moveToFirst() } ?: this

            //noinspection Recycle
            val tagList = context.contentResolver.query(
                TransactionProvider.TRANSACTIONS_TAGS_URI,
                arrayOf(KEY_LABEL),
                "$KEY_TRANSACTIONID = ?",
                arrayOf(rowId.toString()),
                null
            )?.useAndMapToList { it.getString(0) }?.takeIf { it.isNotEmpty() }

            //noinspection Recycle
            val attachmentList = context.contentResolver.query(
                TRANSACTIONS_ATTACHMENTS_URI,
                arrayOf(KEY_URI),
                "$KEY_TRANSACTIONID = ?", arrayOf(rowId.toString()),
                null
            )?.useAndMapToList {
                val uri = it.getString(0).toUri()
                //We should only see file uri from unit test
                if (uri.scheme == "file") uri.toFile().name else uri.fileName(context)
            }?.takeIf { it.isNotEmpty() }?.filterNotNull()

            val originalCurrency = getStringOrNull(KEY_ORIGINAL_CURRENCY)

            val money = Money(currencyUnit, getLong(KEY_AMOUNT))
            val transactionDTO = TransactionDTO(
                uuid = getString(KEY_UUID),
                date = epoch2ZonedDateTime(getLong(KEY_DATE)),
                payee = getStringOrNull(KEY_PAYEE_NAME),
                amount = money.amountMajor,
                currency = money.currencyUnit.code,
                catId = readCat.getLongOrNull(KEY_CATID),
                transferAccount = readCat.getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL),
                comment = getStringOrNull(KEY_COMMENT)?.takeIf { it.isNotEmpty() },
                methodLabel = if (isPart) null else getStringOrNull(KEY_METHOD_LABEL),
                status = if (isPart) null else
                    enumValueOrDefault(
                        getStringOrNull(KEY_CR_STATUS),
                        CrStatus.UNRECONCILED
                    ),
                referenceNumber = if (isPart) null else getStringOrNull(KEY_REFERENCE_NUMBER)
                    ?.takeIf { it.isNotEmpty() },
                attachmentFileNames = attachmentList,
                tagList = tagList,
                splits = splitCursor?.let { splits ->
                    splits.moveToPosition(-1)
                    splits.ingestCategoryPaths()
                    splits.moveToPosition(-1)
                    splits.asSequence.map {
                        it.toDTO(isPart = true)
                    }.toList()
                },
                originalCurrency = originalCurrency,
                originalAmount = originalCurrency?.let {
                    Money(currencyContext[originalCurrency],
                        getLong(KEY_ORIGINAL_AMOUNT)).amountMajor
                },
                equivalentAmount = if (withEquivalentAmount)
                    calculateEquivalentAmount(currencyContext.homeCurrencyUnit, money).amountMajor
                else null
            )
            splitCursor?.close()
            return transactionDTO
        }

        return context.contentResolver.query(
            account.uriForTransactionList(), projection, selection, selectionArgs, KEY_DATE
        )?.use { cursor ->

            if (cursor.count == 0) {
                Result.failure(Exception(context.getString(R.string.no_exportable_expenses)))
            } else {
                cursor.ingestCategoryPaths()

                val output = outputStream.value.getOrThrow()
                (context.contentResolver.openOutputStream(output.uri, if (append) "wa" else "w")
                    ?: throw IOException("openOutputStream returned null")).use { outputStream ->
                    if (encoding == ENCODING_UTF_8_BOM && !append) {
                        outputStream.write(UTF_8_BOM)
                    }
                    OutputStreamWriter(outputStream, if (encoding == ENCODING_UTF_8_BOM) ENCODING_UTF_8 else encoding).use { out ->
                        cursor.moveToFirst()
                        header(context)?.let { out.write(it) }
                        while (cursor.position < cursor.count) {
                            out.write(cursor.toDTO().marshall(categoryPaths))

                            recordDelimiter(cursor.position == cursor.count - 1)?.let { out.write(it) }

                            cursor.moveToNext()
                        }

                        footer()?.let { out.write(it) }

                        Result.success(output)
                    }
                }
            }
        } ?: Result.failure(Exception("Cursor is null"))
    }

    open fun recordDelimiter(isLastLine: Boolean): String? = "\n"

    open fun footer(): String? = null

    fun TransactionDTO.fullLabel(categoryPaths: Map<Long, List<String>>) =
        transferAccount?.let { "[$it]" } ?: categoryPath(categoryPaths)

    open val categoryPathSeparator = ":"

    open fun sanitizeCategoryLabel(label: String) =
        label.replace("/","\\u002F").replace(":","\\u003A")

    fun String.escapeNewLine() = split("\n").joinToString( " + " )

    private fun TransactionDTO.categoryPath(categoryPaths: Map<Long, List<String>>) = catId?.let { cat ->
        categoryPaths[cat]?.joinToString(categoryPathSeparator, transform = ::sanitizeCategoryLabel)
    }

    companion object {
        const val ENCODING_UTF_8 = "UTF-8"
        const val ENCODING_UTF_8_BOM = "UTF-8-BOM"
        const val ENCODING_LATIN_1 = "ISO-8859-1"
        val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}