package org.totschnig.myexpenses.service

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.checkSyncAccounts
import timber.log.Timber

class LegacyAccountChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i(intent.action)
        @Suppress("DEPRECATION")
        if (intent.action == AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION) {
            try {
                if ((context.applicationContext as MyApplication).appComponent.prefHandler().getInt(PrefKey.CURRENT_VERSION, 0) > 0) {
                    checkSyncAccounts(context)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}