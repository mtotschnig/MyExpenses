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
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.Rectangle.NO_BORDER
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPRow
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPTableEvent
import com.itextpdf.text.pdf.PdfPageEventHelper
import com.itextpdf.text.pdf.PdfWriter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
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
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.printLayout
import org.totschnig.myexpenses.preference.printLayoutColumnWidth
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper.timeStampedFile
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.PdfHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.util.ui.dateTimeFormatterLegacy
import org.totschnig.myexpenses.viewmodel.Field
import org.totschnig.myexpenses.viewmodel.PrintLayoutConfigurationViewModel.Companion.asColumns
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.viewmodel.data.mergeTransfers
import java.io.IOException
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.sign


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
            if (prefHandler.getString(
                    PrefKey.PRINT_PAPER_ORIENTATION,
                    context.getString(R.string.orientation_portrait)
                ) == context.getString(R.string.orientation_landscape)
            )
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
        filter: Criterion?,
        colorSource: ColorSource,
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
        if (filter != null) {
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
                        verticalPosition: VerticalPosition,
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
                val subTitle2 = Phrase()
                subTitle2.addAll(
                    helper.print0(
                        context.getString(R.string.current_balance) + " : ",
                        FontType.BOLD
                    )
                )
                subTitle2.addAll(
                    helper.print0(
                        currencyFormatter.formatMoney(
                            Money(
                                currencyUnit,
                                account.currentBalance
                            )
                        ),
                        when (account.currentBalance.sign) {
                            1 -> FontType.INCOME_BOLD
                            -1 -> FontType.EXPENSE_BOLD
                            else -> FontType.BOLD
                        }
                    )
                )
                addHeader(
                    document,
                    helper,
                    account.label,
                    subTitle2,
                    filter?.prettyPrint(context)
                )
                val itemDateFormat = dateTimeFormatterLegacy(account, prefHandler, context)?.first
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
                    prefHandler.printLayout.asColumns(),
                    prefHandler.printLayoutColumnWidth.toIntArray(),
                    colorSource,
                    itemDateFormat
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
        subTitle: Phrase,
        subTitle2: String?,
    ) {
        val preface = PdfPTable(1)
        preface.addCell(helper.printToCell(title, FontType.TITLE))
        preface.addCell(helper.printToCell(subTitle))
        subTitle2?.let {
            preface.addCell(helper.printToCell(it, FontType.BOLD))
        }
        document.add(preface)
        val empty = Paragraph()
        addEmptyLine(empty)
        document.add(empty)
    }

    @Throws(DocumentException::class, IOException::class)
    private fun addTransactionList(
        document: Document,
        transactionCursor: Cursor,
        helper: PdfHelper,
        context: Context,
        account: FullAccount,
        filter: Criterion?,
        currencyUnit: CurrencyUnit,
        currencyFormatter: ICurrencyFormatter,
        currencyContext: CurrencyContext,
        layout: List<List<Field>>,
        columnWidths: IntArray,
        colorSource: ColorSource,
        itemDateFormat: DateFormat?,
    ) {
        val (builder, selection, selectionArgs) = account.groupingQuery(filter)
        val integerHeaderRowMap = context.contentResolver.query(
            builder.build(), null, selection, selectionArgs, null
        )!!.use {
            HeaderData.fromSequence(
                account.openingBalance,
                account.grouping,
                currencyUnit,
                it.asSequence
            )
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

                if (account.grouping != Grouping.NONE) {

                    val groupHeader = helper.print(
                        account.grouping.getDisplayTitle(
                            context,
                            transaction.year,
                            headerRow.second,
                            DateInfo.load(context.contentResolver),
                            headerRow.weekStart,
                            false
                        ),
                        FontType.HEADER
                    )
                    document.add(groupHeader)
                }

                val groupSummary = helper.newTable(3)

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
                var cell = helper.printToCell(
                    if (filter == null) String.format(
                        "%s %s = %s",
                        currencyFormatter.formatMoney(headerRow.previousBalance), formattedDelta,
                        currencyFormatter.formatMoney(interimBalance)
                    ) else formattedDelta, FontType.HEADER
                )
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                groupSummary.addCell(
                    helper.printToCell(
                        "Start: ${
                            currencyFormatter.formatMoney(
                                headerRow.previousBalance
                            )
                        }"
                    )
                )
                groupSummary.addCell(helper.printToCell("Δ: $formattedDelta").apply {
                    horizontalAlignment = Element.ALIGN_CENTER
                })
                groupSummary.addCell(
                    helper.printToCell(
                        "End: ${
                            currencyFormatter.formatMoney(
                                interimBalance
                            )
                        }"
                    ).apply {
                        horizontalAlignment = Element.ALIGN_RIGHT
                    })
                groupSummary.addCell(
                    PdfPCell(
                        Phrase().apply {
                            addAll(
                                helper.print0(
                                    "Income: + ${
                                        currencyFormatter.formatMoney(
                                            sumIncome
                                        )
                                    }", FontType.INCOME
                                )
                            )
                            add(" | ")
                            addAll(
                                helper.print0(
                                    "Expenses: + ${
                                        currencyFormatter.formatMoney(
                                            sumExpense
                                        )
                                    }", FontType.EXPENSE
                                )
                            )
                            add(" | ")
                            addAll(
                                helper.print0(
                                    "Transfers: + ${
                                        currencyFormatter.formatMoney(
                                            sumTransfer
                                        )
                                    }", FontType.TRANSFER
                                )
                            )
                        }).apply {
                        colspan = 3
                        horizontalAlignment = Element.ALIGN_CENTER
                        border = NO_BORDER
                    }
                )

                val header2Table = helper.newTable(3)
                header2Table.widthPercentage = 100f
                cell = helper.printToCell(
                    "+ " + currencyFormatter.formatMoney(sumIncome),
                    FontType.INCOME
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                header2Table.addCell(cell)
                cell = helper.printToCell(
                    "- " + currencyFormatter.formatMoney(sumExpense.negate()),
                    FontType.EXPENSE
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                header2Table.addCell(cell)
                cell = helper.printToCell(
                    Transfer.BI_ARROW + " " + currencyFormatter.formatMoney(sumTransfer),
                    FontType.TRANSFER
                )
                cell.horizontalAlignment = Element.ALIGN_CENTER
                header2Table.addCell(cell)
                header2Table.spacingAfter = 2f

                val wrapper2 = PdfPTable(1)
                wrapper2.widthPercentage = 100f

                val line1Cell2 = PdfPCell(groupSummary)
                line1Cell2.border = NO_BORDER
                wrapper2.addCell(line1Cell2)

                val line2Cell2 = PdfPCell(header2Table)
                line2Cell2.border = NO_BORDER
                wrapper2.addCell(line2Cell2)

                val outer2 = PdfPCell(groupSummary)
                outer2.border = Rectangle.BOX
                outer2.setPadding(8f) // nice visual spacing

                val finalContainer2 = PdfPTable(1)
                finalContainer2.widthPercentage = 100f
                finalContainer2.addCell(outer2)
                document.add(finalContainer2)

                table = helper.newTable(layout.size)

                // Header row
                val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)
                layout.forEach {
                    if (it.size == 1) {
                        addHeaderCell(table, it[0].toString(context), headerFont)
                    } else {
                        addComplexHeaderCell(table, headerFont, texts = it.map { it.toString(context) }.toTypedArray())
                    }
                }

/*                if (withOriginalAmount) {
                    addComplexHeaderCell(
                        table,
                        headerFont,
                        border = NO_BORDER,
                        alignment = Element.ALIGN_RIGHT,
                        text1 = "Amount",
                        text2 = "Original amount"
                    )
                } else {
                    addHeaderCell(table, "Amount", headerFont, alignment = Element.ALIGN_RIGHT)
                }*/
                // Repeat header row on every page
                table.setHeaderRows(1)

                table.tableEvent = object : PdfPTableEvent {
                    private fun findFirstChunkGenericTag(row: PdfPRow): Any? {
                        for (cell in row.cells) {
                            if (cell != null) {
                                val phrase = cell.phrase
                                if (phrase != null) {
                                    val chunks = phrase.chunks
                                    if (chunks.isNotEmpty()) {
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
                        canvases: Array<PdfContentByte>,
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
                if (table.runDirection == PdfWriter.RUN_DIRECTION_RTL) {
                    columnWidths.reverse()
                }
                table.setWidths(columnWidths)
                table.spacingBefore = 8f
                table.spacingAfter = 8f
                table.widthPercentage = 100f
                prevHeaderId = currentHeaderId
            }

            val isVoid = transaction.crStatus == CrStatus.VOID

            layout.forEach {
                if (it.size == 1) {
                    table!!.addCell(helper.printToCell(it.first().toString()))
                } else {
                    helper.addNestedCells(table!!, *it.map { helper.printToCell(it.toString()) }.toTypedArray(), border = Rectangle.TOP)
                }
            }

/*            //Column 1 Date
            val cell = helper.printToCell(
                Utils.convDateTime(transaction._date, itemDateFormat!!), //TODO daily grouping without time
                FontType.NORMAL,
                Rectangle.RIGHT + Rectangle.TOP
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
                                splitText += " (" + Transfer.getIndicatorPrefixForLabel(transaction.displayAmount.amountMinor) + splits.getStringOrNull(
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
                    catText += "(" + Transfer.getIndicatorPrefixForLabel(transaction.displayAmount.amountMinor) + transaction.transferAccountLabel + ")"
                }
            }
            if (!transaction.referenceNumber.isNullOrEmpty()) catText =
                "(${transaction.referenceNumber}) $catText"

            val catCell = catText.takeIf { it.isNotEmpty() }?.let { helper.printToCell(catText) }
            val tagCell = transaction.tagList.takeIf { it.isNotEmpty() }?.let {
                helper.printToCell(it.joinToString { it.second })
            }

            //Colum 2 Category // Tags
            helper.addNestedCells(table, catCell, tagCell)

            //Column 3 Payee
            table.addCell(
                helper.printToCell(
                    transaction.payee ?: "",
                    border = Rectangle.TOP + Rectangle.RIGHT
                )
            )

            //Column 4 notes
            table.addCell(
                helper.printToCell(
                    transaction.comment ?: "",
                    border = Rectangle.TOP + Rectangle.RIGHT
                )
            )
            //Column 5 amount
            val fontType = if (account.id < 0 && transaction.isSameCurrency) FontType.NORMAL else
                (colorSource.transformType(transaction.type)
                    ?: when (transaction.displayAmount.amountMinor.sign) {
                        1 -> FLAG_INCOME
                        -1 -> FLAG_EXPENSE
                        else -> FLAG_NEUTRAL
                    }).asColor()

            val amountCell = helper.printToCell(
                currencyFormatter.formatMoney(
                    if (transaction.type == FLAG_NEUTRAL) transaction.displayAmount.absolute() else transaction.displayAmount
                ), fontType, border = Rectangle.RIGHT
            ).apply {
                horizontalAlignment = Element.ALIGN_RIGHT
            }
            val originalAmountCell = if (withOriginalAmount && transaction.originalAmount != null) {
                helper.printToCell(
                    currencyFormatter.formatMoney(if (transaction.type == FLAG_NEUTRAL) transaction.displayAmount.absolute() else transaction.displayAmount),
                    fontType, border = Rectangle.RIGHT
                ).apply { horizontalAlignment = Element.ALIGN_RIGHT }
            } else null
            helper.addNestedCells(table,amountCell, originalAmountCell,  border = Rectangle.TOP)*/
        }
        // now add all this to the document
        document.add(table)
    }

    private fun addEmptyLine(paragraph: Paragraph) {
        paragraph.add(Paragraph(" "))
    }

    private fun Byte.asColor() = when (this) {
        FLAG_INCOME -> FontType.INCOME
        FLAG_EXPENSE -> FontType.EXPENSE
        FLAG_TRANSFER -> FontType.TRANSFER
        else -> FontType.NORMAL
    }

    private fun addHeaderCell(
        table: PdfPTable,
        text: String,
        font: Font,
        alignment: Int = Element.ALIGN_LEFT,
        border: Int = Rectangle.RIGHT,
    ) {
        val cell = PdfPCell(Phrase(text, font))
        cell.setPadding(5.0f)
        cell.horizontalAlignment = alignment
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.border = border
        table.addCell(cell)
    }

    private fun addComplexHeaderCell(
        table: PdfPTable,
        font: Font,
        alignment: Int = Element.ALIGN_LEFT,
        border: Int = Rectangle.RIGHT,
        vararg texts: String,
    ) {
        val nested = PdfPTable(1)
        nested.widthPercentage = 100f
        texts.forEach {
            addHeaderCell(nested, it, font, alignment, NO_BORDER)
        }
        val cell = PdfPCell(nested)
        cell.border = border
        table.addCell(cell)
    }
}
