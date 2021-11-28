package org.totschnig.mlkit_kore

import androidx.annotation.Keep
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import org.totschnig.mlkit.RecognizerProvider

@Keep
object Options: RecognizerProvider {
    override val textRecognizerOptions: TextRecognizerOptionsInterface
        get() =  KoreanTextRecognizerOptions.Builder().build()
}