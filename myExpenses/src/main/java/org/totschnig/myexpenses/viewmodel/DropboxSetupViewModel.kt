package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.BuildConfig
import java.util.*



class DropboxSetupViewModel(application: Application) : AndroidViewModel(application) {
    // Create a LiveData with a String
    val folderList: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>()
    }

    fun loadDropboxRootFolders(authToken: String) {
        viewModelScope.launch {
            folderList.postValue(getRootFolders(authToken))
        }
    }

    suspend fun getRootFolders(authToken: String) = withContext(Dispatchers.IO) {
        val userLocale = Locale.getDefault().toString()
        val requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build()
        val mDbxClient = DbxClientV2(requestConfig, authToken)
        var result = mDbxClient.files().listFolder("")
        val folderList = mutableListOf<String>()
        while (true) {
            folderList.addAll(result.entries
                    .filter { metadata -> metadata is FolderMetadata }
                    .map { metadata -> metadata.name })
            if (!result.getHasMore()) {
                break
            }
            result = mDbxClient.files().listFolderContinue(result.getCursor())
        }
        folderList
    }
}