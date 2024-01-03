package org.totschnig.onedrive.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.totschnig.myexpenses.activity.AbstractSyncSetup
import org.totschnig.onedrive.R
import org.totschnig.onedrive.viewmodel.OneDriveSetupViewModel
import timber.log.Timber

class OneDriveSetup: AbstractSyncSetup<OneDriveSetupViewModel>() {
    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            this,
            R.raw.msal_config,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                    mMultipleAccountApp = application
                    mMultipleAccountApp!!.acquireToken(this@OneDriveSetup, arrayOf("user.read"),
                        object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            Timber.d("Successfully authenticated")
                            Timber.d("ID Token: " + authenticationResult.account.claims!!["id_token"])
                            showSnackBar(authenticationResult.account.username)
                        }

                        override fun onError(exception: MsalException?) {
                            TODO("Not yet implemented")
                        }

                        override fun onCancel() {
                            TODO("Not yet implemented")
                        }

                    })
                }

                override fun onError(error: MsalException){
                    Timber.e(error)
                }
            })
    }

    override fun handleException(exception: Exception): Boolean {
        TODO("Not yet implemented")
    }

    override fun instantiateViewModel() = ViewModelProvider(this)[OneDriveSetupViewModel::class.java]

    override fun Intent.buildSuccessIntent(folder: Pair<String, String>) {
        TODO("Not yet implemented")
    }
}