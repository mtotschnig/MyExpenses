package org.totschnig.dropbox.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.dropbox.BuildConfig
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel
import timber.log.Timber
import java.util.*


class DropboxSetupViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    AbstractSetupViewModel(BackendService.DROPBOX, application, savedStateHandle) {
    private var mDbxClient: DbxClientV2? = null

    fun initWithCredentials(credentials: DbxCredential) {
        val userLocale = Locale.getDefault().toString()
        val requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build()
        mDbxClient = DbxClientV2(requestConfig, credentials)
    }

    override suspend fun getFolders() = withContext(Dispatchers.IO) {
        val folderList = mutableListOf<Pair<String, String>>()
        mDbxClient?.let {
            var result = it.files().listFolder("")
            while (true) {
                folderList.addAll(result.entries
                        .filterIsInstance<FolderMetadata>()
                        .map { metadata -> Pair(metadata.id, metadata.name) })
                if (!result.hasMore) {
                    break
                }
                result = it.files().listFolderContinue(result.cursor)
            }
        }
        folderList
    }

    override suspend fun createFolderBackground(label: String) = withContext(Dispatchers.IO) {
        mDbxClient?.let { client ->
            try {
                client.files().createFolderV2("/$label")
            } catch (e: Exception) {
                Timber.w("Unable to create folder with label %s", label)
                throw e
            }.metadata.let { Pair(it.id, it.name) }
        } ?: throw Exception("Dropbox client not set up")
    }
}