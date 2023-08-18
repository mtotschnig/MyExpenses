package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.feature.BankingFeature

class BankingViewModel(application: Application) : AndroidViewModel(application) {
    private val bankingFeature: BankingFeature
        get() = getApplication<MyApplication>().appComponent.bankingFeature() ?: object : BankingFeature {}

    fun startBankingList(context: Context) {
        bankingFeature.startBankingList(context)
    }

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {
        bankingFeature.startSyncFragment(bankId, accountId, fragmentManager)
    }
}