package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class AbstractSetupViewModel(application: Application) : AndroidViewModel(application) {
    val folderList: MutableLiveData<List<Pair<String, String>>> by lazy {
        MutableLiveData<List<Pair<String, String>>>()
    }
    val folderCreateResult: MutableLiveData<Pair<String, String>> by lazy {
        MutableLiveData<Pair<String, String>>()
    }
    val error: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }

    fun query() {
        viewModelScope.launch {
            try {
                folderList.postValue(getFolders())
            } catch (e: Exception) {
                error.postValue(e)
            }
        }
    }

    fun createFolder(label: String) {
        viewModelScope.launch {
            try {
                folderCreateResult.postValue(createFolderBackground(label))
            } catch (e: Exception) {
                error.postValue(e)
            }
        }
    }

    abstract suspend fun getFolders(): List<Pair<String, String>>
    abstract suspend fun createFolderBackground(label: String): Pair<String, String>
}