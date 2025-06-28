package org.totschnig.myexpenses.util

import android.util.SparseArray
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Font
import com.itextpdf.text.Utilities
import com.itextpdf.text.error_messages.MessageLocalization
import com.itextpdf.text.pdf.BaseFont
import org.totschnig.myexpenses.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.IOException

const val SMALL_FACTOR = 0.85f
class LazyFontSelector(val files: Array<File>, private val baseSize: Float) {
    enum class FontType(val factor: Float, val style: Int, val color: BaseColor?) {
        NORMAL(1f, Font.NORMAL, null),
        SMALL(SMALL_FACTOR, Font.NORMAL, null),
        TITLE(1.5f, Font.BOLD, null),
        HEADER(1.2f, Font.UNDERLINE, null),
        BOLD(1f, Font.BOLD, null),
        ITALIC(1f, Font.ITALIC, null),
        UNDERLINE(1f, Font.UNDERLINE, null),
        INCOME(1f, Font.NORMAL, BaseColor(-0xff9800)), //#FF006800
        EXPENSE(1f, Font.NORMAL, BaseColor(-0x800000)), //#FF800000
        TRANSFER(1f, Font.NORMAL, BaseColor(-16777088)), //#FF000080
        INCOME_SMALL(SMALL_FACTOR, Font.NORMAL, BaseColor(-0xff9800)), //#FF006800
        EXPENSE_SMALL(SMALL_FACTOR, Font.NORMAL, BaseColor(-0x800000)), //#FF800000
        TRANSFER_SMALL(SMALL_FACTOR, Font.NORMAL, BaseColor(-16777088)), //#FF000080
        INCOME_BOLD(1f, Font.BOLD, BaseColor(-0xff9800)), //#FF006800
        EXPENSE_BOLD(1f, Font.BOLD, BaseColor(-0x800000)), //#FF800000
        BALANCE_CHAPTER(1.2f, Font.BOLD, null),
        BALANCE_SECTION(1f, Font.BOLD, null)
        ;

        private val fonts: SparseArray<Font> = SparseArray()

        fun addFont(index: Int, base: BaseFont, baseSize: Float): Font {
            val f = Font(base, baseSize * factor, style, color)
            fonts.put(index, f)
            return f
        }

        fun getFont(index: Int): Font? {
            return fonts.get(index)
        }

        companion object {
            fun clearCache() {
                entries.forEach {
                    it.clearCache()
                }
            }
        }

        fun clearCache() {
            fonts.clear()
        }
    }

    private var baseFonts: ArrayList<BaseFont> = ArrayList()
    private var currentFont: Font? = null

    /**
     * Process the text so that it will render with a combination of fonts if
     * needed.
     * @return a <CODE>Phrase</CODE> with one or more chunks
     */
    @Throws(DocumentException::class, IOException::class)
    fun process(text: String, type: FontType): List<Chunk> {
        if (files.isEmpty()) throw IndexOutOfBoundsException(
            MessageLocalization.getComposedMessage("no.font.is.defined")
        )
        val cc = text.toCharArray()
        val len = cc.size
        val sb = StringBuffer()
        return buildList {
            currentFont = null
            for (k in 0 until len) {
                val newChunk = processChar(cc, k, sb, type)
                if (newChunk != null) {
                    add(newChunk)
                }
            }
            if (sb.isNotEmpty()) {
                val ck = Chunk(
                    sb.toString(),
                    currentFont ?: getFont(0, type)
                )
                add(ck)
            }
        }
    }

    @Throws(DocumentException::class, IOException::class)
    fun processChar(cc: CharArray, k: Int, sb: StringBuffer, type: FontType): Chunk? {
        var start = k
        var newChunk: Chunk? = null
        val c = cc[start]
        if (c == '\n' || c == '\r') {
            sb.append(c)
        } else {
            var font: Font?
            if (Utilities.isSurrogatePair(cc, start)) {
                val u = Utilities.convertToUtf32(cc, start)
                for (f in files.indices) {
                    font = getFont(f, type)
                    if (font.baseFont.charExists(u)
                        || Character.getType(u) == Character.FORMAT.toInt()
                    ) {
                        if (currentFont !== font) {
                            if (sb.isNotEmpty() && currentFont != null) {
                                newChunk = Chunk(sb.toString(), currentFont)
                                sb.setLength(0)
                            }
                            currentFont = font
                        }
                        sb.append(c)
                        sb.append(cc[++start])
                        break
                    }
                }
            } else {
                var found = false
                for (f in files.indices) {
                    font = getFont(f, type)
                    if (font.baseFont.charExists(c.code)
                        || Character.getType(c) == Character.FORMAT.toInt()
                    ) {
                        if (currentFont !== font) {
                            if (sb.isNotEmpty() && currentFont != null) {
                                newChunk = Chunk(sb.toString(), currentFont)
                                sb.setLength(0)
                            }
                            currentFont = font
                        }
                        sb.append(c)
                        found = true
                        //Log.d("MyExpenses","Character " + c + " was found in font " + currentFont.getBaseFont().getPostscriptFontName());
                        break
                    }
                }
                if (!found && BuildConfig.DEBUG) {
                    Timber.d("Character %c was not found in any fonts", c)
                }
            }
        }
        return newChunk
    }

    @Throws(DocumentException::class, IOException::class)
    private fun getBaseFont(index: Int): BaseFont {
        if (baseFonts.size < index + 1) {
            var file = files[index].absolutePath
            if (file.endsWith("ttc")) {
                file += ",0"
            }
            Timber.i("now loading font file %s", file)
            val bf = BaseFont.createFont(
                file, BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
            )
            baseFonts.add(bf)
            return bf
        } else {
            return baseFonts[index]
        }
    }

    @Throws(DocumentException::class, IOException::class)
    private fun getFont(index: Int, type: FontType) =
        type.getFont(index) ?: type.addFont(index, getBaseFont(index), baseSize)
}
