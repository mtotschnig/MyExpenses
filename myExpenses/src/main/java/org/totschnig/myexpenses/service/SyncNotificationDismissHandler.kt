package org.totschnig.myexpenses.service

import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import android.app.IntentService
import android.content.Intent
import org.totschnig.myexpenses.provider.DatabaseConstants
import android.content.ContentResolver
import org.totschnig.myexpenses.provider.TransactionProvider
import android.os.Bundle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import javax.inject.Inject

class SyncNotificationDismissHandler : IntentService("SyncNotificationDismissHandler") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val accountName = intent.getStringExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
            if (accountName != null) {
                val account = getAccount(accountName)
                SyncAdapter.clearNotificationContent(account)
            }
        }
    }
}