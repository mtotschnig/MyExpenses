package org.totschnig.myexpenses.sync

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import com.vmadalin.easypermissions.EasyPermissions.hasPermissions
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.EditTextDialog
import org.totschnig.myexpenses.util.Result
import java.io.Serializable

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

    override val label = "Local"

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val args = Bundle()
        args.putString(EditTextDialog.KEY_DIALOG_TITLE, "Local backend: Directory path")
        EditTextDialog.newInstance(args)
            .show(activity.supportFragmentManager, "LOCAL_BACKEND_DIRECTORY_PATH")
    }

    override val id = R.id.SYNC_BACKEND_LOCAL

    override fun getRepairIntent(activity: Activity?): Intent? = null

    override fun startRepairTask(activity: ManageSyncBackends?, data: Intent?) = false

    override fun handleRepairTask(mExtra: Serializable?): Result<*>? = null

    override fun init() {}


}