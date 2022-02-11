package org.totschnig.myexpenses.sync

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import com.vmadalin.easypermissions.EasyPermissions.hasPermissions
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.EditTextDialog

@Keep
class LocalFileBackendProviderFactory : SyncBackendProviderFactory() {
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ): LocalFileBackendProvider {
        check(
            hasPermissions(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) { "LocalFileBackendProvider needs READ_EXTERNAL_STORAGE permission" }
        return LocalFileBackendProvider(
            context,
            accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL)
        )
    }

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val args = Bundle()
        args.putString(EditTextDialog.KEY_DIALOG_TITLE, "Local backend: Directory path")
        EditTextDialog.newInstance(args)
            .show(activity.supportFragmentManager, "LOCAL_BACKEND_DIRECTORY_PATH")
    }

}