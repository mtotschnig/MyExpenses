package org.totschnig.tesseract

import Catalano.Imaging.FastBitmap
import android.content.Context
import androidx.annotation.Keep
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import timber.log.Timber
import java.io.File

@Keep
object Engine : org.totschnig.ocr.Engine {
    var timer: Long = 0
    fun initialize(context: Context) {
        System.loadLibrary("jpeg")
        System.loadLibrary("png")
        System.loadLibrary("leptonica")
        System.loadLibrary("tesseract")
    }

    override suspend fun run(file: File, context: Context): Text =
            withContext(Dispatchers.Default) {
                initialize(context)
                with(TessBaseAPI()) {
                    timer = System.currentTimeMillis()
                    if (!init("/sdcard/tesseract4/fast/", "bul")) {
                        throw IllegalStateException("Could not init Tesseract")
                    }
                    timing("Init")
                    setVariable("tessedit_do_invert", TessBaseAPI.VAR_FALSE)
                    setVariable("load_system_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_freq_dawg", TessBaseAPI.VAR_FALSE)
                    pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
                    var bitmap = with(FastBitmap(file.path)) {
                        /*val g: IApplyInPlace = BradleyLocalThreshold()
                        g.applyInPlace(this)*/
                        toBitmap()
                    }
                    /*if (scale < 10) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale / 10), (bitmap.height * scale / 10), true)
                    }*/
                    setImage(bitmap)
                    timing("SetImage")
                    utF8Text
                    timing("utF8Text")
                    val lines = mutableListOf<Line>()
                    with(resultIterator) {
                        begin()
                        do {
                            val lineText = getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                            val lineBoundingRect = getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                            val elements = mutableListOf<Element>()
                            do {
                                val wordText = getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                                val wordBoundingRect = getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                                elements.add(Element(wordText, wordBoundingRect))
                            } while (!isAtFinalElement(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE, TessBaseAPI.PageIteratorLevel.RIL_WORD) && next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
                            lines.add(Line(lineText, lineBoundingRect, elements))
                        } while (next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                        delete()
                    }
                    timing("resultIterator")
                    end()
                    timing("end")
                    Text(listOf(TextBlock(lines)))
                }
            }

    fun timing(step: String) {
        val delta = System.currentTimeMillis() - timer
        Timber.i("Timing (%s): %d", step, delta)
        timer = System.currentTimeMillis()
    }
}