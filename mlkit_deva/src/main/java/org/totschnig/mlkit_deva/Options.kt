package org.totschnig.mlkit_deva

import androidx.annotation.Keep
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import org.totschnig.mlkit.RecognizerProvider

@Keep
object Options: RecognizerProvider {
    override val textRecognizerOptions: TextRecognizerOptionsInterface
        get() =  DevanagariTextRecognizerOptions.Builder().build()
}