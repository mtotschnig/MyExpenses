package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.db2.FinTsAttribute

interface BankingFeature {
    companion object {
        const val TAG = "Banking"
    }

    val bankingActivityClass: Class<out Activity>
        get()  { throw NotImplementedError() }

    fun startSyncFragment(bankId: Long, accountId: Long, fragmentManager: FragmentManager) {}

    val bankIconRenderer: (@Composable (String) -> Unit)?
        get() = null

    fun syncMenuTitle(context: Context): String = ""

    fun resolveAttributeLabel(context: Context, finTsAttribute: FinTsAttribute): String = finTsAttribute.name
}