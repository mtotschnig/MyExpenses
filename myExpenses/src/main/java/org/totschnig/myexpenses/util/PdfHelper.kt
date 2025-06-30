package org.totschnig.myexpenses.util

import android.content.Context
import android.view.View
import androidx.core.text.layoutDirection
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPageEventHelper
import com.itextpdf.text.pdf.PdfWriter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.CENTER
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.LEFT
import org.totschnig.myexpenses.export.pdf.PdfPrinter.HorizontalPosition.RIGHT
import org.totschnig.myexpenses.export.pdf.PdfPrinter.VerticalPosition
import org.totschnig.myexpenses.export.pdf.PdfPrinter.VerticalPosition.BOTTOM
import org.totschnig.myexpenses.export.pdf.PdfPrinter.VerticalPosition.TOP
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Arrays
import java.util.Locale
import java.util.regex.Pattern

class PdfHelper(private val baseFontSize: Float, memoryClass: Int) {
    private var lfs: LazyFontSelector?
    private val fNormal: Font by lazy { convertFallback(FontType.NORMAL) }
    private val fTitle: Font by lazy { convertFallback(FontType.TITLE) }
    private val fHeader: Font by lazy { convertFallback(FontType.HEADER) }
    private val fBold: Font by lazy { convertFallback(FontType.BOLD) }
    private val fItalic: Font by lazy { convertFallback(FontType.ITALIC) }
    private val fUnderline: Font by lazy { convertFallback(FontType.UNDERLINE) }
    private val fIncome: Font by lazy { convertFallback(FontType.INCOME) }
    private val fExpense: Font by lazy { convertFallback(FontType.EXPENSE) }
    private val fTransfer: Font by lazy { convertFallback(FontType.TRANSFER) }
    private val fIncomeBold: Font by lazy { convertFallback(FontType.INCOME_BOLD) }
    private val fExpenseBold: Font by lazy { convertFallback(FontType.EXPENSE_BOLD) }
    private val fSmall: Font by lazy { convertFallback(FontType.SMALL) }
    private val fIncomeSmall: Font by lazy { convertFallback(FontType.INCOME_SMALL) }
    private val fExpenseSmall: Font by lazy { convertFallback(FontType.EXPENSE_SMALL) }
    private val fTransferSmall: Font by lazy { convertFallback(FontType.TRANSFER_SMALL) }
    private val fBalanceChapter: Font by lazy { convertFallback(FontType.BALANCE_CHAPTER) }
    private val fBalanceSection: Font by lazy { convertFallback(FontType.BALANCE_SECTION) }

    private val layoutDirectionFromLocaleIsRTL: Boolean

    init {
        val l = Locale.getDefault()
        layoutDirectionFromLocaleIsRTL = (l.layoutDirection == View.LAYOUT_DIRECTION_RTL)
        lfs = if (memoryClass >= 32) {
            //we want the Default Font to be used first
            try {
                val dir = File("/system/fonts")
                val files = dir.listFiles { _: File?, filename: String ->
                    //NotoSans*-Regular.otf files found not to work:
                    (filename.endsWith("ttf") || filename.endsWith("ttc"))
                            //BaseFont.charExists finds chars that are not visible in PDF
                            //NotoColorEmoji.ttf and SamsungColorEmoji.ttf are known not to work
                            && !filename.contains("ColorEmoji")
                            //vivo devices report 43653f0a14cf41b707579c51642b7046
                            && !filename.startsWith("NEX-")
                            //seen on Pixel with Android 15 Beta
                            && filename != "NotoSansCJK-Regular.ttc"
                            //cannot be embedded due to licensing restrictions: report 55cdc91d2279b63b23419bc9cec1a21d
                            && filename != "Kindle_Symbol.ttf"

                }
                Arrays.sort(files!!) { f1: File, f2: File ->
                    val n1 = f1.name
                    val n2 = f2.name
                    if (n1 == "DroidSans.ttf") {
                        return@sort -1
                    } else if (n2 == "DroidSans.ttf") {
                        return@sort 1
                    }
                    if (n1.startsWith("Droid")) {
                        if (n2.startsWith("Droid")) {
                            return@sort n1.compareTo(n2)
                        } else {
                            return@sort -1
                        }
                    } else if (n2.startsWith("Droid")) {
                        return@sort 1
                    }
                    n1.compareTo(n2)
                }
                LazyFontSelector(files, baseFontSize)
            } catch (e: Exception) {
                report(e)
                null
            }
        } else null
    }

    private fun convertFallback(fontType: FontType) = Font(
        Font.FontFamily.HELVETICA,
        fontType.factor * baseFontSize,
        fontType.style,
        fontType.color
    )

    @Throws(DocumentException::class, IOException::class)
    fun printToCell(
        text: String?,
        font: FontType = FontType.NORMAL,
        border: Int = Rectangle.NO_BORDER,
        withPadding: Boolean = true,
    ) = if (text == null) emptyCell(border) else
        PdfPCell(print(text, font)).apply {
            if (hasAnyRtl(text)) {
                runDirection = PdfWriter.RUN_DIRECTION_RTL
            }
            setPadding(if (withPadding) 5f else 0f)
            this.border = border
            verticalAlignment = Element.ALIGN_MIDDLE
        }

    @Throws(DocumentException::class, IOException::class)
    fun printToCell(
        phrase: Phrase,
        border: Int = Rectangle.NO_BORDER,
        withPadding: Boolean = true,
    ) = PdfPCell(phrase).apply {
        if (hasAnyRtl(phrase.content)) {
            this.runDirection = PdfWriter.RUN_DIRECTION_RTL
        }
        setPadding(if (withPadding) 5f else 0f)
        this.border = border
    }

    fun List<Chunk>.join() = Phrase().also { phrase ->
        forEach { phrase.add(it) }
    }

    /**
     * variant of [print] that returns list of Chunks instead of Phrase
     */
    @Throws(DocumentException::class, IOException::class)
    fun print0(text: String, font: FontType): List<Chunk> = lfs?.process(text, font) ?: listOf(
        when (font) {
            FontType.BOLD -> Chunk(text, fBold)
            FontType.EXPENSE -> Chunk(text, fExpense)
            FontType.HEADER -> Chunk(text, fHeader)
            FontType.INCOME -> Chunk(text, fIncome)
            FontType.ITALIC -> Chunk(text, fItalic)
            FontType.NORMAL -> Chunk(text, fNormal)
            FontType.TITLE -> Chunk(text, fTitle)
            FontType.UNDERLINE -> Chunk(text, fUnderline)
            FontType.INCOME_BOLD -> Chunk(text, fIncomeBold)
            FontType.EXPENSE_BOLD -> Chunk(text, fExpenseBold)
            FontType.TRANSFER -> Chunk(text, fTransfer)
            FontType.SMALL -> Chunk(text, fSmall)
            FontType.INCOME_SMALL -> Chunk(text, fIncomeSmall)
            FontType.EXPENSE_SMALL -> Chunk(text, fExpenseSmall)
            FontType.TRANSFER_SMALL -> Chunk(text, fTransferSmall)
            FontType.BALANCE_SECTION -> Chunk(text, fBalanceSection)
            FontType.BALANCE_CHAPTER -> Chunk(text, fBalanceChapter)
        }
    )

    @Throws(DocumentException::class, IOException::class)
    fun print(text: String, font: FontType): Phrase =
        lfs?.process(text, font)?.join() ?: when (font) {
            FontType.BOLD -> Phrase(text, fBold)
            FontType.EXPENSE -> Phrase(text, fExpense)
            FontType.HEADER -> Phrase(text, fHeader)
            FontType.INCOME -> Phrase(text, fIncome)
            FontType.ITALIC -> Phrase(text, fItalic)
            FontType.NORMAL -> Phrase(text, fNormal)
            FontType.TITLE -> Phrase(text, fTitle)
            FontType.UNDERLINE -> Phrase(text, fUnderline)
            FontType.INCOME_BOLD -> Phrase(text, fIncomeBold)
            FontType.EXPENSE_BOLD -> Phrase(text, fExpenseBold)
            FontType.TRANSFER -> Phrase(text, fTransfer)
            FontType.SMALL -> Phrase(text, fSmall)
            FontType.INCOME_SMALL -> Phrase(text, fIncomeSmall)
            FontType.EXPENSE_SMALL -> Phrase(text, fExpenseSmall)
            FontType.TRANSFER_SMALL -> Phrase(text, fTransferSmall)
            FontType.BALANCE_SECTION -> Phrase(text, fBalanceSection)
            FontType.BALANCE_CHAPTER -> Phrase(text, fBalanceChapter)
        }

    fun emptyCell(border: Int = Rectangle.NO_BORDER) = PdfPCell().apply {
        this.border = border
    }

    fun newTable(numColumns: Int) = PdfPTable(numColumns).apply {
        if (layoutDirectionFromLocaleIsRTL) {
            this.runDirection = PdfWriter.RUN_DIRECTION_RTL
        }
    }

    fun printToNestedCell(
        vararg cells: PdfPCell,
    ): PdfPCell {
        return when (cells.size) {
            0 -> PdfPCell()
            1 -> cells.first()
            else -> {
                PdfPCell(PdfPTable(1).apply {
                    this.widthPercentage = 100f
                    cells.forEach {
                        addCell(it)
                    }
                })
            }
        }
    }

    fun getPageEventHelper(
        context: Context,
        prefHandler: PrefHandler
    ) = object : PdfPageEventHelper() {

        override fun onEndPage(writer: PdfWriter, document: Document) {
            val cb = writer.getDirectContent()
            fun print(
                cb: PdfContentByte,
                content: PrefKey,
                horizontalPosition: HorizontalPosition,
                verticalPosition: VerticalPosition,
            ) {
                prefHandler.getString(content)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
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
                            TOP -> document.top() + minOf(baseFontSize, 10f)
                            BOTTOM -> document.bottom() - baseFontSize - minOf(
                                baseFontSize,
                                10f
                            )
                        }
                        val alignment = when (horizontalPosition) {
                            LEFT -> Element.ALIGN_LEFT
                            CENTER -> Element.ALIGN_CENTER
                            RIGHT -> Element.ALIGN_RIGHT
                        }
                        ColumnText.showTextAligned(
                            cb, alignment, print(text, FontType.NORMAL), x, y, 0F
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

    companion object {
        private val HAS_ANY_RTL_RE: Pattern = Pattern.compile(".*[\\p{InArabic}\\p{InHebrew}].*")

        @JvmStatic
        fun hasAnyRtl(str: String): Boolean {
            return HAS_ANY_RTL_RE.matcher(str).matches()
        }
    }
}
