package org.totschnig.ocr

import android.content.Context
import androidx.annotation.Keep
import androidx.preference.ListPreference
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.ocr.OcrHandlerImpl.Companion.availableEngines
import org.totschnig.ocr.OcrHandlerImpl.Companion.getEngine

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler) : OcrFeature() {
    override fun downloadTessData(context: Context) =
        (getEngine(context, prefHandler) as? TesseractEngine)?.downloadTessData(
            context,
            prefHandler
        )

    override fun isAvailable(context: Context) =
        (getEngine(context, prefHandler) as? TesseractEngine)?.tessDataExists(context, prefHandler)
            ?: true

    override fun offerInstall(baseActivity: BaseActivity) {
        (getEngine(baseActivity, prefHandler) as? TesseractEngine)?.offerTessDataDownload(
            baseActivity
        )
    }

    override fun configureOcrEnginePrefs(
        tesseract: ListPreference,
        mlkit: ListPreference
    ) {
        val engine = getEngine(tesseract.context, prefHandler)
        if (engine is TesseractEngine) {
            tesseract.isVisible = true
            tesseract.entries = engine.getLanguageArray(tesseract.context)
        } else {
            tesseract.isVisible = false
        }
        if (engine is MlkitEngine) {
            mlkit.isVisible = true
            mlkit.entries = engine.getScriptArray(tesseract.context)
        } else {
            mlkit.isVisible = false
        }
    }

    override fun shouldShowEngineSelection() =
        if (DistributionHelper.distribution.hasDynamicFeatureDelivery)
            true
        else
            availableEngines().size > 1
}