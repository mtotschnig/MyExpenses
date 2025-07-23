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
import org.totschnig.myexpenses.viewmodel.data.BalanceData
import org.totschnig.myexpenses.viewmodel.data.BalanceSection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs


class BalanceSheetPdfGenerator(private val context: Context) {

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
        data: BalanceData,
        debts: Long,
        date: LocalDate
    ): Pair<Uri, String> {

        val fileName = "BalanceSheet"
        val outputFile = timeStampedFile(
            destDir,
            fileName,
            "application/pdf", "pdf"
        ) ?: throw createFileFailure(context, destDir, fileName)

        val (assets, totalAssets, liabilities, totalLiabilities) = data

        val document = PdfPrinter.getDocument(context, prefHandler)

        PdfWriter.getInstance(
            document, context.contentResolver.openOutputStream(outputFile.uri)
        ).pageEvent = helper.getPageEventHelper(context, prefHandler)
        document.open()

        try {
            addTitleAndDate(document, date)

            val table = PdfPTable(2)
            table.setWidthPercentage(100f)
            table.setWidths(floatArrayOf(3f, 1f))

            addLine(
                table,
                context.getString(R.string.balance_sheet_section_assets),
                totalAssets
            )
            for (section in assets) {
                addAccountTypeSectionToPdf(table, section)
            }

            addLineSeparator(table)

            addLine(
                table,
                context.getString(R.string.balance_sheet_section_liabilities),
                totalLiabilities
            )
            for (section in liabilities) {
                addAccountTypeSectionToPdf(table, section)
            }

            addLineSeparator(table)

            if (debts != 0L) {
                addLine(
                    table,
                    context.getString(R.string.debts),
                    debts,
                    isAbsolute = false,
                    paddingBottom = 0f
                )
                addLineSeparator(table)
            }

            addLine(
                table,
                context.getString(R.string.balance_sheet_net_worth),
                totalAssets + totalLiabilities + debts,
                isAbsolute = false,
                paddingBottom = 0f
            )
            document.add(table)
        } finally {
            document.close()
        }
        return outputFile.uri to outputFile.displayName
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

    private fun addTitleAndDate(document: Document, date: LocalDate) {
        var paragraph =
            Paragraph(helper.print(context.getString(R.string.balance_sheet), FontType.TITLE))
        paragraph.alignment = Element.ALIGN_CENTER
        document.add(paragraph)

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        paragraph = Paragraph(helper.print(date.format(formatter), FontType.NORMAL))
        paragraph.alignment = Element.ALIGN_CENTER
        paragraph.spacingAfter = 10f
        document.add(paragraph)
    }

    private fun addAccountTypeSectionToPdf(
        table: PdfPTable,
        section: BalanceSection
    ) {
        addLine(
            table = table,
            label = section.type.localizedName(context),
            amount = section.total,
            isAbsolute = true,
            labelFontType = FontType.BALANCE_SECTION
        )

        for (account in section.accounts) {
            val printreal =
                account.currency.code != homeCurrencyUnit.code && account.currentBalance != 0L
            addLine(
                table = table,
                label = account.label,
                amount = account.equivalentCurrentBalance,
                isAbsolute = true,
                labelFontType = FontType.NORMAL,
                expenseFontType = FontType.EXPENSE,
                incomeFontType = FontType.INCOME,
                paddingTop = 4f,
                paddingBottom = if (printreal) 1f else 4f
            )

            if (printreal) {
                addLine(
                    table = table,
                    label = null,
                    amount = account.currentBalance,
                    currency = account.currency,
                    isAbsolute = true,
                    labelFontType = FontType.NORMAL,
                    expenseFontType = FontType.EXPENSE_SMALL,
                    incomeFontType = FontType.INCOME_SMALL,
                    paddingTop = 1f,
                    paddingBottom = 4f
                )
            }
        }
    }

    private fun addLine(
        table: PdfPTable,
        label: String?,
        amount: Long,
        currency: CurrencyUnit = homeCurrencyUnit,
        isAbsolute: Boolean = true,
        labelFontType: FontType = FontType.BALANCE_CHAPTER,
        expenseFontType: FontType = FontType.EXPENSE_BOLD,
        incomeFontType: FontType = FontType.INCOME_BOLD,
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
            text = formatCurrency(amount, isAbsolute, currency),
            font = if (amount < 0) expenseFontType else incomeFontType,
            withPadding = false,
        )
        cell.horizontalAlignment = Element.ALIGN_RIGHT
        cell.border = Rectangle.NO_BORDER
        cell.paddingTop = paddingTop
        cell.paddingBottom = paddingBottom
        table.addCell(cell)
    }

    private fun formatCurrency(
        amount: Long,
        absolute: Boolean,
        currency: CurrencyUnit = homeCurrencyUnit
    ): String {
        val value = if (absolute) abs(amount) else amount
        return currencyFormatter.formatMoney(Money(currency, value))
    }
}