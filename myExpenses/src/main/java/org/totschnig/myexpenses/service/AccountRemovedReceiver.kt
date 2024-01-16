package org.totschnig.myexpenses.service

import android.accounts.AccountManager
import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.accounts.AccountManager.KEY_ACCOUNT_TYPE
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.doAsync
import timber.log.Timber

class AccountRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i(intent.action)
        Timber.i(intent.getStringExtra(KEY_ACCOUNT_TYPE) + "/" + intent.getStringExtra(KEY_ACCOUNT_NAME))
        if (intent.action == AccountManager.ACTION_ACCOUNT_REMOVED &&
            intent.getStringExtra(KEY_ACCOUNT_TYPE) == GenericAccountService.ACCOUNT_TYPE &&
            context.injector.prefHandler().getInt(PrefKey.CURRENT_VERSION, 0) > 0
                ) {
            doAsync {
                try {
                    val where = "${DatabaseConstants.KEY_SYNC_ACCOUNT_NAME} = ? "
                    context.contentResolver.update(
                        TransactionProvider.ACCOUNTS_URI, ContentValues(1).apply {
                            putNull(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
                        },
                        where, arrayOf(intent.getStringExtra(KEY_ACCOUNT_NAME))
                    )
                } catch (e: Exception) {
                    CrashHandler.report(e)
                }
            }
        }
    }
}