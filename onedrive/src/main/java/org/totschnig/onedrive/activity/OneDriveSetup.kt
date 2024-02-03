package org.totschnig.onedrive.activity

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.evernote.android.state.State
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.totschnig.myexpenses.activity.AbstractSyncSetup
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.onedrive.R
import org.totschnig.onedrive.sync.OneDriveBackendProvider.Companion.KEY_MICROSOFT_ACCOUNT
import org.totschnig.onedrive.viewmodel.OneDriveSetupViewModel
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.N)
class OneDriveSetup : AbstractSyncSetup<OneDriveSetupViewModel>() {
    private lateinit var mMultipleAccountApp: IMultipleAccountPublicClientApplication

    @State
    var account: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
        for debugging add this block to msal_config
          "logging" :  {
    "log_level": "VERBOSE",
    "logcat_enabled" : true
  }
         */
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            applicationContext,
            R.raw.msal_config,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    mMultipleAccountApp.acquireToken(
                        AcquireTokenParameters.Builder()
                            .startAuthorizationFromActivity(this@OneDriveSetup)
                            .withScopes(listOf("Files.ReadWrite.All"))
                            .withCallback(object : AuthenticationCallback {
                                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                    account = authenticationResult.account.id
                                    Timber.i("AuthenticationResult expiresOn: ${authenticationResult.expiresOn}")
                                    viewModel.initWithAccessToken(authenticationResult.accessToken)
                                    viewModel.query()
                                }

                                override fun onError(exception: MsalException) {
                                    onException(exception)
                                }

                                override fun onCancel() {
                                    finish()
                                }

                            })
                            .build()
                    )
                }

                override fun onError(exception: MsalException) {
                    onException(exception)
                }
            })
    }

    fun onException(exception: Exception) {
        CrashHandler.report(exception)
        showMessage(exception.safeMessage)
    }

    override fun handleException(exception: Exception) = false

    override fun instantiateViewModel() =
        ViewModelProvider(this)[OneDriveSetupViewModel::class.java]

    override fun Intent.buildSuccessIntent(folder: Pair<String, String>) {
        putExtra(AccountManager.KEY_USERDATA, Bundle(1).apply {
            putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, folder.second)
            putString(KEY_MICROSOFT_ACCOUNT, account)
        })
    }
}