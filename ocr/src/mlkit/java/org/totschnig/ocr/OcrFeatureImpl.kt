package org.totschnig.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.FeatureScope
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import timber.log.Timber
import java.io.File
import java.text.NumberFormat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class OcrFeatureImpl @Inject constructor(private val prefHandler: PrefHandler) : OcrFeature {

    private val numberFormat = NumberFormat.getInstance()

    override suspend fun runTextRecognition(file: File, context: Context) : OcrResult? {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        return suspendCoroutine { cont ->
            TextRecognition.getClient().process(image)
                    .addOnSuccessListener { texts ->
                        cont.resume(processTextRecognitionResult(texts, prefHandler.getString(PrefKey.OCR_TOTAL_INDICATORS, "Total")!!.lines())?.let { OcrResult(it) })
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e as Throwable)
                    }
        }
    }

    fun Text.Line.bOr0() = boundingBox?.bottom ?: 0
    fun Text.Line.tOr0() = boundingBox?.top ?: 0

    private fun processTextRecognitionResult(texts: Text, totalIndicators: List<String>): String? {
        val blocks = texts.textBlocks
        val lines = mutableListOf<Text.Line>()
        for (i in blocks.indices) {
            lines.addAll(blocks[i].lines)
        }
        for (line in lines) {
            Timber.d("OCR: Line: %s %s", line.text, line.boundingBox)
        }
        return lines.filter {
            for (totalIndicator in totalIndicators) {
                if (it.text.filter { c -> c.isLetter() }.startsWith(totalIndicator.replace(" ", ""))) return@filter true
                var matchesAllSplits = true
                for (split in totalIndicator.split(' ')) {
                    var found = false
                    for (element in it.elements) {
                        val filter = element.text.filter { c -> c.isLetter() }
                        if (filter.startsWith(split, ignoreCase = true)) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        matchesAllSplits = false
                        break
                    }
                }
                if (matchesAllSplits) return@filter true
            }
            false
        }.map { totalBlock ->
            //find amount in the line or in the nearest line
            extractAmount(totalBlock) ?:
            lines.minus(totalBlock).minByOrNull { (it.bOr0() - totalBlock.bOr0()).absoluteValue.coerceAtMost((it.tOr0() - totalBlock.tOr0()).absoluteValue) } ?.let {
                extractAmount(it)
            }
        }.filterNotNull().firstOrNull()
    }

    fun extractAmount(line: Text.Line): String? = line.elements.filter { element ->
        try {
            numberFormat.parse(element.text)
            true
        } catch (e: Exception) {
            false
        }
    }.map { it.text }.takeIf { !it.isEmpty() }?.joinToString(separator = "")
}