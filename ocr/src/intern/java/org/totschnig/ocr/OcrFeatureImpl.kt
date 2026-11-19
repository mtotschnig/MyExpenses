package org.totschnig.ocr

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.ListPreference
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.ocr.OcrHandlerImpl.Companion.availableEngines
import org.totschnig.ocr.OcrHandlerImpl.Companion.getEngine

const val GMS_SCANNER_REQUEST_KEY = "GmsDocumentScannerRequest"

data class GmsCaptureBinding(
    val launcher: ActivityResultLauncher<IntentSenderRequest>
)

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler) : OcrFeature() {
    private val gmsBindings = HashMap<BaseActivity, GmsCaptureBinding>()

    override fun registerCaptureLaunchers(
        activity: BaseActivity,
        onResult: (Uri) -> Unit,
        onException: (Exception) -> Unit,
        onCancel: (() -> Unit)?
    ) {
        super.registerCaptureLaunchers(activity, onResult, onException, onCancel)

        val contract = ActivityResultContracts.StartIntentSenderForResult()
        val launcher = activity.activityResultRegistry.register(
            GMS_SCANNER_REQUEST_KEY,
            contract
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                    ?.pages?.firstOrNull()?.imageUri?.let { uri ->
                        val contentUri = AppDirHelper.ensureContentUri(uri, activity)
                        onResult(contentUri)
                    }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                onCancel?.invoke()
            }
        }

        gmsBindings[activity] = GmsCaptureBinding(launcher)

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                gmsBindings.remove(activity)
            }
        })
    }

    override fun startCapture(
        activity: BaseActivity,
        fileName: String
    ) {
        val gmsBinding = gmsBindings[activity]
        if (gmsBinding != null && prefHandler.getBoolean(PrefKey.GMS_DOCUMENT_SCANNER, true)) {
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
                        gmsBinding.launcher.launch(request)
                    }
                    .addOnFailureListener { e ->
                        CrashHandler.report(e)
                        startFallbackCapture(activity, fileName)
                    }
                return
            } catch (e: Exception) {
                CrashHandler.report(e)
                startFallbackCapture(activity, fileName)
            }
        } else {
            startFallbackCapture(activity, fileName)
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
        mlkit: ListPreference,
        gmsDocumentScanner: androidx.preference.Preference?
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
            mlkit.entries = engine.getScriptArray(mlkit.context)
        } else {
            mlkit.isVisible = false
        }
        gmsDocumentScanner?.isVisible = true
    }

    override fun configureAttachPicturePrefs(
        gmsDocumentScanner: androidx.preference.Preference?,
        cameraCategory: androidx.preference.PreferenceGroup?
    ) {
        gmsDocumentScanner?.isVisible = true
        val useGmsScanner = prefHandler.getBoolean(PrefKey.GMS_DOCUMENT_SCANNER, true)
        cameraCategory?.isVisible = !useGmsScanner
    }

    override fun shouldShowEngineSelection() =
        DistributionHelper.distribution.hasDynamicFeatureDelivery || availableEngines().size > 1
}
