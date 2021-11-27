package org.totschnig.mlkit

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.getLocaleForUserCountry
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Keep
object Engine : org.totschnig.ocr.MlkitEngine {
    private var initialized: Boolean = false
    private fun initialize(context: Context) {
        if (!initialized) {
            MlKit.initialize(context)
            initialized = true
        }
    }

    enum class Script {
        Latn, Han, Deva, Jpan, Kore
    }

    private fun getOptions(script: Script) =
        try {
                (Class.forName("org.totschnig.mlkit_${script.name}.Options").kotlin.objectInstance as RecognizerProvider).textRecognizerOptions
            } catch (e: Exception) {
                throw java.lang.IllegalStateException("Recognizer for ${script.name} not found")
            }

    private fun options(
        context: Context,
        prefHandler: PrefHandler
    ): TextRecognizerOptionsInterface = getOptions(script(context, prefHandler))

    private fun script(context: Context, prefHandler: PrefHandler) =
        prefHandler.getString(PrefKey.MLKIT_SCRIPT, null)?.let {
            try {
                Script.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: defaultScript(context)

    private fun defaultScript(context: Context) =
        when (getLocaleForUserCountry(context).language) {
            "zh" -> Script.Han
            "hi", "mr", "ne" -> Script.Deva
            "ja" -> Script.Jpan
            "ko" -> Script.Kore
            else -> Script.Latn
        }

    override fun getScriptArray(context: Context) =
        context.resources.getStringArray(R.array.pref_mlkit_script_values)
            .map { getMlkitScriptDisplayName(context, it) }
            .toTypedArray()

    private fun getMlkitScriptDisplayName(context: Context, script: String) =
        when(script) {
            "Han" -> Locale.CHINESE.displayLanguage
            else -> Locale.Builder().setScript(script).build().getDisplayScript(Utils.localeFromContext(context))
        }

    override suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text =
        withContext(Dispatchers.Default) {
            initialize(context)
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            TextRecognition.getClient(options(context, prefHandler)).use { recognizer ->
                suspendCoroutine { cont ->
                    recognizer.process(image)
                        .addOnSuccessListener { texts ->
                            cont.resume(texts.wrap())
                        }
                        .addOnFailureListener { e ->
                            cont.resumeWithException(e as Throwable)
                        }
                }
            }
        }

    override fun info(context: Context, prefHandler: PrefHandler): CharSequence {
        return "Ml Kit"
    }
}

fun com.google.mlkit.vision.text.Text.wrap() = Text(textBlocks.map { textBlock ->
    TextBlock(textBlock.lines.map { line ->
        Line(line.text, line.boundingBox, line.elements.map { element ->
            Element(element.text, element.boundingBox)
        })
    })
})
