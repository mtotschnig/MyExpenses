package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.model2.Bank

interface BankingFeature {
    companion object {
        const val TAG = "Banking"
    }

    val bankingActivityClass: Class<out Activity>
        get()  { throw NotImplementedError() }

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {}

    val bankIconRenderer: @Composable ((Modifier, Bank) -> Unit)?
        get() = null

    fun syncMenuTitle(context: Context): String = "FinTS"

    fun resolveAttributeLabel(context: Context, finTsAttribute: FinTsAttribute): String = finTsAttribute.name
}