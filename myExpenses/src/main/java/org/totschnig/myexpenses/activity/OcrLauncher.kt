package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import java.io.Serializable

class OcrLauncher: ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            contribFeatureRequested(ContribFeature.OCR, true)
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature == ContribFeature.OCR) {
            if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                startMediaChooserDo("SCAN")
            } else {
                featureViewModel.requestFeature(this, Feature.OCR)
            }
        }
    }

    override fun processImageCaptureError(
        resultCode: Int,
        activityResult: CropImage.ActivityResult?
    ): Boolean {
        if (!super.processImageCaptureError(resultCode, activityResult)) {
            finish()
        }
        return true
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        if (feature == Feature.OCR) {
            startMediaChooserDo("SCAN")
        }
    }

    override suspend fun getEditIntent(): Intent = super.getEditIntent()!!.apply {
        putExtra(KEY_ACCOUNTID, intent.getLongExtra(KEY_ACCOUNTID, 0))
        putExtra(KEY_CURRENCY, intent.getStringExtra(KEY_CURRENCY))
        putExtra(KEY_COLOR, intent.getIntExtra(KEY_COLOR, 0))
    }

    override fun startEditFromOcrResult(result: OcrResultFlat?, scanUri: Uri) {
        super.startEditFromOcrResult(result, scanUri)
        finish()
    }

    override val imageCaptureErrorDismissCallback: Snackbar.Callback
        get() = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                finish()
            }
        }
}