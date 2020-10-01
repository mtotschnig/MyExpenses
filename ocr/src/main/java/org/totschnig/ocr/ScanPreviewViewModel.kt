package org.totschnig.ocr

import android.app.Application
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.feature.OcrResult
import java.io.File
import javax.inject.Inject

class ScanPreviewViewModel(application: Application) : AndroidViewModel(application) {
    var running: Boolean = false

    @Inject
    lateinit var ocrFeature: OcrFeature

    private val result = MutableLiveData<Result<OcrResult>>()

    fun getResult(): LiveData<Result<OcrResult>> = result

    init {
        DaggerOcrComponent.builder().appComponent((application as MyApplication).appComponent).build().inject(this)
    }

    fun rotate(right: Boolean, scanFilePath: String, action: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val exif = ExifInterface(scanFilePath)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_NORMAL -> if (right) ExifInterface.ORIENTATION_ROTATE_90 else ExifInterface.ORIENTATION_ROTATE_270
                    ExifInterface.ORIENTATION_ROTATE_90 -> if (right) ExifInterface.ORIENTATION_ROTATE_180 else ExifInterface.ORIENTATION_NORMAL
                    ExifInterface.ORIENTATION_ROTATE_180 -> if (right) ExifInterface.ORIENTATION_ROTATE_270 else ExifInterface.ORIENTATION_ROTATE_90
                    ExifInterface.ORIENTATION_ROTATE_270 -> if (right) ExifInterface.ORIENTATION_NORMAL else ExifInterface.ORIENTATION_ROTATE_180
                    else -> 0
                }.takeIf { it != 0 }?.also {
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, it.toString())
                    exif.saveAttributes()
                }
            }?.run { action() }
        }
    }

    fun runTextRecognition(scanFile: File) {
        if (!running) {
            running = true
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    result.postValue(runCatching { ocrFeature.runTextRecognition(scanFile, getApplication()) })
                }
            }
        }
    }
}