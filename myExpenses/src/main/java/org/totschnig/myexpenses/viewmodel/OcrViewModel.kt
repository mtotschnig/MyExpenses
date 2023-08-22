package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.ListPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.PictureDirHelper
import javax.inject.Inject

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrFeature: OcrFeature
        get() = getApplication<MyApplication>().appComponent.ocrFeature() ?: object : OcrFeature {}

    fun tessDataExists() = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(ocrFeature.isAvailable(getApplication<MyApplication>()))
    }

    fun downloadTessData() = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(ocrFeature.downloadTessData(getApplication()))
    }

    fun startOcrFeature(scanUri: Uri, fragmentManager: FragmentManager) {
        ocrFeature.start(scanUri, fragmentManager)
    }

    fun handleOcrData(intent: Intent?, fragmentManager: FragmentManager) {
        ocrFeature.handleData(intent, fragmentManager)
    }

    fun offerTessDataDownload(baseActivity: BaseActivity) {
        ocrFeature.offerInstall(baseActivity)
    }

    fun configureOcrEnginePrefs(tesseract: ListPreference, mlkit: ListPreference) {
        ocrFeature.configureOcrEnginePrefs(tesseract, mlkit)
    }

    fun shouldShowEngineSelection() = ocrFeature.shouldShowEngineSelection()
}