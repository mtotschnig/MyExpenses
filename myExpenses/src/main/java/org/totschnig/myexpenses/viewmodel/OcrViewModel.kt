package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.File

class TessdataMissingException(val language: String) : Throwable() {}

class OcrViewModel(application: Application) : AndroidViewModel(application) {
    var ocrFeatureProvider: OcrFeatureProvider? = try {
        Class.forName("org.totschnig.ocr.OcrFeatureProviderImpl").kotlin.objectInstance as OcrFeatureProvider
    } catch (e: ClassNotFoundException) {
        CrashHandler.report(e)
        null
    }

    fun tessDataExists(language: String) = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        ocrFeatureProvider?.let {
            emit(it.tessDataExists(getApplication<MyApplication>(), language))
        }
    }

    fun downloadTessData(language: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ocrFeatureProvider?.downloadTessData(getApplication(), language)
            }
        }
    }

    fun startOcrFeature(scanFile: @NotNull File, fragmentManager: FragmentManager) {
        ocrFeatureProvider?.start(scanFile, fragmentManager)
    }

    fun handleOcrData(intent: @NotNull Intent, fragmentManager: FragmentManager) {
        ocrFeatureProvider?.handleData(intent, fragmentManager)
    }

    fun onDownloadComplete(fragmentManager: FragmentManager) {
        ocrFeatureProvider?.onDownloadComplete(fragmentManager)
    }

    fun getScanFiles(action: (file: Pair<File, File>) -> Unit) {
        viewModelScope.launch {
            action(withContext(Dispatchers.IO) {
                Pair(PictureDirHelper.getOutputMediaFile("SCAN", true, false), PictureDirHelper.getOutputMediaFile("SCAN_CROPPED", true, false))
            })
        }
    }

    fun getScanUri(file: File) = try {
        AppDirHelper.getContentUriForFile(file)
    }  catch (e: IllegalArgumentException) {
        Uri.fromFile(file)
    }
}