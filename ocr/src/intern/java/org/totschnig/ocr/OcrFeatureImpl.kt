package org.totschnig.ocr

import android.content.Context
import androidx.annotation.Keep
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.ocr.OcrHandlerImpl.Companion.availableEngines
import org.totschnig.ocr.OcrHandlerImpl.Companion.getEngine

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler): OcrFeature() {
    override fun downloadTessData(context: Context) =
            (getEngine(context, prefHandler) as? TesseractEngine)?.downloadTessData(context, prefHandler)

    override fun isAvailable(context: Context) =
            (getEngine(context, prefHandler) as? TesseractEngine)?.tessDataExists(context, prefHandler) ?: true

    override fun offerInstall(baseActivity: BaseActivity) {
        (getEngine(baseActivity, prefHandler) as? TesseractEngine)?.offerTessDataDownload(baseActivity)
    }

    override fun configureTesseractLanguagePref(listPreference: androidx.preference.ListPreference) {
        (getEngine(listPreference.context, prefHandler) as? TesseractEngine)?.let {
            listPreference.isVisible = true
            listPreference.entries = it.getLanguageArray(listPreference.context)
        } ?: run { super.configureTesseractLanguagePref(listPreference) }
    }

    override fun shouldShowEngineSelection() = availableEngines().size > 1
}