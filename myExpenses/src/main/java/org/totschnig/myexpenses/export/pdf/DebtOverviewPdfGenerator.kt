package org.totschnig.myexpenses.export.pdf

// Import BaseColor for colors
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.AppDirHelper.timeStampedFile
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.PdfHelper
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt


class DebtOverviewPdfGenerator(private val context: Context) {

    val currencyFormatter = context.injector.currencyFormatter()
    val homeCurrencyUnit = context.injector.currencyContext().homeCurrencyUnit
    val prefHandler = context.injector.prefHandler()
    val baseFontSize = prefHandler.getFloat(PrefKey.PRINT_FONT_SIZE, 12f)
    val helper = PdfHelper(
        baseFontSize,
        context.myApplication.memoryClass
    )

    fun generatePdf(
        destDir: DocumentFile,
        groups: Collection<List<DisplayDebt>>,
    ): Pair<Uri, String> {

        val fileName = "DebtOverview"
        val outputFile = timeStampedFile(
            destDir,
            fileName,
            "application/pdf", "pdf"
        ) ?: throw createFileFailure(context, destDir, fileName)


        val document = PdfPrinter.getDocument(context, prefHandler)

        PdfWriter.getInstance(
            document, context.contentResolver.openOutputStream(outputFile.uri)
        ).pageEvent = helper.getPageEventHelper(context, prefHandler)
        document.open()

        try {
            addTitle(document)

            val table = PdfPTable(2)
            table.setWidthPercentage(100f)
            table.setWidths(floatArrayOf(3f, 1f))
            table.spacingAfter = 10f

            groups.forEach { debts ->

                addLine(
                    table = table,
                    label = debts.first().payeeName,
                    amount = debts.sumOf { it.currentEquivalentBalance },
                    labelFontType = FontType.BALANCE_SECTION,
                    expenseFontType = FontType.EXPENSE_BOLD,
                    incomeFontType = FontType.INCOME_BOLD
                )

                debts.forEach { debt ->
                    addLine(
                        table,
                        debt.label,
                        debt.currentEquivalentBalance,
                        paddingTop = 1f,
                        paddingBottom = 4f
                    )
                }
                addLineSeparator(table)
            }
            addLine(
                table = table,
                label = context.getString(R.string.menu_aggregates),
                amount = groups.flatten().sumOf { it.currentEquivalentBalance },
                labelFontType = FontType.BALANCE_CHAPTER,
                expenseFontType = FontType.EXPENSE_BOLD,
                incomeFontType = FontType.INCOME_BOLD
            )
            document.add(table)
        } finally {
            document.close()
        }
        return outputFile.uri to outputFile.displayName
    }

    private fun addLine(
        table: PdfPTable,
        label: String?,
        amount: Long,
        labelFontType: FontType = FontType.NORMAL,
        expenseFontType: FontType = FontType.EXPENSE,
        incomeFontType: FontType = FontType.INCOME,
        paddingTop: Float = 10f,
        paddingBottom: Float = 10f
    ) {

        var cell = if (label == null) helper.emptyCell() else
            helper.printToCell(text = label, font = labelFontType, withPadding = false)
        cell.horizontalAlignment = Element.ALIGN_LEFT
        cell.border = Rectangle.NO_BORDER
        cell.paddingTop = paddingTop
        cell.paddingBottom = paddingBottom
        table.addCell(cell)

        cell = helper.printToCell(
            text = formatCurrency(amount, homeCurrencyUnit),
            font = if (amount < 0) expenseFontType else incomeFontType,
            withPadding = false,
        )
        cell.horizontalAlignment = Element.ALIGN_RIGHT
        cell.border = Rectangle.NO_BORDER
        cell.paddingTop = paddingTop
        cell.paddingBottom = paddingBottom
        table.addCell(cell)
    }

    private fun addLineSeparator(table: PdfPTable) {
        table.addCell(PdfPCell().apply {
            colspan = 2
            paddingTop = 0f
            paddingBottom = 0f
            paddingLeft = 0f
            paddingRight = 0f
            border = Rectangle.NO_BORDER
            addElement(Chunk(LineSeparator()))
        })
    }

    private fun addTitle(document: Document) {
        document.add(
            Paragraph(
                helper.print(
                    context.getString(R.string.title_activity_debt_overview),
                    FontType.TITLE
                )
            ).apply {
                this.alignment = Element.ALIGN_CENTER
            })

    }

    private fun formatCurrency(
        amount: Long,
        currency: CurrencyUnit = homeCurrencyUnit
    ): String {
        return currencyFormatter.formatMoney(Money(currency, amount))
    }
}