package org.totschnig.ocr

import android.content.Context
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler

@Keep
object OcrFeatureProviderImpl: OcrFeatureProvider() {
    override fun downloadTessData(context: Context, prefHandler: PrefHandler) =
            tesseractEngine().downloadTessData(context, prefHandler)

    override fun tessDataExists(context: Context, prefHandler: PrefHandler) =
            tesseractEngine().tessDataExists(context, prefHandler)

    private fun tesseractEngine() =
            (Class.forName("org.totschnig.tesseract.Engine").kotlin.objectInstance as TesseractEngine)

    override fun onDownloadComplete(fragmentManager: FragmentManager) {
        (fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.onDownloadComplete()
    }

    override fun offerTessDataDownload(baseActivity: BaseActivity) {
        tesseractEngine().offerTessDataDownload(baseActivity)
    }
}