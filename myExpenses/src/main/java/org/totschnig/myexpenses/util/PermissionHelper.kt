package org.totschnig.myexpenses.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import com.vmadalin.easypermissions.EasyPermissions
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import java.io.File

object PermissionHelper {
    const val PERMISSIONS_REQUEST_WRITE_CALENDAR = 1

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

    enum class PermissionGroup(
        val androidPermissions: Array<String>,
        val prefKey: PrefKey,
        val requestCode: Int
    ) {
        CALENDAR(
            arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
            PrefKey.CALENDAR_PERMISSION_REQUESTED,
            PERMISSIONS_REQUEST_WRITE_CALENDAR
        );

        fun permissionRequestRationale(context: Context): String {
            return when (this) {
                CALENDAR -> Utils.getTextWithAppName(
                    context,
                    R.string.calendar_permission_required
                ).toString()
            }
        }

        /**
         * @return true if all of our [.androidPermissions] are granted
         */
        fun hasPermission(context: Context): Boolean {
            return EasyPermissions.hasPermissions(context, *androidPermissions)
        }

        companion object {
            fun fromRequestCode(requestCode: Int): PermissionGroup {
                if (requestCode == CALENDAR.requestCode) return CALENDAR
                throw IllegalArgumentException("Undefined requestCode $requestCode")
            }
        }
    }
}