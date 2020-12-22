package org.totschnig.tesseract

import Catalano.Imaging.FastBitmap
import Catalano.Imaging.Filters.BradleyLocalThreshold
import Catalano.Imaging.IApplyInPlace
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.feature.getLocaleForUserCountry
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.TesseractEngine
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import timber.log.Timber
import java.io.File
import java.util.*

const val TESSERACT_DOWNLOAD_FOLDER = "tesseract4/fast/"

@Keep
object Engine : TesseractEngine {
    private var timer: Long = 0
    private fun initialize() {
        System.loadLibrary("jpeg")
        System.loadLibrary("png")
        System.loadLibrary("leptonica")
        System.loadLibrary("tesseract")
    }

    private fun language(context: Context, prefHandler: PrefHandler) =
            prefHandler.getString(PrefKey.TESSERACT_LANGUAGE, null)
                    ?: defaultLanguage(context)

    private fun defaultLanguage(context: Context): String {
        val default = getLocaleForUserCountry(context)
        val language = default.isO3Language
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (language == "aze" || language == "uzb") {
                if (default.script == "Cyrl") {
                    return language + "_cyrl"
                }
            }
            if (language == "srp") {
                if (default.script == "Latn") {
                    return language + "_latn"
                }
            }
        }
        if (language == "zho") {
            val script = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
                    && default.script == "Hans") "sim" else "tra"
            return "chi_${script}"
        }
        return language.takeIf { context.resources.getStringArray(R.array.pref_tesseract_language_values).indexOf(it) > -1 } ?: "eng"
    }

    override fun getLanguageArray(context: Context) =
            context.resources.getStringArray(R.array.pref_tesseract_language_values)
                    .map { getTesseractLanguageDisplayName(context, it)}
                    .toTypedArray()

    private fun getTesseractLanguageDisplayName(context: Context, localeString: String): String {
        val localeParts = localeString.split("_")
        val lang = when (localeParts[0]) {
            "kmr" -> "kur"
            else -> localeParts[0]
        }
        val localeFromContext = Utils.localeFromContext(context)
        return if (localeParts.size == 2) {
            val script = when (localeParts[1]) {
                "sim" -> "Hans"
                "tra" -> "Hant"
                else -> localeParts[1]
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Locale.Builder().setLanguage(lang).setScript(script).build().getDisplayName(localeFromContext)
            } else {
                "${Locale(lang).getDisplayName(localeFromContext)} ($script)"
            }
        } else
            Locale(lang).getDisplayName(localeFromContext)
    }

    override fun tessDataExists(context: Context, prefHandler: PrefHandler) =
            File(context.getExternalFilesDir(null), filePath(language(context, prefHandler))).exists()

    override fun offerTessDataDownload(baseActivity: BaseActivity) {
        val language = language(baseActivity, baseActivity.prefHandler)
        if (language != baseActivity.downloadPending) {
            ConfirmationDialogFragment.newInstance(Bundle().apply {
                putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.button_download)
                putString(ConfirmationDialogFragment.KEY_MESSAGE,
                        baseActivity.getString(R.string.tesseract_download_confirmation,
                                getTesseractLanguageDisplayName(baseActivity, language)))
                putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.TESSERACT_DOWNLOAD_COMMAND)
            }).show(baseActivity.supportFragmentManager, "DOWNLOAD_TESSDATA")
        }
    }

    private fun filePath(language: String) = "${TESSERACT_DOWNLOAD_FOLDER}tessdata/${language}.traineddata"

    private fun fileName(language: String) = "${language}.traineddata"

    override fun downloadTessData(context: Context, prefHandler: PrefHandler): String {
        val language = language(context, prefHandler)
        val uri = Uri.parse("https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/${fileName(language)}")
        ContextCompat.getSystemService(context, DownloadManager::class.java)?.enqueue(DownloadManager.Request(uri)
                .setTitle(context.getString(R.string.pref_tesseract_language_title))
                .setDescription(language)
                .setDestinationInExternalFilesDir(context, null, filePath(language)))
        return getTesseractLanguageDisplayName(context, language)
    }

    override suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text =
            withContext(Dispatchers.Default) {
                initialize()
                with(TessBaseAPI()) {
                    timer = System.currentTimeMillis()
                    if (!init(File(context.getExternalFilesDir(null), TESSERACT_DOWNLOAD_FOLDER).path, language(context, prefHandler))) {
                        throw IllegalStateException("Could not init Tesseract")
                    }
                    timing("Init")
                    setVariable("tessedit_do_invert", TessBaseAPI.VAR_FALSE)
                    setVariable("load_system_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_freq_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_punc_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_number_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_unambig_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_bigram_dawg", TessBaseAPI.VAR_FALSE)
                    setVariable("load_fixed_length_dawgs", TessBaseAPI.VAR_FALSE)
                    pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
                    var bitmap = with(FastBitmap(file.path)) {
                        toGrayscale()
                        val g: IApplyInPlace = BradleyLocalThreshold()
                        g.applyInPlace(this)
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

    override fun info(context: Context, prefHandler: PrefHandler): CharSequence {
        return "Tesseract (${language(context, prefHandler)})"
    }

    private fun timing(step: String) {
        val delta = System.currentTimeMillis() - timer
        Timber.i("Timing (%s): %d", step, delta)
        timer = System.currentTimeMillis()
    }
}
