package org.totschnig.myexpenses.export.pdf

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Element
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPRow
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPTableEvent
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.tagMap
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper.timeStampedFile
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.PdfHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData.Companion.fromSequence
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs

object PdfPrinter {
    private const val VOID_MARKER = "void"

    @Throws(IOException::class, DocumentException::class)
    fun print(
        context: Context,
        account: FullAccount,
        destDir: DocumentFile,
        filter: WhereFilter,
    ): Pair<Uri, String> {
        val currencyFormatter = context.injector.currencyFormatter()
        val currencyContext = context.injector.currencyContext()
        val currencyUnit = currencyContext[account.currency]
        val prefHandler = context.injector.prefHandler()
        val helper = PdfHelper()
        var selection = "$KEY_PARENTID is null"
        val selectionArgs: Array<String>
        if (!filter.isEmpty) {
            selection += " AND " + filter.getSelectionForParents(
                DatabaseConstants.VIEW_EXTENDED,
                false
            )
            selectionArgs = filter.getSelectionArgs(false)
        } else {
            selectionArgs = arrayOf()
        }
        val fileName = account.label.replace("\\W".toRegex(), "")
        val outputFile = timeStampedFile(
            destDir,
            fileName,
            "application/pdf", "pdf"
        ) ?: throw createFileFailure(context, destDir, fileName)
        val document = Document()
        val sortBy = if (DatabaseConstants.KEY_AMOUNT == account.sortBy) {
            "abs(" + DatabaseConstants.KEY_AMOUNT + ")"
        } else {
            account.sortBy
        }
        context.contentResolver.query(
            account.uriForTransactionList(
                mergeTransfers = false,
                shortenComment = false,
                extended = true
            ),
            Transaction2.projection(
                account.id,
                account.grouping,
                currencyContext.homeCurrencyString,
                prefHandler
            ),
            selection, selectionArgs, sortBy + " " + account.sortDirection
        )!!.use {
            if (it.count == 0) {
                throw Exception("No data")
            }

            PdfWriter.getInstance(
                document,
                context.contentResolver.openOutputStream(outputFile.uri)
            )
            document.open()
            try {
                addMetaData(document, account.label)
                addHeader(
                    document,
                    helper,
                    account.label,
                    context.getString(R.string.current_balance) + " : " +
                            currencyFormatter.formatMoney(
                                Money(
                                    currencyUnit,
                                    account.currentBalance
                                )
                            )
                )
                addTransactionList(
                    document,
                    it,
                    helper,
                    context,
                    account,
                    filter,
                    currencyUnit,
                    currencyFormatter
                )
            } finally {
                document.close()
            }
            return outputFile.uri to outputFile.displayName
        }
    }

    private fun addMetaData(document: Document, title: String) {
        document.addTitle(title)
        document.addSubject("Generated by MyExpenses.mobi")
    }

    @Throws(DocumentException::class, IOException::class)
    private fun addHeader(
        document: Document,
        helper: PdfHelper,
        title: String,
        subTitle: String
    ) {
        val preface = PdfPTable(1)
        preface.addCell(helper.printToCell(title, FontType.TITLE))
        preface.addCell(
            helper.printToCell(
                DateFormat.getDateInstance(DateFormat.FULL).format(Date()), FontType.BOLD
            )
        )
        preface.addCell(helper.printToCell(subTitle, FontType.BOLD))
        document.add(preface)
        val empty = Paragraph()
        addEmptyLine(empty, 1)
        document.add(empty)
    }

    @Throws(DocumentException::class, IOException::class)
    private fun addTransactionList(
        document: Document,
        transactionCursor: Cursor,
        helper: PdfHelper,
        context: Context,
        account: FullAccount,
        filter: WhereFilter,
        currencyUnit: CurrencyUnit,
        currencyFormatter: ICurrencyFormatter
    ) {
        val (builder, selection, selectionArgs) = account.groupingQuery(filter)
        val integerHeaderRowMap = context.contentResolver.query(
            builder.build(), null, selection, selectionArgs, null
        )!!.use {
            fromSequence(
                account.openingBalance,
                account.grouping,
                currencyUnit,
                it.asSequence
            )
        }

        val itemDateFormat = when (account.grouping) {
            Grouping.DAY -> android.text.format.DateFormat.getTimeFormat(context)
            Grouping.MONTH -> SimpleDateFormat("dd")
            Grouping.WEEK -> SimpleDateFormat("EEE")
            else -> Utils.localizedYearLessDateFormat(context)
        }
        var table: PdfPTable? = null
        var prevHeaderId = 0
        var currentHeaderId: Int
        val tagMap = context.contentResolver.tagMap
        transactionCursor.moveToFirst()
        while (transactionCursor.position < transactionCursor.count) {
            //could use /with/ scoping function with Kotlin 2.0.
            val transaction =
                Transaction2.fromCursor(
                    cursor = transactionCursor,
                    accountCurrency = account.currencyUnit,
                    tags = tagMap
                )

            currentHeaderId = account.grouping.calculateGroupId(transaction)
            if (currentHeaderId != prevHeaderId) {
                if (table != null) {
                    document.add(table)
                }
                val headerRow = integerHeaderRowMap[currentHeaderId]!!
                table = helper.newTable(2)
                table.widthPercentage = 100f
                var cell = helper.printToCell(
                    account.grouping.getDisplayTitle(
                        context,
                        transaction.year,
                        headerRow.second,
                        DateInfo.load(context.contentResolver),
                        headerRow.weekStart,
                        false
                    ),  //TODO
                    FontType.HEADER
                )
                table.addCell(cell)

                val sumExpense = headerRow.expenseSum
                val sumIncome = headerRow.incomeSum
                val sumTransfer = headerRow.transferSum
                val (_, amountMinor) = headerRow.delta
                val interimBalance = headerRow.interimBalance
                val formattedDelta = String.format(
                    "%s %s", if (java.lang.Long.signum(
                            amountMinor
                        ) > -1
                    ) "+" else "-",
                    currencyFormatter.convAmount(abs(amountMinor), currencyUnit)
                )
                cell = helper.printToCell(
                    if (filter.isEmpty) String.format(
                        "%s %s = %s",
                        currencyFormatter.formatMoney(headerRow.previousBalance), formattedDelta,
                        currencyFormatter.formatMoney(interimBalance)
                    ) else formattedDelta, FontType.HEADER
                )
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                table.addCell(cell)
                document.add(table)
                table = helper.newTable(3)
                table.widthPercentage = 100f
                cell = helper.printToCell(
                    "+ " + currencyFormatter.formatMoney(sumIncome),
                    FontType.NORMAL
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
                cell = helper.printToCell(
                    "- " + currencyFormatter.formatMoney(sumExpense.negate()),
                    FontType.NORMAL
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
                cell = helper.printToCell(
                    Transfer.BI_ARROW + " " + currencyFormatter.formatMoney(sumTransfer),
                    FontType.NORMAL
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
                table.spacingAfter = 2f
                document.add(table)
                val sep = LineSeparator()
                document.add(sep)
                table = helper.newTable(4)
                table.tableEvent = object : PdfPTableEvent {
                    private fun findFirstChunkGenericTag(row: PdfPRow): Any? {
                        for (cell in row.cells) {
                            if (cell != null) {
                                val phrase = cell.phrase
                                if (phrase != null) {
                                    val chunks = phrase.chunks
                                    if (chunks.size > 0) {
                                        val attributes = chunks[0].attributes
                                        if (attributes != null) {
                                            return attributes[Chunk.GENERICTAG]
                                        }
                                    }
                                }
                            }
                        }
                        return null
                    }

                    override fun tableLayout(
                        table1: PdfPTable,
                        widths: Array<FloatArray>,
                        heights: FloatArray,
                        headerRows: Int,
                        rowStart: Int,
                        canvases: Array<PdfContentByte>
                    ) {
                        for (row in rowStart until widths.size) {
                            if (VOID_MARKER == findFirstChunkGenericTag(table1.getRow(row))) {
                                val canvas = canvases[PdfPTable.BASECANVAS]
                                canvas.saveState()
                                canvas.setColorStroke(BaseColor.RED)
                                val left = widths[row][0]
                                val right = widths[row][widths[row].size - 1]
                                val bottom = heights[row]
                                val top = heights[row + 1]
                                val center = (bottom + top) / 2
                                canvas.moveTo(left, center)
                                canvas.lineTo(right, center)
                                canvas.stroke()
                                canvas.restoreState()
                            }
                        }
                    }
                }
                table.setWidths(
                    if (table.runDirection == PdfWriter.RUN_DIRECTION_RTL) intArrayOf(
                        2,
                        3,
                        5,
                        1
                    ) else intArrayOf(1, 5, 3, 2)
                )
                table.spacingBefore = 2f
                table.spacingAfter = 2f
                table.widthPercentage = 100f
                prevHeaderId = currentHeaderId
            }
            var isVoid = false
            try {
                isVoid = transaction.crStatus == CrStatus.VOID
            } catch (ignored: IllegalArgumentException) {
            }
            var cell = helper.printToCell(
                Utils.convDateTime(transaction._date, itemDateFormat),
                FontType.NORMAL
            )
            table!!.addCell(cell)
            if (isVoid) {
                cell.phrase.chunks[0].setGenericTag(VOID_MARKER)
            }
            var catText = ""
            if (account.id < 0) {
                //for aggregate accounts we need to indicate the account name
                catText = transaction.accountLabel + " "
            }
            val catId = transaction.catId
            if (DatabaseConstants.SPLIT_CATID == catId) {
                context.contentResolver.query(
                    Transaction.CONTENT_URI, null,
                    "$KEY_PARENTID = ${transaction.id}", null, null
                )!!.use { splits ->
                    splits.moveToFirst()
                    val catTextBuilder = StringBuilder()
                    while (splits.position < splits.count) {
                        var splitText = splits.getString(DatabaseConstants.KEY_PATH)
                        if (splitText.isNotEmpty()) {
                            if (splits.getLongOrNull(DatabaseConstants.KEY_TRANSFER_PEER) != null) {
                                splitText += " (" + Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor) + splits.getStringOrNull(
                                    DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
                                ) + ")"
                            }
                        } else {
                            splitText = Category.NO_CATEGORY_ASSIGNED_LABEL
                        }
                        splitText += " " + currencyFormatter.convAmount(
                            splits.getLong(
                                splits.getColumnIndexOrThrow(DatabaseConstants.KEY_DISPLAY_AMOUNT)
                            ), currencyUnit
                        )
                        val splitComment = splits.getString(DatabaseConstants.KEY_COMMENT)
                        if (splitComment.isNotEmpty()) {
                            splitText += " ($splitComment)"
                        }
                        catTextBuilder.append(splitText)
                        if (splits.position != splits.count - 1) {
                            catTextBuilder.append("; ")
                        }
                        splits.moveToNext()
                    }
                    catText += catTextBuilder.toString()
                }
            } else {
                catText += if (catId == null) {
                    Category.NO_CATEGORY_ASSIGNED_LABEL
                } else {
                    transaction.categoryPath
                }
                if (transaction.transferPeer != null) {
                    catText += " (" + Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor) + transaction.transferAccountLabel + ")"
                }
            }
            if (!transaction.referenceNumber.isNullOrEmpty()) catText =
                "(${transaction.referenceNumber}) $catText"
            cell = helper.printToCell(catText, FontType.NORMAL)
            if (transaction.payee.isNullOrEmpty()) {
                cell.colspan = 2
            }
            table.addCell(cell)
            if (!transaction.payee.isNullOrEmpty()) {
                table.addCell(helper.printToCell(transaction.payee, FontType.UNDERLINE))
            }
            val t: FontType = if (account.id < 0 && transaction.isSameCurrency) {
                FontType.NORMAL
            } else {
                if (transaction.amount.amountMinor < 0) FontType.EXPENSE else FontType.INCOME
            }
            cell = helper.printToCell(currencyFormatter.formatMoney(transaction.amount), t)
            cell.horizontalAlignment = Element.ALIGN_RIGHT
            table.addCell(cell)
            val hasComment = !transaction.comment.isNullOrEmpty()
            val hasTags = transaction.tagList.isNotEmpty()
            if (hasComment || hasTags) {
                table.addCell(helper.emptyCell())
                if (hasComment) {
                    cell = helper.printToCell(transaction.comment, FontType.ITALIC)
                    if (isVoid) {
                        cell.phrase.chunks.getOrNull(0)?.also {
                            it.setGenericTag(VOID_MARKER)
                        } ?: run {
                            CrashHandler.report(IllegalStateException("Comment ${transaction.comment} considered not null or empty by Kotlin, but has length 0 for Java."))
                        }
                    }
                    if (!hasTags) {
                        cell.colspan = 2
                    }
                    table.addCell(cell)
                }
                if (hasTags) {
                    //TODO make use of color
                    cell = helper.printToCell(transaction.tagList.joinToString { it.first }, FontType.BOLD)
                    if (isVoid) {
                        cell.phrase.chunks[0].setGenericTag(VOID_MARKER)
                    }
                    if (!hasComment) {
                        cell.colspan = 2
                    }
                    table.addCell(cell)
                }
                table.addCell(helper.emptyCell())
            }
            transactionCursor.moveToNext()
        }
        // now add all this to the document
        document.add(table)
    }

    private fun addEmptyLine(paragraph: Paragraph, number: Int) {
        for (i in 0 until number) {
            paragraph.add(Paragraph(" "))
        }
    }
}
