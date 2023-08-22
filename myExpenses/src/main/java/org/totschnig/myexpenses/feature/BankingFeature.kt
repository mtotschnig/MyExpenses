package org.totschnig.myexpenses.feature

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager

interface BankingFeature {
    fun startBankingList(context: Context) {}

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {}

    val bankIconRenderer: (@Composable (String) -> Unit)?
        get() = null

    fun syncMenuTitle(context: Context): String = ""
}