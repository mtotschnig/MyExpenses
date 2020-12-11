package org.totschnig.tesseract

import Catalano.Imaging.FastBitmap
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.viewmodel.TessdataMissingException
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.TesseractEngine
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import timber.log.Timber
import java.io.File

const val TESSERACT_DOWNLOAD_FOLDER = "tesseract4/fast/"

@Keep
object Engine : TesseractEngine {
    var timer: Long = 0
    fun initialize() {
        System.loadLibrary("jpeg")
        System.loadLibrary("png")
        System.loadLibrary("leptonica")
        System.loadLibrary("tesseract")
    }

    override fun tessDataExists(context: Context, language: String) =
            File(context.getExternalFilesDir(null), filePath(language)).exists()

    fun filePath(language: String) = "${TESSERACT_DOWNLOAD_FOLDER}tessdata/%s.traineddata".format(language)

    private fun fileName(language: String) = "%s.traineddata".format(language)

    override fun downloadTessData(context: Context, language: String) {
        val uri = Uri.parse("https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/%s".format(fileName(language)))
        ContextCompat.getSystemService(context, DownloadManager::class.java)?.enqueue(DownloadManager.Request(uri)
                .setTitle(context.getString(R.string.pref_tesseract_language_title))
                .setDescription(language)
                .setDestinationInExternalFilesDir(context, null, filePath(language)))
    }

    override suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text =
            withContext(Dispatchers.Default) {
                initialize()
                with(TessBaseAPI()) {
                    timer = System.currentTimeMillis()
                    val language = prefHandler.getString(PrefKey.TESSERACT_LANGUAGE, "eng")!!
                    if (!init(File(context.getExternalFilesDir(null), TESSERACT_DOWNLOAD_FOLDER).path, language)) {
                        throw if (tessDataExists(context, language)) {
                            IllegalStateException("Could not init Tesseract")
                        } else {
                            TessdataMissingException(language)
                        }
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
