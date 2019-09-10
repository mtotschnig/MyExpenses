package org.totschnig.myexpenses.viewmodel

import android.app.Application
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.BuildConfig
import java.util.*


class DropboxSetupViewModel(application: Application) : AbstractSetupViewModel(application) {
    private var mDbxClient: DbxClientV2? = null

    fun initWithAuthToken(authToken: String) {
        val userLocale = Locale.getDefault().toString()
        val requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build()
        mDbxClient = DbxClientV2(requestConfig, authToken)
    }

    override suspend fun getFolders() = withContext(Dispatchers.IO) {
        val folderList = mutableListOf<Pair<String, String>>()
        mDbxClient?.let {
            var result = it.files().listFolder("")
            while (true) {
                folderList.addAll(result.entries
                        .filter { metadata -> metadata is FolderMetadata }
                        .map { metadata ->  (metadata as FolderMetadata).let { Pair(metadata.id, metadata.name) } })
                if (!result.getHasMore()) {
                    break
                }
                result = it.files().listFolderContinue(result.getCursor())
            }
        }
        folderList
    }

    override suspend fun createFolderBackground(label: String) = withContext(Dispatchers.IO) {
        mDbxClient?.let {
            it.files().createFolderV2("/" + label).metadata.let { Pair(it.id, it.name) }
        } ?: throw Exception("Dropbox client not set up")
    }
}