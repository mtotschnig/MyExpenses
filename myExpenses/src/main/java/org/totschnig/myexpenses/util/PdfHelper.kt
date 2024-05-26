package org.totschnig.myexpenses.util

import android.text.TextUtils
import android.view.View
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Font
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.totschnig.myexpenses.util.LazyFontSelector.FontType
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import java.io.File
import java.io.IOException
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

    private val layoutDirectionFromLocaleIsRTL: Boolean

    init {
        val l = Locale.getDefault()
        layoutDirectionFromLocaleIsRTL = (TextUtils.getLayoutDirectionFromLocale(l)
                == View.LAYOUT_DIRECTION_RTL)
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
        Font.FontFamily.TIMES_ROMAN,
        fontType.factor * baseFontSize,
        fontType.style,
        fontType.color
    )

    @Throws(DocumentException::class, IOException::class)
    fun printToCell(text: String, font: FontType): PdfPCell {
        val cell = PdfPCell(print(text, font))
        if (hasAnyRtl(text)) {
            cell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        }
        cell.border = Rectangle.NO_BORDER
        return cell
    }

    @Throws(DocumentException::class, IOException::class)
    fun print(text: String, font: FontType) = lfs?.process(text, font) ?: when (font) {
        FontType.BOLD -> Phrase(text, fBold)
        FontType.EXPENSE -> Phrase(text, fExpense)
        FontType.HEADER -> Phrase(text, fHeader)
        FontType.INCOME -> Phrase(text, fIncome)
        FontType.ITALIC -> Phrase(text, fItalic)
        FontType.NORMAL -> Phrase(text, fNormal)
        FontType.TITLE -> Phrase(text, fTitle)
        FontType.UNDERLINE -> Phrase(text, fUnderline)
    }

    fun emptyCell(): PdfPCell {
        val cell = PdfPCell()
        cell.border = Rectangle.NO_BORDER
        return cell
    }

    fun newTable(numColumns: Int): PdfPTable {
        val t = PdfPTable(numColumns)
        if (layoutDirectionFromLocaleIsRTL) {
            t.runDirection = PdfWriter.RUN_DIRECTION_RTL
        }
        return t
    }

    companion object {
        private val HAS_ANY_RTL_RE: Pattern = Pattern.compile(".*[\\p{InArabic}\\p{InHebrew}].*")

        @JvmStatic
        fun hasAnyRtl(str: String): Boolean {
            return HAS_ANY_RTL_RE.matcher(str).matches()
        }
    }
}
