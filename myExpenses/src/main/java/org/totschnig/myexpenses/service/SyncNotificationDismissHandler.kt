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
    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate() {
        super.onCreate()
        (application as MyApplication).appComponent.inject(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val accountName = intent.getStringExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
            if (accountName != null) {
                val account = getAccount(accountName)
                if (!ContentResolver.isSyncActive(account, TransactionProvider.AUTHORITY)) {
                    GenericAccountService.requestSync(accountName, extras = Bundle().apply {
                        putLong(SyncAdapter.KEY_NOTIFICATION_CANCELLED, GenericAccountService.getSyncFrequency(prefHandler))
                    })
                }
            }
        }
    }
}