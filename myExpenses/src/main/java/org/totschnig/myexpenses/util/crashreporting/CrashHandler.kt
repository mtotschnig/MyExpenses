package org.totschnig.myexpenses.util.crashreporting

import android.content.Context
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import timber.log.Timber
import java.util.*

abstract class CrashHandler {
    private var currentBreadCrumb: String? = null

    open fun onAttachBaseContext(application: MyApplication) {}
    open fun setupLogging(context: Context) {}
    open fun putCustomData(key: String, value: String) {}
    open fun setEnabled(enabled: Boolean) {}

    open fun setKeys(context: Context) {
        putCustomData("Distribution", getVersionInfo(context))
        context.packageManager.getInstallerPackageName(context.packageName)?.let {
            putCustomData(
                "Installer",
                it
            )
        }
        putCustomData("Locale", Locale.getDefault().toString())
    }

    fun setUserEmail(value: String?) {
        if (value != null) {
            putCustomData("UserEmail", value)
        }
    }

    @Synchronized
    fun addBreadcrumb(breadcrumb: String) {
        Timber.i("Breadcrumb: %s", breadcrumb)
        currentBreadCrumb =
            (if (currentBreadCrumb == null) "" else currentBreadCrumb!!.substring(
                Math.max(0, currentBreadCrumb!!.length - 500)
            ) + "->$breadcrumb").also {
                putCustomData(CUSTOM_DATA_KEY_BREADCRUMB, it)
            }
    }

    open fun initProcess(context: Context, syncService: Boolean) {}

    companion object {
        private const val CUSTOM_DATA_KEY_BREADCRUMB = "Breadcrumb"
        @JvmStatic
        fun reportWithDbSchema(e: Throwable) {
            report(e, DbUtils.getSchemaDetails())
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

        private fun throwOrReport(e: RuntimeException, tag: String? = null) {
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

        var NO_OP: CrashHandler = object : CrashHandler() {}
    }
}