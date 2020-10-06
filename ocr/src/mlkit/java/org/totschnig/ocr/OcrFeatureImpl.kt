package org.totschnig.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OcrFeatureImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context): AbstractOcrFeatureImpl(prefHandler, userLocaleProvider, context) {
    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        @Suppress("BlockingMethodInNonBlockingContext") val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        return suspendCoroutine { cont ->
            TextRecognition.getClient().process(image)
                    .addOnSuccessListener { texts ->
                        cont.resume(processTextRecognitionResult(texts.wrap()))
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e as Throwable)
                    }
        }
    }
}

fun com.google.mlkit.vision.text.Text.wrap() = Text(textBlocks.map { textBlock ->
    TextBlock(textBlock.lines.map { line ->
        Line(line.text, line.boundingBox, line.elements.map { element ->
            Element(element.text, element.boundingBox)
        })
    })
})