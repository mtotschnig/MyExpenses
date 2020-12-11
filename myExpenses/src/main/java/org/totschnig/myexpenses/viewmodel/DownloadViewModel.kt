package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import timber.log.Timber
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    fun tessDataExists(language: String) = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(File(getApplication<MyApplication>().getExternalFilesDir("tesseract4"), filePath(language)).exists())
    }

    private fun fileName(language: String) = "%s.traineddata".format(language)
    private fun filePath(language: String) = "fast/tessdata/%s.traineddata".format(language)

    fun downloadTessData(language: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val application = getApplication<MyApplication>()
                val uri = Uri.parse("https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/%s".format(fileName(language)))
                ContextCompat.getSystemService(application, DownloadManager::class.java)?.let {
                    it.enqueue(DownloadManager.Request(uri)
                            .setTitle(application.getString(R.string.pref_tesseract_language_title))
                            .setDescription(language)
                            .setDestinationInExternalFilesDir(application, "tesseract4", filePath(language)))
                }
            }
        }
    }
}