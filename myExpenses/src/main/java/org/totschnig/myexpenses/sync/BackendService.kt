package org.totschnig.myexpenses.sync

import android.Manifest
import android.content.Context
import com.vmadalin.easypermissions.EasyPermissions
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

enum class BackendService(
    private val className: String,
    val id: Int,
    val label: String,
    val feature: Feature?
) {
    DRIVE(
        "org.totschnig.drive.sync.GoogleDriveBackendProviderFactory",
        R.id.SYNC_BACKEND_DRIVE,
        "Drive",
        Feature.DRIVE
    ),
    LOCAL(
        "org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory",
        R.id.SYNC_BACKEND_LOCAL,
        "Local",
        null
    ) {
        override fun isAvailable(context: Context) =
            super.isAvailable(context) && EasyPermissions.hasPermissions(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    },
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
        Feature.WEBDAV
    );

    open fun isAvailable(context: Context) = try {
        Class.forName(className, false, this::class.java.classLoader)
        true
    } catch (e: Exception) {
        false
    }

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
        fun forAccount(account: String) = values().find { account.startsWith(it.label) }
        fun allAvailable(context: Context) = values().filter { it.isAvailable(context) }
    }
}