package org.totschnig.mlkit_han

import androidx.annotation.Keep
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.totschnig.mlkit.RecognizerProvider

@Keep
object Options: RecognizerProvider {
    override val textRecognizerOptions: TextRecognizerOptionsInterface
        get() =  ChineseTextRecognizerOptions.Builder().build()
}