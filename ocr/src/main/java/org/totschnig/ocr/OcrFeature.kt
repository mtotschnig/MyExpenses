package org.totschnig.ocr

import android.app.Activity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
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

@Keep
abstract class OcrFeature : OcrFeature {

    override fun showPreview(scanUri: Uri, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanUri).show(fragmentManager, FRAGMENT_TAG)
    }

    override fun startMediaChooserDo(
        activity: BaseActivity,
        fileName: String,
        onResult: (Uri) -> Unit,
        onException: (Exception) -> Unit,
        onCancel: (() -> Unit)?
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
            val launcher =
                activity.activityResultRegistry.register(key, contract) { activityResult ->
                    val cropResult = CropImage.getActivityResult(activityResult.data)
                    if (activityResult.resultCode == Activity.RESULT_OK && cropResult != null) {
                        activity.baseViewModel.cleanupOrigFile(cropResult.originalUri)
                        onResult(cropResult.uri)
                    } else if (activityResult.resultCode ==
                        CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE
                    ) {
                        onException(cropResult.error)
                    } else if (activityResult.resultCode == Activity.RESULT_CANCELED) {
                        onCancel?.invoke()
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
}
