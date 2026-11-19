package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.OcrFeature

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrFeature: OcrFeature
        get() = getApplication<MyApplication>().appComponent.ocrFeature() ?: object : OcrFeature {}

    private var savedOnResult: ((Uri) -> Unit)? = null
    private var savedOnException: ((Exception) -> Unit)? = null
    private var savedOnCancel: (() -> Unit)? = null

    fun tessDataExists() = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(ocrFeature.isAvailable(getApplication<MyApplication>()))
    }

    fun downloadTessData() = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(ocrFeature.downloadTessData(getApplication()))
    }

    fun registerCaptureLaunchers(
        baseActivity: BaseActivity,
        onResult: (Uri) -> Unit = {
            ocrFeature.showPreview(it, baseActivity.supportFragmentManager)
        },
        onException: (Exception) -> Unit = { baseActivity.processImageCaptureError(it) },
        onCancel: (() -> Unit)? = null
    ) {
        savedOnResult = onResult
        savedOnException = onException
        savedOnCancel = onCancel
        ocrFeature.registerCaptureLaunchers(baseActivity, onResult, onException, onCancel)
    }

    fun startCapture(
        baseActivity: BaseActivity,
        fileName: String = "SCAN"
    ) {
        val onResult = savedOnResult ?: {
            ocrFeature.showPreview(it, baseActivity.supportFragmentManager)
        }
        val onException = savedOnException ?: { baseActivity.processImageCaptureError(it) }
        val onCancel = savedOnCancel
        ocrFeature.registerCaptureLaunchers(baseActivity, onResult, onException, onCancel)
        ocrFeature.startCapture(baseActivity, fileName)
    }

    fun handleOcrData(intent: Intent?, fragmentManager: FragmentManager) {
        ocrFeature.handleData(intent, fragmentManager)
    }

    fun offerTessDataDownload(baseActivity: BaseActivity) {
        ocrFeature.offerInstall(baseActivity)
    }

    fun configureOcrEnginePrefs(
        tesseract: ListPreference,
        mlkit: ListPreference,
        gmsDocumentScanner: Preference?
    ) {
        ocrFeature.configureOcrEnginePrefs(tesseract, mlkit, gmsDocumentScanner)
    }

    fun configureAttachPicturePrefs(
        gmsDocumentScanner: Preference?,
        cameraCategory: PreferenceGroup?
    ) {
        ocrFeature.configureAttachPicturePrefs(gmsDocumentScanner, cameraCategory)
    }

    fun shouldShowEngineSelection() = ocrFeature.shouldShowEngineSelection()
}
