package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Account

class SyncViewModel(application: Application): ContentResolvingAndroidViewModel(application) {
    fun syncLinkRemote(account: Account): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            val accountId = Account.findByUuid(account.uuid)
            if (deleteAccountsInternal(arrayOf(accountId))) {
                account.save()
                emit(true)
            } else {
                emit(false)
            }
        }
}