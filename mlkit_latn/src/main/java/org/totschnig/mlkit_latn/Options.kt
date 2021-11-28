package org.totschnig.mlkit_latn

import androidx.annotation.Keep
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.totschnig.mlkit.RecognizerProvider

@Keep
object Options: RecognizerProvider {
    override val textRecognizerOptions: TextRecognizerOptionsInterface
        get() =  TextRecognizerOptions.DEFAULT_OPTIONS
}