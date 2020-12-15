package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.R

@Keep
class OcrFeatureImpl(val prefHandler: PrefHandler): OcrFeature() {
    override fun isAvailable(context: Context) = Utils.isIntentAvailable(context, org.totschnig.myexpenses.feature.OcrFeature.intent())

    override fun offerInstall(baseActivity: BaseActivity) {
        MessageDialogFragment.newInstance(
                null,
                baseActivity.getString(R.string.ocr_download_info),
                MessageDialogFragment.Button(R.string.button_download, R.id.OCR_DOWNLOAD_COMMAND, null),
                MessageDialogFragment.Button(R.string.learn_more, R.id.OCR_FAQ_COMMAND, null),
                null).show(baseActivity.supportFragmentManager, "OCR_DOWNLOAD")
    }

    override fun handleData(intent: Intent?, fragmentManager: FragmentManager) {
        (fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.handleData(intent)
    }
}