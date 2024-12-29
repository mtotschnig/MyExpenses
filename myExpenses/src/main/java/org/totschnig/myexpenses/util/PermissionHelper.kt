package org.totschnig.myexpenses.util

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import com.vmadalin.easypermissions.EasyPermissions
import org.totschnig.myexpenses.R
import java.io.File


object PermissionHelper {
    @IntDef(PERMISSIONS_REQUEST_WRITE_CALENDAR, PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI,
        PERMISSIONS_REQUEST_NOTIFICATIONS_SYNC, PERMISSIONS_REQUEST_NOTIFICATIONS_PLANNER,
        PERMISSIONS_REQUEST_NOTIFICATIONS_AUTO_BACKUP, PERMISSIONS_REQUEST_STORAGE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PermissionRequestCode

    const val PERMISSIONS_REQUEST_WRITE_CALENDAR = 1
    const val PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI = 2
    const val PERMISSIONS_REQUEST_NOTIFICATIONS_SYNC = 3
    const val PERMISSIONS_REQUEST_NOTIFICATIONS_PLANNER = 4
    const val PERMISSIONS_REQUEST_NOTIFICATIONS_AUTO_BACKUP = 5
    const val PERMISSIONS_REQUEST_STORAGE = 6

    @JvmStatic
    fun hasCalendarPermission(context: Context) = PermissionGroup.CALENDAR.hasPermission(context)

    @JvmStatic
    fun canReadUri(uri: Uri, context: Context): Boolean {
        if ("file" == uri.scheme) {
            uri.path?.let {
                return with(File(it)) {
                    exists() && canRead()
                }
            }
            return false
        }
        return AppDirHelper.getFileProviderAuthority(context) == uri.authority || context.checkUriPermission(
            uri, Binder.getCallingPid(), Binder.getCallingUid(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    fun getRationale(context: Context, @PermissionRequestCode requestCode: Int, vararg group: PermissionGroup): String =
        when (requestCode) {
            PERMISSIONS_REQUEST_WRITE_CALENDAR -> {
                group.joinToString(" ") {
                    Utils.getTextWithAppName(
                        context, when(it) {
                            PermissionGroup.CALENDAR -> R.string.calendar_permission_required
                            PermissionGroup.NOTIFICATION -> R.string.notifications_permission_required_planner
                            else -> throw IllegalArgumentException()
                        }).toString()
                }
            }
            PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI -> context.getString(R.string.notifications_permission_required_webui)
            PERMISSIONS_REQUEST_NOTIFICATIONS_SYNC -> context.getString(R.string.notifications_permission_required_sync)
            PERMISSIONS_REQUEST_NOTIFICATIONS_PLANNER -> Utils.getTextWithAppName(context, R.string.notifications_permission_required_planner).toString()
            PERMISSIONS_REQUEST_NOTIFICATIONS_AUTO_BACKUP -> context.getString(R.string.notifications_permission_required_auto_backup)
            PERMISSIONS_REQUEST_STORAGE -> "In order to save images to external storage you must grant Storage Permission"
            else -> throw IllegalArgumentException()
        }

    enum class PermissionGroup(
        val androidPermissions: List<String>
    ) {
        STORAGE(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ),
        CALENDAR(
            listOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
        ),
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        NOTIFICATION(
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        )
        ;

        /**
         * @return true if all of our [.androidPermissions] are granted
         */
        fun hasPermission(context: Context): Boolean {
            return EasyPermissions.hasPermissions(context, *androidPermissions.toTypedArray())
        }

        companion object {
            fun fromPermission(permission: String): PermissionGroup {
                return PermissionGroup.entries.first { it.androidPermissions.contains(permission) }
            }
        }
    }
}