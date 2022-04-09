package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Account

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle):
    DistributionViewModelBase<Account>(application, savedStateHandle) {
    fun initWithAccount(accountId: Long) {
        viewModelScope.launch {
            account(accountId, true).asFlow().collect {
                _accountInfo.tryEmit(it)
            }
        }
    }
}