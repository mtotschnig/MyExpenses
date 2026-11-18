package org.totschnig.ocr

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.preference.ListPreference
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.ocr.OcrHandlerImpl.Companion.availableEngines
import org.totschnig.ocr.OcrHandlerImpl.Companion.getEngine
import timber.log.Timber

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler) : OcrFeature() {
    override fun startScan(activity: BaseActivity, fileName: String, onResult: (Uri) -> Unit) {
        try {
            MlKit.initialize(activity.applicationContext)
        } catch (_: IllegalStateException) {
            // Already initialized
        }

        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

            val scanner = GmsDocumentScanning.getClient(options)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    val key = "GmsDocumentScannerRequest"
                    val contract = ActivityResultContracts.StartIntentSenderForResult()
                    val launcher =
                        activity.activityResultRegistry.register(key, contract) { result ->
                            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                                GmsDocumentScanningResult.fromActivityResultIntent(
                                    result.data
                                )?.pages?.firstOrNull()?.imageUri?.let(onResult)
                            }
                        }
                    launcher.launch(request)
                }
                .addOnFailureListener { e ->
                    Timber.w(e, "GmsDocumentScanner failed, falling back to media chooser")
                    startMediaChooserDo(activity, fileName, onResult)
                }
        } catch (e: Exception) {
            Timber.w(e, "GmsDocumentScanner unavailable, falling back to media chooser")
            startMediaChooserDo(activity, fileName, onResult)
        }
    }

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
        if (DistributionHelper.distribution.hasDynamicFeatureDelivery) {
            true
        } else {
            availableEngines().size > 1
        }
}
