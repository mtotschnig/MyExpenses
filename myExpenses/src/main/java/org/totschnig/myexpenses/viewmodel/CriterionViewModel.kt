package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.dialog.CriterionInfo
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.KEY_INFO
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION

class CriterionViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle,
): ContentResolvingAndroidViewModel(application) {

    var info: CriterionInfo
        get() = savedStateHandle.get<CriterionInfo>(KEY_INFO)!!
        set(value) {
            savedStateHandle[KEY_INFO] = value
        }

    val infoLiveData = savedStateHandle.getLiveData<CriterionInfo?>(KEY_INFO)

    fun updateCriterion(accountId: Long, criterion: Long) {
        repository.updateAccount(accountId) {
            put(KEY_CRITERION, criterion)
        }
    }
}