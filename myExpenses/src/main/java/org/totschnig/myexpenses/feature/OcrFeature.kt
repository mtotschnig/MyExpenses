package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.PictureDirHelper

interface OcrFeature {
    companion object {
        const val TAG = "OcrFeature"
        const val ACTION = "org.totschnig.ocr.action.RECOGNIZE"
        const val MIME_TYPE = "image/jpeg"
        fun intent() = Intent(ACTION).setType(MIME_TYPE)
    }

    fun start(scanUri: Uri, fragmentManager: FragmentManager) {}
    fun startScan(
        activity: BaseActivity,
        fileName: String = "SCAN",
        onResult: (Uri) -> Unit = {
            start(it, activity.supportFragmentManager)
        }
    ) {
        startMediaChooserDo(activity, fileName, onResult)
    }

    fun startMediaChooserDo(
        activity: BaseActivity,
        fileName: String,
        onResult: (Uri) -> Unit
    ) {
        activity.lifecycleScope.launch {
            val uris = withContext(Dispatchers.IO) {
                PictureDirHelper.getOutputMediaUri(
                    temp = true,
                    application = activity.requireApplication(),
                    fileName = fileName
                ) to PictureDirHelper.getOutputMediaUri(
                    temp = true,
                    application = activity.requireApplication(),
                    fileName = "${fileName}_CROPPED"
                )
            }
            val contract = ActivityResultContracts.StartActivityForResult()
            val key = "CropImageRequest_$fileName"
            val launcher = activity.activityResultRegistry.register(key, contract) { activityResult ->
                val cropResult = CropImage.getActivityResult(activityResult.data)
                if (activityResult.resultCode == Activity.RESULT_OK && cropResult != null) {
                    activity.baseViewModel.cleanupOrigFile(cropResult)
                    onResult(cropResult.uri)
                } else {
                    activity.processImageCaptureError(activityResult.resultCode, cropResult)
                }
            }
            val intent = CropImage.activity()
                .setCameraOnly(!activity.prefHandler.getBoolean(PrefKey.CAMERA_CHOOSER, false))
                .setCameraPackage(
                    activity.prefHandler.getString(PrefKey.CAMERA_APP)?.takeIf { it.isNotEmpty() }
                )
                .setAllowFlipping(false)
                .setCaptureImageOutputUri(uris.first)
                .setOutputUri(uris.second)
                .setGuidelines(CropImageView.Guidelines.ON)
                .getIntent(activity)

            launcher.launch(intent)
        }
    }

    fun handleData(intent: Intent?, fragmentManager: FragmentManager) {}
    fun downloadTessData(context: Context): String? = null
    fun isAvailable(context: Context): Boolean = false
    fun offerInstall(baseActivity: BaseActivity) {}
    fun configureOcrEnginePrefs(tesseract: ListPreference, mlkit: ListPreference) {
        tesseract.isVisible = false
        mlkit.isVisible = false
    }

    fun shouldShowEngineSelection() = true
}

@Parcelize
data class OcrResult(
    val amountCandidates: List<String>,
    val dateCandidates: List<Pair<LocalDate, LocalTime?>>,
    val payeeCandidates: List<Payee>
) : Parcelable {
    fun needsDisambiguation() =
        amountCandidates.size > 1 || dateCandidates.size > 1 || payeeCandidates.size > 1

    fun selectCandidates(amountIndex: Int = 0, dateIndex: Int = 0, payeeIndex: Int = 0) =
        OcrResultFlat(
            amountCandidates.getOrNull(amountIndex),
            dateCandidates.getOrNull(dateIndex),
            payeeCandidates.getOrNull(payeeIndex)
        )
}

@Parcelize
data class OcrResultFlat(
    val amount: String?,
    val date: Pair<LocalDate, LocalTime?>?,
    val payee: Payee?
) : Parcelable {
    @IgnoredOnParcel
    val isEmpty = amount == null && date == null && payee == null
}

@Parcelize
data class Payee(val id: Long, val name: String) : Parcelable

interface OcrHost {
    fun processOcrResult(result: Result<OcrResult>, scanUri: Uri)
}
