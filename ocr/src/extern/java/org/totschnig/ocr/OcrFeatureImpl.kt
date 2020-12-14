package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import javax.inject.Inject

class OcrFeatureImpl  @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context): AbstractOcrFeatureImpl(prefHandler, userLocaleProvider, context) {
    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        TODO("Not implemented. Should be handled by calling Intent with action org.totschnig.ocr.action.RECOGNIZE")
    }
    override fun isAvailable(context: Context) = Utils.isIntentAvailable(context, OcrFeature.intent())

    override fun offerInstall(activity: BaseActivity) {
        MessageDialogFragment.newInstance(
                null,
                activity.getString(R.string.ocr_download_info),
                MessageDialogFragment.Button(R.string.button_download, R.id.OCR_DOWNLOAD_COMMAND, null),
                MessageDialogFragment.Button(R.string.learn_more, R.id.OCR_FAQ_COMMAND, null),
                null).show(activity.getSupportFragmentManager(), "OCR_DOWNLOAD")
    }
}