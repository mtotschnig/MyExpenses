package org.totschnig.ocr

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.OCR_REQUEST
import org.totschnig.myexpenses.feature.OcrFeatureProvider.Companion.ACTION
import org.totschnig.myexpenses.feature.OcrFeatureProvider.Companion.MIME_TYPE
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.util.AppDirHelper
import java.io.File
import javax.inject.Inject

class ScanPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private var running: Boolean = false

    private var orientation = 0

    @Inject
    lateinit var ocrFeature: OcrFeature

    private val result = MutableLiveData<Result<OcrResult>>()

    fun getResult(): LiveData<Result<OcrResult>> = result

    init {
        DaggerOcrComponent.builder().appComponent((application as MyApplication).appComponent).build().inject(this)
    }

    fun runTextRecognition(scanFile: File, activity: Activity) {
        if (!running) {
            running = true
            if (BuildConfig.FLAVOR == "extern") {
                if (orientation == 0) {
                    viewModelScope.launch {
                        withContext(Dispatchers.Default) {
                            orientation = ExifInterface(scanFile.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        }
                        runExternal(scanFile, activity)
                    }
                } else {
                    runExternal(scanFile, activity)
                }
            } else {
                viewModelScope.launch {
                    result.postValue(runCatching { ocrFeature.runTextRecognition(scanFile, activity) })
                }
            }
        }
    }

    private fun runExternal(scanFile: File, activity: Activity) {
        activity.startActivityForResult(
                Intent(ACTION).apply {
                    putExtra("orientation", when (orientation) {
                        ExifInterface.ORIENTATION_NORMAL -> 0
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    })
                    setDataAndType(AppDirHelper.ensureContentUri(Uri.fromFile(scanFile)), MIME_TYPE)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }, OCR_REQUEST)
    }

    fun handleData(intent: Intent) {
        viewModelScope.launch {
            result.postValue(runCatching { ocrFeature.handleData(intent) })
        }
    }
}