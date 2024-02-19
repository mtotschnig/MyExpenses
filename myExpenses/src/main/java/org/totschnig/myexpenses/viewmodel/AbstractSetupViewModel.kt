package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.sync.BackendService

abstract class AbstractSetupViewModel(
    val backendService: BackendService,
    application: Application,
    val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val loadFinished: Boolean
        get() = folderList.isInitialized

    val folderList: LiveData<List<Pair<String, String>>> = savedStateHandle.getLiveData("folderList")

    private fun setFolderList(folderList: List<Pair<String, String>>) {
        savedStateHandle["folderList"] = folderList
    }

    val folderCreateResult: LiveData<Pair<String, String>> = savedStateHandle.getLiveData("folderCreateResult")

    private fun setCreateFolderResult(createFolderResult: Pair<String, String>) {
        savedStateHandle["folderCreateResult"] = createFolderResult
    }

    val error: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }

    fun query() {
        viewModelScope.launch {
            try {
                setFolderList(getFolders())
            } catch (e: Exception) {
                error.postValue(e)
            }
        }
    }

    fun createFolder(label: String) {
        if (folderList.value?.any { it.second == label } == true) {
            error.postValue(Exception("A folder with this name already exists."))
        } else {
            viewModelScope.launch {
                try {
                    setCreateFolderResult(createFolderBackground(label.trim()))
                } catch (e: Exception) {
                    error.postValue(e)
                }
            }
        }
    }

    abstract suspend fun getFolders(): List<Pair<String, String>>
    abstract suspend fun createFolderBackground(label: String): Pair<String, String>
}