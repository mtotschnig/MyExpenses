package org.totschnig.myexpenses.feature

import android.content.Context
import androidx.fragment.app.FragmentManager

interface BankingFeature {
    fun startBankingList(context: Context) {}

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {}
}