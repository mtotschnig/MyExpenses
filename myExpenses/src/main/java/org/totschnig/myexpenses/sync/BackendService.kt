package org.totschnig.myexpenses.sync

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper

enum class BackendService(
    private val className: String,
    val id: Int,
    val label: String,
    val feature: Feature?,
    val supportsReconfiguration: Boolean = false
) {
    DRIVE(
        "org.totschnig.drive.sync.GoogleDriveBackendProviderFactory",
        R.id.SYNC_BACKEND_DRIVE,
        "Drive",
        Feature.DRIVE
    ),
    SAF(
        "org.totschnig.myexpenses.sync.StorageAccessFrameworkBackendProviderFactory",
        R.id.SYNC_BACKEND_LOCAL,
        "SAF",
        null
    ),
    DROPBOX(
        "org.totschnig.dropbox.sync.DropboxProviderFactory",
        R.id.SYNC_BACKEND_DROPBOX,
        "Dropbox",
        Feature.DROPBOX
    ),
    WEBDAV(
        "org.totschnig.webdav.sync.WebDavBackendProviderFactory",
        R.id.SYNC_BACKEND_WEBDAV,
        "WebDAV",
        Feature.WEBDAV,
        true
    );


    open fun isAvailable(context: Context) = if (DistributionHelper.isGithub) try {
        Class.forName(className, false, this::class.java.classLoader)
        true
    } catch (e: Exception) {
        false
    } else true

    fun instantiate(): SyncBackendProviderFactory? = try {
        Class.forName(className).newInstance() as? SyncBackendProviderFactory
    } catch (e: Exception) {
        CrashHandler.report(e)
        null
    }

    fun buildAccountName(extra: String): String {
        return "$label - $extra"
    }

    companion object {
        fun forAccount(account: String) = values().first { account.startsWith(it.label) }
        fun allAvailable(context: Context) = values().filter { it.isAvailable(context) }
    }
}