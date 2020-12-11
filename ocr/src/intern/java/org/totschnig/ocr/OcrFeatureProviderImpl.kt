package org.totschnig.ocr

import android.content.Context
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager

@Keep
object OcrFeatureProviderImpl: OcrFeatureProvider() {
    override fun downloadTessData(context: Context, language: String) {
        tesseractEngine().downloadTessData(context, language)
    }

    override fun tessDataExists(context: Context, language: String) =
            tesseractEngine().tessDataExists(context, language)

    private fun tesseractEngine() =
            (Class.forName("org.totschnig.tesseract.Engine").kotlin.objectInstance as TesseractEngine)

    override fun onDownloadComplete(fragmentManager: FragmentManager) {
        (fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.onDownloadComplete()
    }
}