package org.totschnig.myexpenses.activity

import android.view.Menu
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.viewmodel.DistributionViewModel

abstract class DistributionBaseActivity : ProtectedFragmentActivity() {
    open val viewModel: DistributionViewModel by viewModels()
    abstract val prefKey: PrefKey
    val expansionState
        get() = viewModel.expansionState

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES)?.let {
            it.isChecked = viewModel.aggregateTypes
        }
        return true
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.TOGGLE_AGGREGATE_TYPES -> {
                val value = tag as Boolean
                viewModel.setAggregateTypes(value)
                if (value) {
                    prefHandler.remove(prefKey)
                } else {
                    prefHandler.putBoolean(prefKey, viewModel.incomeType)
                }
                invalidateOptionsMenu()
                reset()
                true
            }
            else -> false
        }

    protected fun reset() {
        expansionState.clear()
    }
}