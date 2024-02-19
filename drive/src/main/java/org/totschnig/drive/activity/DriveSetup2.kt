package org.totschnig.drive.activity

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.evernote.android.state.State
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import org.totschnig.drive.sync.GoogleDriveBackendProvider
import org.totschnig.drive.viewmodel.DriveSetupViewModel
import org.totschnig.myexpenses.activity.AbstractSyncSetup
import org.totschnig.myexpenses.sync.GenericAccountService

class DriveSetup2 : AbstractSyncSetup<DriveSetupViewModel>() {

    @State
    var accountName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val result = googleApiAvailability.isGooglePlayServicesAvailable(this)
            when {
                result == ConnectionResult.SUCCESS ->
                    startActivityForResult(AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), true, null, null, null, null),
                        REQUEST_ACCOUNT_PICKER
                    )
                googleApiAvailability.isUserResolvableError(result) ->
                    googleApiAvailability.getErrorDialog(this, result, 0)?.show()
                else -> showSnackBar("Google Play Services error $result", Snackbar.LENGTH_LONG)
            }
        }
    }

    override fun instantiateViewModel(): DriveSetupViewModel = ViewModelProvider(this)[DriveSetupViewModel::class.java]

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ACCOUNT_PICKER -> if (data != null) {
                    handleSignInResult(data)
                }
                REQUEST_RESOLUTION -> viewModel.query()
                else -> super.onActivityResult(requestCode, resultCode, data)
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
        ((if (exception is UserRecoverableAuthIOException) exception.cause else exception) as? UserRecoverableAuthException)?.intent?.let {
            startActivityForResult(it, REQUEST_RESOLUTION)
            true
        } ?: false

    override fun Intent.buildSuccessIntent(folder: Pair<String, String>)  {
        putExtra(AccountManager.KEY_USERDATA, Bundle(2).apply {
            putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, folder.first)
            putString(GoogleDriveBackendProvider.KEY_GOOGLE_ACCOUNT_EMAIL, accountName)
        })
    }

    companion object {
        private const val REQUEST_ACCOUNT_PICKER = 1
        private const val REQUEST_RESOLUTION = 2
    }
}