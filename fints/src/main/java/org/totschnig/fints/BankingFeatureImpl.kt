package org.totschnig.fints

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.feature.BankingFeature

@Keep
class BankingFeatureImpl: BankingFeature {
    override fun startBankingList(context: Context) {
        context.startActivity(Intent(context, Banking::class.java))
    }

    override fun startSyncFragment(
        bankId: Long,
        accountId: Long,
        fragmentManager: FragmentManager
    ) {
        BankingSyncFragment.newInstance(bankId, accountId).show(fragmentManager, "BANKING_SYNC")

    }

    override val bankIconRenderer: @Composable() ((String) -> Unit) = { BankIconImpl(blz = it) }

    override fun syncMenuTitle(context: Context) = context.getString(R.string.menu_sync_account) + " (FinTS)"

}