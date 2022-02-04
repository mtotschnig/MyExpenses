package org.totschnig.drive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import org.totschnig.drive.activity.DriveSetup2
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.SYNC_BACKEND_SETUP_REQUEST
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.util.Result
import java.io.Serializable
import java.util.*

@Keep
class GoogleDriveBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = GoogleDriveBackendProvider(context, account, accountManager)

    override val label = "Drive"

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        when {
            result == ConnectionResult.SUCCESS -> {
                activity.startActivityForResult(
                    Intent(activity, DriveSetup2::class.java),
                    SYNC_BACKEND_SETUP_REQUEST
                )
            }
            googleApiAvailability.isUserResolvableError(result) -> {
                googleApiAvailability.getErrorDialog(activity, result, 0)?.show()
            }
            else -> {
                activity.showSnackBar(
                    String.format(
                        Locale.ROOT,
                        "Google Play Services error %d",
                        result
                    ), Snackbar.LENGTH_LONG
                )
            }
        }
    }

    override fun isEnabled(context: Context?): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(context!!)
        return result == ConnectionResult.SUCCESS || googleApiAvailability.isUserResolvableError(
            result
        )
    }

    override val id = R.id.SYNC_BACKEND_DRIVE

    override fun getRepairIntent(activity: Activity?): Intent? = null

    override fun startRepairTask(activity: ManageSyncBackends?, data: Intent?) = false

    override fun handleRepairTask(mExtra: Serializable?): Result<*>? = null

    override fun init() {}
}