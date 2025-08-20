package org.totschnig.myexpenses.util.crashreporting

import android.content.ContentResolver
import android.content.Context
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import timber.log.Timber
import java.util.*

interface CrashHandler {
    fun onAttachBaseContext(application: MyApplication) {}
    suspend fun setupLogging(context: Context) {}
    fun putCustomData(key: String, value: String) {}
    fun setEnabled(enabled: Boolean) {}
    fun setUserEmail(email: String?) {}
    fun addBreadcrumb(breadcrumb: String) {}
    fun initProcess(context: Context, syncService: Boolean) {}
    fun  getInfo(): Pair<String, String>? = null

    companion object {
        @JvmStatic
        fun reportWithDbSchema(contentResolver: ContentResolver, e: Throwable) {
            report(e, DbUtils.getSchemaDetails(contentResolver))
        }

        @JvmStatic
        @JvmOverloads
        fun report(e: Throwable, customData: Map<String, String>, tag: String? = null) {
            for ((key, value) in customData) {
                tag?.let { Timber.tag(it) }
                Timber.w("%s: %s", key, value)
            }
            report(e, tag)
        }

        @JvmStatic
        @JvmOverloads
        fun report(e: Throwable, key: String, data: String, tag: String? = null) {
            tag?.let { Timber.tag(it) }
            Timber.w("%s: %s", key, data)
            report(e, tag)
        }

        @JvmStatic
        @JvmOverloads
        fun throwOrReport(message: String, tag: String? = null) {
            throwOrReport(IllegalStateException(message), tag)
        }

        fun throwOrReport(e: Throwable, tag: String? = null) {
            if (BuildConfig.DEBUG) {
                throw e
            } else {
                report(e, tag)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun report(e: Throwable, tag: String? = null) {
            tag?.let { Timber.tag(it) }
            Timber.e(e)
        }

        val NO_OP: CrashHandler = object : CrashHandler {}
    }
}

abstract class BaseCrashHandler(val prefHandler: PrefHandler): CrashHandler {
    private var currentBreadCrumb: String? = null

    open fun setKeys(context: Context) {
        putCustomData("Distribution", getVersionInfo(context))
        context.packageManager.getInstallerPackageName(context.packageName)?.let {
            putCustomData("Installer", it)
        }
        putCustomData("Locale", Locale.getDefault().toString())
        putCustomData("Protection", when {
            prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) -> "Legacy"
            prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false) -> "Device"
            else -> "None"
        })
    }

    override fun setUserEmail(email: String?) {
        if (email != null) {
            putCustomData("UserEmail", email)
        }
    }

    @Synchronized
    override fun addBreadcrumb(breadcrumb: String) {
        Timber.i("Breadcrumb: %s", breadcrumb)
        currentBreadCrumb =
            (if (currentBreadCrumb == null) "" else currentBreadCrumb!!.substring(
                0.coerceAtLeast(currentBreadCrumb!!.length - 500)
            ) + "->$breadcrumb").also {
                putCustomData(CUSTOM_DATA_KEY_BREADCRUMB, it)
            }
    }

    companion object {
        private const val CUSTOM_DATA_KEY_BREADCRUMB = "Breadcrumb"
    }

}