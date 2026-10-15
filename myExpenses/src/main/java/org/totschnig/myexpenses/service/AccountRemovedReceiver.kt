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
import org.totschnig.myexpenses.provider.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.doAsync
import timber.log.Timber

class AccountRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i(intent.action)
        val accountName = intent.getStringExtra(KEY_ACCOUNT_NAME)
        val accountType = intent.getStringExtra(KEY_ACCOUNT_TYPE)
        Timber.i("%s/%s", accountType, accountName)
        val prefHandler = context.injector.prefHandler()
        if (intent.action == AccountManager.ACTION_ACCOUNT_REMOVED &&
            accountType == GenericAccountService.ACCOUNT_TYPE &&
            prefHandler.getInt(PrefKey.CURRENT_VERSION, 0) > 0
                ) {
            if (prefHandler.cloudStorage == accountName) {
                prefHandler.cloudStorage = null
            }
            doAsync {
                try {
                    val where = "$KEY_SYNC_ACCOUNT_NAME = ? "
                    context.contentResolver.update(
                        TransactionProvider.ACCOUNTS_URI, ContentValues(1).apply {
                            putNull(KEY_SYNC_ACCOUNT_NAME)
                        },
                        where, arrayOf(accountName)
                    )
                } catch (e: Exception) {
                    CrashHandler.report(e)
                }
            }
        }
    }
}