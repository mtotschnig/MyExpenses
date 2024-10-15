package org.totschnig.myexpenses.viewmodel

import android.app.Application
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION

class CriterionViewModel(application: Application): ContentResolvingAndroidViewModel(application) {

    fun updateCriterion(accountId: Long, criterion: Long) {
        repository.updateAccount(accountId) {
            put(KEY_CRITERION, criterion)
        }
    }
}