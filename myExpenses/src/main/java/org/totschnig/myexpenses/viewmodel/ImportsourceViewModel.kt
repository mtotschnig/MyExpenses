package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.io.FileUtils

class ImportSourceViewModel(application: Application) : BaseViewModel(application) {
    private val _appData: MutableLiveData<List<DocumentFile>> = MutableLiveData()
    val appData: LiveData<List<DocumentFile>> = _appData

    fun loadAppData(typeChecker: (String, String) -> Boolean) {
        viewModelScope.launch(coroutineContext()) {
            AppDirHelper.getAppDir(getApplication()).onSuccess { dir ->
                _appData.postValue(
                    dir.listFiles()
                        .filter { it.length() > 0 && !it.isDirectory }
                        .sortedByDescending { it.lastModified() }
                        .filter { it.name != null && it.type != null && typeChecker(it.type!!, FileUtils.getExtension(it.name!!)) }
                )
            }
        }
    }
}