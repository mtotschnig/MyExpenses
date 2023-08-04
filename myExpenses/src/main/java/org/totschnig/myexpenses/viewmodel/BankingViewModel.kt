package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.viewmodel.data.Bank

class BankingViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    val banks: StateFlow<List<Bank>> =
        flow<List<Bank>> {
            //emit(listOf(Bank("DKB"), Bank("Sparkasse")))
        }
            .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
}