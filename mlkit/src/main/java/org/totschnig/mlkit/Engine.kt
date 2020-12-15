package org.totschnig.mlkit

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.ocr.Element
import org.totschnig.ocr.Line
import org.totschnig.ocr.Text
import org.totschnig.ocr.TextBlock
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Keep
object Engine: org.totschnig.ocr.Engine  {
    var initialized: Boolean = false
    private fun initialize(context: Context) {
        if (!initialized) {
            MlKit.initialize(context)
            initialized = true
        }
    }

    override suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text =
            withContext(Dispatchers.Default) {
                initialize(context)
                val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                suspendCoroutine { cont ->
                    TextRecognition.getClient().process(image)
                            .addOnSuccessListener { texts ->
                                cont.resume(texts.wrap())
                            }
                            .addOnFailureListener { e ->
                                cont.resumeWithException(e as Throwable)
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
