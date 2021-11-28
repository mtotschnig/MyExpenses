package org.totschnig.mlkit_jpan

import androidx.annotation.Keep
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import org.totschnig.mlkit.RecognizerProvider

@Keep
object Options: RecognizerProvider {
    override val textRecognizerOptions: TextRecognizerOptionsInterface
        get() =  JapaneseTextRecognizerOptions.Builder().build()
}