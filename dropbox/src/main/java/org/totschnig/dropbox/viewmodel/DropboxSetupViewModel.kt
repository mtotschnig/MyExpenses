package org.totschnig.dropbox.viewmodel

import android.app.Application
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.dropbox.BuildConfig
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel
import java.io.IOException
import java.util.*


class DropboxSetupViewModel(application: Application) : AbstractSetupViewModel(application) {
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
                throw IOException("Unable to create folder with label $label", e).also {
                    CrashHandler.report(it)
                }
            }.metadata.let { Pair(it.id, it.name) }
        } ?: throw Exception("Dropbox client not set up")
    }
}