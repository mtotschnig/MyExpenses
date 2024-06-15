package org.totschnig.mlkit

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.Keep
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.Script
import org.totschnig.myexpenses.feature.getUserConfiguredMlkitScript
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.isDebugAsset
import org.totschnig.myexpenses.util.getDisplayNameForScript
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import java.util.Locale
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

    private fun getOptions(script: Script) =
        try {
            (Class.forName("org.totschnig.mlkit_${script.name.lowercase(Locale.ROOT)}.Options").kotlin.objectInstance as RecognizerProvider).textRecognizerOptions
        } catch (e: Exception) {
            throw java.lang.IllegalStateException("Recognizer for ${script.name} not found")
        }

    private fun options(
        context: Context,
        prefHandler: PrefHandler
    ): TextRecognizerOptionsInterface =
        getOptions(getUserConfiguredMlkitScript(context, prefHandler))

    override fun getScriptArray(context: Context) =
        context.resources.getStringArray(R.array.pref_mlkit_script_values)
            .map { getDisplayNameForScript(context, it) }
            .toTypedArray()


    override suspend fun run(uri: Uri, context: Context, prefHandler: PrefHandler): Text =
        withContext(Dispatchers.Default) {
            initialize(context)
            val image =
                if (uri.isDebugAsset)
                    InputImage.fromBitmap(
                        BitmapFactory.decodeStream(context.assets.open(uri.pathSegments[1])),
                        0
                    )
                else InputImage.fromFilePath(context, uri)
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

    override fun info(context: Context, prefHandler: PrefHandler) = "Ml Kit (" +
            getDisplayNameForScript(
                context,
                getUserConfiguredMlkitScript(context, prefHandler).name
            ) + ")"
}

fun com.google.mlkit.vision.text.Text.wrap() = Text(textBlocks.map { textBlock ->
    TextBlock(textBlock.lines.map { line ->
        Line(line.text, line.boundingBox, line.elements.map { element ->
            Element(element.text, element.boundingBox)
        })
    })
})
