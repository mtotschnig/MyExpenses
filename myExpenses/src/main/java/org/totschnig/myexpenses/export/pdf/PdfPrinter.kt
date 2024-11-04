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
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPRow
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPTableEvent
import com.itextpdf.text.pdf.PdfPageEventHelper
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.tagMap
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.CENTER
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.LEFT
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.RIGHT
import org.totschnig.myexpenses.export.pdf.PdfPrinter.VerticalPosition.BOTTOM
import org.totschnig.myexpenses.export.pdf.PdfPrinter.VerticalPosition.TOP
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
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
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData.Companion.fromSequence
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.viewmodel.data.mergeTransfers
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs


object PdfPrinter {
    private const val VOID_MARKER = "void"
    private const val MARGIN_FRACTION = 0.06f

    enum class HorizontalPosition {
        LEFT, CENTER, RIGHT;
    }

    enum class VerticalPosition {
        TOP, BOTTOM;
    }

    private fun getPaperFormat(context: Context, prefHandler: PrefHandler) =
        (prefHandler.getString(PrefKey.PRINT_PAPER_FORMAT, null)?.let {
            PageSize::class.java.getField(it).get(null) as Rectangle
        } ?: when (Utils.getCountryFromTelephonyManager(context)) {
            "ph", "us", "bz", "ca", "pr", "cl", "co", "cr", "gt", "mx", "ni", "pa", "sv", "ve" -> PageSize.LETTER
            else -> PageSize.A4
        }).let {
            if (prefHandler.getString(PrefKey.PRINT_PAPER_ORIENTATION, "PORTRAIT") == "LANDSACPE")
                it.rotate() else it
        }

    private fun getDocument(context: Context, prefHandler: PrefHandler): Document {
        val paperFormat = getPaperFormat(context, prefHandler)
        val margin = paperFormat.width * MARGIN_FRACTION
        return Document(paperFormat, margin, margin, margin, margin)
    }

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
        val helper = PdfHelper(
            prefHandler.getFloat(PrefKey.PRINT_FONT_SIZE, 12f),
            context.myApplication.memoryClass
        )
        var selection = "$KEY_PARENTID is null"
        val selectionArgs: Array<String>
        if (!filter.isEmpty) {
            selection += " AND " + filter.getSelectionForParents()
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
        val document = getDocument(context, prefHandler)
        val sortBy = if (DatabaseConstants.KEY_AMOUNT == account.sortBy) {
            "abs(" + DatabaseConstants.KEY_AMOUNT + ")"
        } else {
            account.sortBy
        }
        context.contentResolver.query(
            account.uriForTransactionList(
                shortenComment = false,
                extended = true
            ),
            Transaction2.projection(
                account.id,
                account.grouping,
                prefHandler
            ),
            selection, selectionArgs, sortBy + " " + account.sortDirection
        )!!.use { cursor ->
            if (cursor.count == 0) {
                throw Exception("No data")
            }

            PdfWriter.getInstance(
                document,
                context.contentResolver.openOutputStream(outputFile.uri)
            ).pageEvent = object : PdfPageEventHelper() {


                override fun onEndPage(writer: PdfWriter, document: Document) {
                    val cb = writer.getDirectContent()
                    fun print(
                        cb: PdfContentByte,
                        content: PrefKey,
                        horizontalPosition: HorizontalPosition,
                        verticalPosition: VerticalPosition
                    ) {
                        prefHandler.getString(content)?.let {
                            val text = it
                                .replace("{generator}", context.getString(R.string.app_name))
                                .replace("{page}", document.pageNumber.toString())
                                .replace(
                                    "{date}", LocalDate.now().format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                                    )
                                )
                            val x = when (horizontalPosition) {
                                LEFT -> document.left()
                                CENTER -> (document.right() - document.left()) / 2 + document.leftMargin()
                                RIGHT -> document.right()
                            }
                            val y = when (verticalPosition) {
                                TOP -> document.top() + 10
                                BOTTOM -> document.bottom() - 10
                            }
                            val alignment = when (horizontalPosition) {
                                LEFT -> Element.ALIGN_LEFT
                                CENTER -> Element.ALIGN_CENTER
                                RIGHT -> Element.ALIGN_RIGHT
                            }
                            ColumnText.showTextAligned(
                                cb, alignment, helper.print(text, FontType.NORMAL), x, y, 0F
                            )
                        }
                    }
                    print(cb, PrefKey.PRINT_HEADER_LEFT, LEFT, TOP)
                    print(cb, PrefKey.PRINT_HEADER_CENTER, CENTER, TOP)
                    print(cb, PrefKey.PRINT_HEADER_RIGHT, RIGHT, TOP)
                    print(cb, PrefKey.PRINT_FOOTER_LEFT, LEFT, BOTTOM)
                    print(cb, PrefKey.PRINT_FOOTER_CENTER, CENTER, BOTTOM)
                    print(cb, PrefKey.PRINT_FOOTER_RIGHT, RIGHT, BOTTOM)
                }
            }
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
                            ),
                    filter.takeIf { !it.isEmpty }?.let { whereFilter ->
                        whereFilter.criteria.joinToString { it.prettyPrint(context) }
                    }
                )
                addTransactionList(
                    document,
                    cursor,
                    helper,
                    context,
                    account,
                    filter,
                    currencyUnit,
                    currencyFormatter,
                    currencyContext,
                    prefHandler.getBoolean(PrefKey.UI_ITEM_RENDERER_ORIGINAL_AMOUNT, false)
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
        subTitle: String,
        subTitle2: String?
    ) {
        val preface = PdfPTable(1)
        preface.addCell(helper.printToCell(title, FontType.TITLE))
        preface.addCell(helper.printToCell(subTitle, FontType.BOLD))
        subTitle2?.let {
            preface.addCell(helper.printToCell(it, FontType.BOLD))
        }
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
        currencyFormatter: ICurrencyFormatter,
        currencyContext: CurrencyContext,
        withOriginalAmount: Boolean
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
        for (transaction in transactionCursor.asSequence.map {
            Transaction2.fromCursor(
                currencyContext,
                it,
                tagMap,
                accountCurrency = account.currencyUnit
            )
        }.asIterable().let {
            if (account.isAggregate) it.mergeTransfers(
                account,
                currencyContext.homeCurrencyString
            ) else it
        }) {
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
                table = helper.newTable(if (withOriginalAmount) 5 else 4)
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
                var widths = intArrayOf(1, 5, 3, 2)
                if (withOriginalAmount) {
                    widths += 2
                }
                if (table.runDirection == PdfWriter.RUN_DIRECTION_RTL) {
                    widths.reverse()
                }
                table.setWidths(widths)
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
                                ) + ") "
                            }
                        }
                        splitText += currencyFormatter.convAmount(
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
                if (catId != null) {
                    catText += transaction.categoryPath
                }
                if (transaction.transferPeer != null) {
                    if (catText.isNotEmpty()) {
                        catText += " "
                    }
                    catText += "(" + Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor) + transaction.transferAccountLabel + ")"
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

            val fontType = if (account.id < 0 && transaction.isSameCurrency) FontType.NORMAL else
                if (transaction.amount.amountMinor < 0) FontType.EXPENSE else FontType.INCOME

            cell = helper.printToCell(
                currencyFormatter.formatMoney(
                    if (transaction.type == FLAG_NEUTRAL) transaction.amount.absolute() else transaction.amount
                ), fontType
            )
            cell.horizontalAlignment = Element.ALIGN_RIGHT
            table.addCell(cell)
            val emptyCell = helper.emptyCell()
            if (withOriginalAmount) {
                table.addCell(
                    transaction.originalAmount?.let {
                        helper.printToCell(
                            currencyFormatter.formatMoney(if (transaction.type == FLAG_NEUTRAL) transaction.amount.absolute() else transaction.amount),
                            fontType
                        )
                    }?.apply { horizontalAlignment = Element.ALIGN_RIGHT } ?: emptyCell
                )
            }
            val hasComment = !transaction.comment.isNullOrEmpty()
            val hasTags = transaction.tagList.isNotEmpty()
            if (hasComment || hasTags) {
                table.addCell(emptyCell)
                if (hasComment) {
                    cell = helper.printToCell(transaction.comment!!, FontType.ITALIC)
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
                    cell = helper.printToCell(
                        transaction.tagList.joinToString { it.second },
                        FontType.BOLD
                    )
                    if (isVoid) {
                        cell.phrase.chunks[0].setGenericTag(VOID_MARKER)
                    }
                    if (!hasComment) {
                        cell.colspan = 2
                    }
                    table.addCell(cell)
                }
                table.addCell(emptyCell)
                if (withOriginalAmount) {
                    table.addCell(emptyCell)
                }
            }
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
