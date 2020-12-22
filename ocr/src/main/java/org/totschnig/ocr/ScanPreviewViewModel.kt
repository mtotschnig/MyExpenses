package org.totschnig.ocr

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.OCR_REQUEST
import org.totschnig.myexpenses.feature.OcrFeature.Companion.ACTION
import org.totschnig.myexpenses.feature.OcrFeature.Companion.MIME_TYPE
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.util.AppDirHelper
import java.io.File
import javax.inject.Inject

class ScanPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private var running: Boolean = false

    @Inject
    lateinit var ocrHandler: OcrHandler

    private val result = MutableLiveData<Result<OcrResult>>()

    fun getResult(): LiveData<Result<OcrResult>> = result

    init {
        DaggerOcrComponent.builder().appComponent((application as MyApplication).appComponent).build().inject(this)
    }

    fun runTextRecognition(scanFile: File, activity: Activity) {
        if (!running) {
            running = true
            if (BuildConfig.FLAVOR == "extern") {
                runExternal(scanFile, activity)
            } else {
                viewModelScope.launch {
                    result.postValue(runCatching { ocrHandler.runTextRecognition(scanFile, activity) })
                }
            }
        }
    }

    private fun runExternal(scanFile: File, activity: Activity) {
        activity.startActivityForResult(
                Intent(ACTION).apply {
                    setDataAndType(AppDirHelper.ensureContentUri(Uri.fromFile(scanFile)), MIME_TYPE)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }, OCR_REQUEST)
    }

    fun handleData(intent: Intent) {
        viewModelScope.launch {
            result.postValue(runCatching { ocrHandler.handleData(intent) })
        }
    }

    fun getOcrInfo(context: Context): CharSequence? = ocrHandler.info(context)
}