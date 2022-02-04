package org.totschnig.myexpenses.sync

import android.content.Context
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub

/**
 * [java.util.ServiceLoader] unfortunately does not work from sync process
 */
object ServiceLoader {
    private const val GOOGLE = "org.totschnig.drive.sync.GoogleDriveBackendProviderFactory"
    private const val LOCAL = "org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory"
    private const val DROPBOX = "org.totschnig.dropbox.sync.DropboxProviderFactory"
    private const val WEBDAV = "org.totschnig.webdav.sync.WebDavBackendProviderFactory"

    @JvmStatic
    fun load(context: Context) = buildList {

        tryToInstantiate(WEBDAV, context)?.let {
            add(it)
        }

        tryToInstantiate(DROPBOX, context)?.let {
            add(it)
        }

        tryToInstantiate(LOCAL, context)?.let {
            add(it)
        }

        tryToInstantiate(GOOGLE, context, !isGithub)?.let {
            add(it)
        }
    }

    private fun tryToInstantiate(
        className: String,
        context: Context,
        reportFailure: Boolean = false
    ) = try {
        (Class.forName(className).newInstance() as? SyncBackendProviderFactory)?.takeIf {
            it.isEnabled(context)
        }
    } catch (e: Exception) {
        if (reportFailure) {
            CrashHandler.report(e)
        }
        null
    }
}