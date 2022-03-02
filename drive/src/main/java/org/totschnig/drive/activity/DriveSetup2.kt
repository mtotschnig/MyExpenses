package org.totschnig.drive.activity

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import icepick.State
import org.totschnig.drive.sync.GoogleDriveBackendProvider
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AbstractSyncSetup
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.drive.viewmodel.DriveSetupViewModel
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity

class DriveSetup2 : AbstractSyncSetup<DriveSetupViewModel>() {

    @JvmField
    @State
    var accountName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            startActivityForResult(AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), true, null, null, null, null),
                REQUEST_ACCOUNT_PICKER
            )
        }
    }

    override fun instantiateViewModel(): DriveSetupViewModel = ViewModelProvider(this)[DriveSetupViewModel::class.java]

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ACCOUNT_PICKER -> if (resultData != null) {
                    handleSignInResult(resultData)
                }
                REQUEST_RESOLUTION -> viewModel.query()
                else -> super.onActivityResult(requestCode, resultCode, resultData)
            }
        } else {
            abort()
        }
    }

    private fun handleSignInResult(result: Intent) {
        accountName = result.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        accountName?.let {
            viewModel.initWithAccount(it)
            viewModel.query()
        }
    }

    override fun handleException(exception: java.lang.Exception) : Boolean =
        ((if (exception is UserRecoverableAuthIOException) exception.cause else exception) as? UserRecoverableAuthException)?.let {
            startActivityForResult(it.intent, REQUEST_RESOLUTION)
            true
        } ?: false

    override fun buildSuccessIntent(folder: Pair<String, String>) = Intent().apply {
        putExtra(AccountManager.KEY_USERDATA, Bundle(2).apply {
            putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, folder.first)
            putString(GoogleDriveBackendProvider.KEY_GOOGLE_ACCOUNT_EMAIL, accountName)
        })
        putExtra(SyncBackendSetupActivity.KEY_SYNC_PROVIDER_ID, R.id.SYNC_BACKEND_DRIVE)
        putExtra(AccountManager.KEY_ACCOUNT_NAME, folder.second)
    }

    companion object {
        private const val REQUEST_ACCOUNT_PICKER = 1
        private const val REQUEST_RESOLUTION = 2
    }
}