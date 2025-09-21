package org.totschnig.myexpenses.export.pdf

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPRow
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPTableEvent
import com.itextpdf.text.pdf.PdfWriter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.tagMap
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.printLayout
import org.totschnig.myexpenses.preference.printLayoutColumnWidths
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.util.AppDirHelper.timeStampedFile
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.PdfHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.util.ui.dateTimeFormatterLegacy
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.Amount
import org.totschnig.myexpenses.viewmodel.Category
import org.totschnig.myexpenses.viewmodel.CombinedField
import org.totschnig.myexpenses.viewmodel.Date
import org.totschnig.myexpenses.viewmodel.Field
import org.totschnig.myexpenses.viewmodel.Notes
import org.totschnig.myexpenses.viewmodel.OriginalAmount
import org.totschnig.myexpenses.viewmodel.Payee
import org.totschnig.myexpenses.viewmodel.PrintLayoutConfigurationViewModel.Companion.asColumns
import org.totschnig.myexpenses.viewmodel.ReferenceNumber
import org.totschnig.myexpenses.viewmodel.Tags
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.viewmodel.data.mergeTransfers
import timber.log.Timber
import java.io.IOException
import java.text.DateFormat
import kotlin.math.abs
import kotlin.math.sign

object PdfPrinter {
    private const val VOID_MARKER = "void"

    enum class HorizontalPosition {
        LEFT, CENTER, RIGHT;
    }

    enum class VerticalPosition {
        TOP, BOTTOM;
    }

    fun defaultPaperSize(context: Context) = when (Utils.getCountryFromTelephonyManager(context)) {
        "ph", "us", "bz", "ca", "pr", "cl", "co", "cr", "gt", "mx", "ni", "pa", "sv", "ve" -> "LETTER"
        else -> "A4"
    }

    private fun getPaperFormat(context: Context, prefHandler: PrefHandler) =
        prefHandler.requireString(PrefKey.PRINT_PAPER_FORMAT, defaultPaperSize(context)).let {
            PageSize::class.java.getField(it).get(null) as Rectangle
        }.let {
            if (prefHandler.getString(
                    PrefKey.PRINT_PAPER_ORIENTATION,
                    context.getString(R.string.orientation_portrait)
                ) == context.getString(R.string.orientation_landscape)
            )
                it.rotate() else it
        }

    fun getDocument(context: Context, prefHandler: PrefHandler): Document {
        val paperFormat = getPaperFormat(context, prefHandler)
        val hasHeader = !(prefHandler.getString(PrefKey.PRINT_HEADER_LEFT).isNullOrEmpty()
                && prefHandler.getString(PrefKey.PRINT_HEADER_CENTER).isNullOrEmpty()
                && prefHandler.getString(PrefKey.PRINT_HEADER_RIGHT).isNullOrEmpty())
        val hasFooter = !(prefHandler.getString(PrefKey.PRINT_FOOTER_LEFT).isNullOrEmpty()
                && prefHandler.getString(PrefKey.PRINT_FOOTER_CENTER).isNullOrEmpty()
                && prefHandler.getString(PrefKey.PRINT_FOOTER_RIGHT).isNullOrEmpty()
                )
        val baseFontSize = prefHandler.getFloat(PrefKey.PRINT_FONT_SIZE, 12f)
        val marginTop = (paperFormat.height * prefHandler.getFloat(PrefKey.PRINT_MARGIN_TOP, 0.04f))
            .coerceAtLeast(if (hasHeader) baseFontSize * 2 else 0f)
        val marginRight =
            paperFormat.width * prefHandler.getFloat(PrefKey.PRINT_MARGIN_RIGHT, 0.06f)
        val marginBottom =
            (paperFormat.height * prefHandler.getFloat(PrefKey.PRINT_MARGIN_BOTTOM, 0.04f))
                .coerceAtLeast(if (hasFooter) baseFontSize * 2 else 0f)
        val marginLeft = paperFormat.width * prefHandler.getFloat(PrefKey.PRINT_MARGIN_LEFT, 0.06f)
        Timber.d("Margin: $marginLeft, $marginTop, $marginRight, $marginBottom")
        return Document(paperFormat, marginLeft, marginRight, marginTop, marginBottom)
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
        val baseFontSize = prefHandler.getFloat(PrefKey.PRINT_FONT_SIZE, 12f)
        val helper = PdfHelper(
            baseFontSize,
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
        val uri = account.uriForTransactionList(
            shortenComment = false,
            extended = true
        )
        val projection = Transaction2.projection(
            account.id,
            account.grouping,
            prefHandler
        )
        context.contentResolver.query(
            uri,
            projection,
            selection, selectionArgs, sortBy + " " + account.sortDirection
        )!!.use { cursor ->
            if (cursor.count == 0) {
                throw Exception("No data")
            }

            PdfWriter.getInstance(
                document,
                context.contentResolver.openOutputStream(outputFile.uri)
            ).pageEvent = helper.getPageEventHelper(context, prefHandler)
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
                val columnsPreference = prefHandler.printLayout.asColumns()
                val columnWidthsPreference = prefHandler.printLayoutColumnWidths
                val (columns, columnWidths) = if (itemDateFormat == null) {
                    val finalColumns = mutableListOf<List<Field>>()
                    val finalColumnWidths = mutableListOf<Int>()
                    columnsPreference.map { column ->
                        column.mapNotNull { field -> if (field == Date) null else field }
                    }.forEachIndexed { index, list ->
                        if (list.isNotEmpty()) {
                            finalColumns.add(list)
                            finalColumnWidths.add(columnWidthsPreference[index])
                        }
                    }
                    finalColumns to finalColumnWidths
                } else columnsPreference to columnWidthsPreference
                if (columns.count { it.isNotEmpty() } == 0) {
                    throw Exception(context.getString(R.string.print_configuration_empty))
                }
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
                    columns,
                    columnWidths.toIntArray(),
                    colorSource,
                    itemDateFormat
                ) {
                    context.contentResolver.query(
                        uri,
                        projection,
                        "$KEY_PARENTID = ?",
                        arrayOf(it.toString()),
                        null
                    )!!
                }
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
        columns: List<List<Field>>,
        columnWidths: IntArray,
        colorSource: ColorSource,
        itemDateFormat: DateFormat?,
        splitCursor: (Long) -> Cursor,
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

        fun columnAlignment(fields: List<Field>) =
            if (fields.all { it is Amount || it is OriginalAmount })
                Element.ALIGN_RIGHT else Element.ALIGN_LEFT

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
                            ctx = context,
                            groupYear = transaction.year,
                            groupSecond = headerRow.second,
                            dateInfo = DateInfo.load(context.contentResolver),
                            weekStart = headerRow.weekStart,
                            weekRangeOnly = true,
                            relativeDay = false
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
                    "%s %s", if (amountMinor.sign > -1) "+" else "-",
                    currencyFormatter.convAmount(abs(amountMinor), currencyUnit)
                )
                if (filter == null) {
                    groupSummary.addCell(
                        helper.printToCell(
                            buildString {
                                append(context.getString(R.string.at_start))
                                append(": ")
                                append(
                                    currencyFormatter.formatMoney(
                                        headerRow.previousBalance
                                    )
                                )
                            }
                        )
                    )
                    groupSummary.addCell(helper.printToCell("Δ: $formattedDelta").apply {
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
                    groupSummary.addCell(
                        helper.printToCell(
                            buildString {
                                append(context.getString(R.string.at_end))
                                append(": ")
                                append(
                                    currencyFormatter.formatMoney(
                                        interimBalance
                                    )
                                )
                            }
                        ).apply {
                            horizontalAlignment = Element.ALIGN_RIGHT
                        })
                } else {
                    groupSummary.addCell(helper.printToCell("Δ: $formattedDelta").apply {
                        colspan = 3
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
                }
                groupSummary.addCell(
                    PdfPCell(
                        Phrase().apply {
                            addAll(
                                helper.print0(
                                    buildString {
                                        append(context.getString(R.string.sum_income))
                                        append(": + ")
                                        append(
                                            currencyFormatter.formatMoney(
                                                sumIncome
                                            )
                                        )
                                    }, FontType.INCOME
                                )
                            )
                            add(" | ")
                            addAll(
                                helper.print0(
                                    buildString {
                                        append(context.getString(R.string.sum_expenses))
                                        append(": - ")
                                        append(
                                            currencyFormatter.formatMoney(
                                                sumExpense.absolute()
                                            )
                                        )
                                    }, FontType.EXPENSE
                                )
                            )
                            add(" | ")
                            addAll(
                                helper.print0(
                                    buildString {
                                        append(context.getString(R.string.sum_transfer))
                                        append(": ")
                                        append(
                                            when (sumTransfer.amountMinor.sign) {
                                                1 -> "+ "
                                                -1 -> "- "
                                                else -> ""
                                            }
                                        )
                                        append(
                                            currencyFormatter.formatMoney(
                                                sumTransfer.absolute()
                                            )
                                        )
                                    },
                                    FontType.TRANSFER
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
                header2Table.addCell(
                    helper.printToCell(
                        "+ " + currencyFormatter.formatMoney(sumIncome),
                        FontType.INCOME
                    ).apply {
                        horizontalAlignment = Element.ALIGN_CENTER
                    })

                header2Table.addCell(
                    helper.printToCell(
                        "- " + currencyFormatter.formatMoney(sumExpense.negate()),
                        FontType.EXPENSE
                    ).apply {
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
                header2Table.addCell(
                    helper.printToCell(
                        Transfer.BI_ARROW + " " + currencyFormatter.formatMoney(sumTransfer),
                        FontType.TRANSFER
                    ).apply {
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
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

                table = helper.newTable(columns.size)

                // Header row
                val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)

                columns.forEachIndexed { index, fields ->
                    val border =
                        if (index == columns.lastIndex) NO_BORDER else Rectangle.RIGHT
                    val alignment = columnAlignment(fields)
                    val extra = Bundle(1).apply {
                        putBoolean(Date.KEY_IS_TIME_FIELD, account.grouping == Grouping.DAY)
                    }
                    if (fields.size == 1) {
                        table.addCell(
                            headerCell(
                                fields[0].toString(context, extra),
                                headerFont,
                                alignment,
                                border
                            )
                        )
                    } else {
                        table.addCell(
                            complexHeaderCell(
                                headerFont,
                                alignment,
                                border,
                                *fields.map { it.toString(context, extra) }.toTypedArray()
                            )
                        )
                    }
                }

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

            fun Transaction2.print(field: Field): List<String> {
                return when (field) {
                    is CombinedField -> listOf(field.fields.flatMap { print(it) }
                        .joinToString(" / "))

                    Amount -> listOf(
                        if (account.isHomeAggregate)
                            amount?.let {
                                currencyFormatter.formatMoney(
                                    if (transaction.type == FLAG_NEUTRAL) it.absolute() else it
                                )
                            } else null,
                        currencyFormatter.formatMoney(
                            if (transaction.type == FLAG_NEUTRAL) displayAmount.absolute() else displayAmount
                        )
                    )

                    OriginalAmount -> listOf(originalAmount?.let {
                        currencyFormatter.formatMoney(if (type == FLAG_NEUTRAL) it.absolute() else it)
                    })

                    Category -> listOf(categoryPath)
                    Date -> listOf(
                        if (isSplitPart) null else
                            itemDateFormat?.let {
                                Utils.convDateTime(_date, it)
                            }
                    )

                    Notes -> listOf(comment)
                    Payee -> listOf(party?.displayName)
                    Tags -> listOf(tagList.joinToString { it.second })

                    ReferenceNumber -> listOf(referenceNumber)
                    Account -> listOf(buildString {
                        if (account.id < 0 && !isSplitPart) {
                            //for aggregate accounts we need to indicate the account name
                            append(accountLabel)
                        }
                        if (transferPeer != null) {
                            if (isNotEmpty()) {
                                append(" ")
                            }
                            append(Transfer.getIndicatorPrefixForLabel(displayAmount.amountMinor) + transferAccountLabel)
                        }
                    })
                }
                    .filterNotNull()
                    .filter { it.isNotEmpty() }
            }


            fun Transaction2.print(paddingTop: Float = 5f, paddingBottom: Float = 5f, isSplitPart: Boolean = false) {
                columns.forEachIndexed { index, fields ->
                    val finalFields = if (isSplitPart) fields.filter { it != Payee } else fields
                    val border = (if (index == columns.lastIndex) 0 else Rectangle.RIGHT) +
                            if (isSplitPart) 0 else Rectangle.TOP
                    val rows: List<Pair<FontType, String>> = finalFields.flatMap { field ->
                        val fontType = when (field) {
                            is Amount, OriginalAmount -> {
                                if (account.id >= 0 || !isSameCurrency)
                                    (colorSource.transformType(type)
                                        ?: when (displayAmount.amountMinor.sign) {
                                            1 -> FLAG_INCOME
                                            -1 -> FLAG_EXPENSE
                                            else -> FLAG_NEUTRAL
                                        }).asFontType(isSplitPart) else FontType.NORMAL
                            }

                            else -> if (isSplitPart) FontType.SMALL else FontType.NORMAL
                        }

                        print(field).map { fontType to it }
                    }
                    val cell =
                        helper.printToNestedCell(*rows.mapIndexed { index, (fontType, text) ->
                            helper.printToCell(text, fontType, withPadding = false).also {
                                it.horizontalAlignment = columnAlignment(finalFields)
                            }.apply {
                                if (index != rows.lastIndex) {
                                    this.paddingBottom = 2f
                                }
                            }
                        }.toTypedArray()).apply {
                            this.border = border
                            paddingLeft = 5f
                            paddingRight = 5f
                            this.paddingTop = paddingTop
                            this.paddingBottom = paddingBottom
                        }

                    table!!.addCell(cell)
                    if (isVoid) {
                        cell.phrase
                            ?.chunks
                            ?.firstOrNull()
                            ?.setGenericTag(this@PdfPrinter.VOID_MARKER)
                    }
                }
            }

            transaction.print()

            if (DatabaseConstants.SPLIT_CATID == transaction.catId) {
                splitCursor(transaction.id).use { it ->
                    val list = it.asSequence.map {
                        Transaction2.fromCursor(
                            currencyContext,
                            it,
                            tagMap,
                            accountCurrency = account.currencyUnit
                        )
                    }.toList()
                    list.forEachIndexed { index, split ->
                        split.print(
                            paddingTop = 0f,
                            paddingBottom = if (index == list.lastIndex) 5f else 0f,
                            isSplitPart = true
                        )
                    }
                }
            }
        }

        // now add all this to the document
        document.add(table)
    }

    private fun addEmptyLine(paragraph: Paragraph) {
        paragraph.add(Paragraph(" "))
    }

    private fun Byte.asFontType(isSplitPart: Boolean) = when (this) {
        FLAG_INCOME -> if (isSplitPart) FontType.INCOME_SMALL else FontType.INCOME
        FLAG_EXPENSE -> if (isSplitPart) FontType.EXPENSE_SMALL else FontType.EXPENSE
        FLAG_TRANSFER -> if (isSplitPart) FontType.TRANSFER_SMALL else FontType.TRANSFER
        else -> if (isSplitPart) FontType.SMALL else FontType.NORMAL
    }

    private fun headerCell(
        text: String,
        font: Font,
        alignment: Int = Element.ALIGN_LEFT,
        border: Int = Rectangle.RIGHT,
        withPadding: Boolean = true,
    ) = PdfPCell(Phrase(text, font)).apply {
        setPadding(if (withPadding) 5f else 0f)
        horizontalAlignment = alignment
        verticalAlignment = Element.ALIGN_MIDDLE
        this.border = border
    }

    private fun complexHeaderCell(
        font: Font,
        alignment: Int = Element.ALIGN_LEFT,
        border: Int = Rectangle.RIGHT,
        vararg texts: String,
    ) = PdfPCell(PdfPTable(1).apply {
        widthPercentage = 100f
        texts.forEachIndexed { index, text ->
            addCell(
                headerCell(
                    text,
                    font,
                    alignment,
                    NO_BORDER,
                    withPadding = false
                ).apply {
                    if (index != texts.lastIndex) {
                        paddingBottom = 2f
                    }
                }
            )
        }
    }).apply {
        this.border = border
        setPadding(5f)
    }
}
