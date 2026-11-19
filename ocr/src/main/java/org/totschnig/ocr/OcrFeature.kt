package org.totschnig.ocr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.PictureDirHelper

const val FRAGMENT_TAG = "SCAN_PREVIEW"
const val CROP_IMAGE_REQUEST_KEY = "CropImageRequest"

data class CropCaptureBinding(
    val launcher: ActivityResultLauncher<Intent>
)

@Keep
abstract class OcrFeature : OcrFeature {
    protected val cropBindings = HashMap<BaseActivity, CropCaptureBinding>()

    override fun showPreview(scanUri: Uri, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanUri).show(fragmentManager, FRAGMENT_TAG)
    }

    override fun registerCaptureLaunchers(
        activity: BaseActivity,
        onResult: (Uri) -> Unit,
        onException: (Exception) -> Unit,
        onCancel: (() -> Unit)?
    ) {
        val contract = ActivityResultContracts.StartActivityForResult()
        val launcher = activity.activityResultRegistry.register(
            CROP_IMAGE_REQUEST_KEY,
            contract
        ) { activityResult ->
            val cropResult = CropImage.getActivityResult(activityResult.data)
            when (activityResult.resultCode) {
                Activity.RESULT_OK if cropResult != null -> {
                    activity.baseViewModel.cleanupOrigFile(cropResult.originalUri)
                    onResult(cropResult.uri)
                }

                CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> onException(cropResult.error)

                Activity.RESULT_CANCELED -> onCancel?.invoke()
            }
        }

        cropBindings[activity] = CropCaptureBinding(launcher)

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cropBindings.remove(activity)
            }
        })
    }

    override fun startCapture(
        activity: BaseActivity,
        fileName: String
    ) {
        startFallbackCapture(activity, fileName)
    }

    fun startFallbackCapture(
        activity: BaseActivity,
        fileName: String
    ) {
        val binding = cropBindings[activity] ?: return
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

            binding.launcher.launch(intent)
        }
    }
}
