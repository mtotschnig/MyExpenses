package org.totschnig.drive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import org.totschnig.drive.activity.DriveSetup2
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.SYNC_BACKEND_SETUP_REQUEST
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import java.util.*

@Keep
class GoogleDriveBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = GoogleDriveBackendProvider(context, account, accountManager)

    override fun setupIntent(activity: ProtectedFragmentActivity) = Intent(activity, DriveSetup2::class.java)
}