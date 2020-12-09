package org.totschnig.ocr

import Catalano.Imaging.FastBitmap
import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel.RIL_WORD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import javax.inject.Inject

class OcrFeatureImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context) : AbstractOcrFeatureImpl(prefHandler, userLocaleProvider, context) {
    val scale: Int
    init {
        scale = prefHandler.getInt("ocr_scale_down_factor", 1)
    }
    var timer: Long = 0
    override suspend fun runTextRecognition(file: File, context: Context): OcrResult =
            withContext(Dispatchers.Default) {
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
                    var bitmap = with(FastBitmap(file.path))  {
                        /*val g: IApplyInPlace = BradleyLocalThreshold()
                        g.applyInPlace(this)*/
                        toBitmap()
                    }
                    if (scale < 10) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale / 10), (bitmap.height * scale / 10), true)
                    }
                    setImage(bitmap)
                    timing("SetImage")
                    utF8Text
                    timing("utF8Text")
                    val lines = mutableListOf<Line>()
                    with(resultIterator) {
                        begin()
                        do {
                            val lineText = getUTF8Text(RIL_TEXTLINE)
                            val lineBoundingRect = getBoundingRect(RIL_TEXTLINE)
                            val elements = mutableListOf<Element>()
                            do {
                                val wordText = getUTF8Text(RIL_WORD)
                                val wordBoundingRect = getBoundingRect(RIL_WORD)
                                elements.add(Element(wordText, wordBoundingRect))
                            } while (!isAtFinalElement(RIL_TEXTLINE, RIL_WORD) && next(RIL_WORD))
                            lines.add(Line(lineText, lineBoundingRect, elements))
                        } while (next(RIL_TEXTLINE))
                        delete()
                    }
                    timing("resultIterator")
                    end()
                    timing("end")
                    val processTextRecognitionResult = processTextRecognitionResult(Text(listOf(TextBlock(lines))), queryPayees())
                    timing("processTextRecognitionResult")
                    processTextRecognitionResult
                }
            }

    fun timing(step: String) {
        val delta = System.currentTimeMillis() - timer
        log("Timing (%s): %d", step, delta)
        timer = System.currentTimeMillis()
    }
}