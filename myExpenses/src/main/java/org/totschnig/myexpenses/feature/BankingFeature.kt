package org.totschnig.myexpenses.feature

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.db2.FinTsAttribute

interface BankingFeature {
    fun startBankingList(context: Context) {}

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {}

    val bankIconRenderer: (@Composable (String) -> Unit)?
        get() = null

    fun syncMenuTitle(context: Context): String = ""

    fun resolveAttributeLabel(context: Context, finTsAttribute: FinTsAttribute): String = finTsAttribute.name
}