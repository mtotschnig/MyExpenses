package org.totschnig.mlkit

import com.google.mlkit.vision.text.TextRecognizerOptionsInterface

interface RecognizerProvider {
    val textRecognizerOptions: TextRecognizerOptionsInterface
}