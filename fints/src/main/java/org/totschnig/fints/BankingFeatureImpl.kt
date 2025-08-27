package org.totschnig.fints

import android.app.Activity
import android.content.Context
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.model2.Bank

@Keep
class BankingFeatureImpl: BankingFeature {

    override val bankingActivityClass: Class<out Activity>
        get() = Banking::class.java

    override fun startSyncFragment(
        bankId: Long,
        accountId: Long,
        accountTypeId: Long,
        fragmentManager: FragmentManager
    ) {
        BankingSyncFragment.newInstance(bankId, accountId, accountTypeId).show(fragmentManager, "BANKING_SYNC")

    }

    override val bankIconRenderer: @Composable() (Modifier, Bank) -> Unit = { modifier, bank -> BankIconImpl(modifier, bank) }

    override fun bankIcon(bank: Bank) = bank.asWellKnown?.icon

    override fun syncMenuTitle(context: Context) = try {
        context.getString(R.string.menu_sync_account) + " (FinTS)"
    } catch (_: Exception) { super.syncMenuTitle(context) }

    override fun resolveAttributeLabel(context: Context, finTsAttribute: FinTsAttribute): String {
        return when(finTsAttribute) {
            FinTsAttribute.EREF -> R.string.eref_label
            FinTsAttribute.KREF -> R.string.kref_label
            FinTsAttribute.MREF -> R.string.mref_label
            FinTsAttribute.CRED -> R.string.cred_label
            FinTsAttribute.DBET -> R.string.dbet_label
            FinTsAttribute.SALDO -> R.string.saldo
            else -> null
        }?.let { context.getString(it) } ?: finTsAttribute.name
    }

}