package org.totschnig.ocr

import android.content.Context
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler): OcrFeature() {
    override fun downloadTessData(context: Context, prefHandler: PrefHandler) =
            (getEngine(prefHandler, context) as? TesseractEngine)?.downloadTessData(context, prefHandler)

    override fun isAvailable(context: Context) =
            (getEngine(prefHandler, context) as? TesseractEngine)?.tessDataExists(context, prefHandler) ?: true

    override fun offerInstall(baseActivity: BaseActivity) {
        (getEngine(prefHandler, baseActivity) as? TesseractEngine)?.offerTessDataDownload(baseActivity)
    }
}