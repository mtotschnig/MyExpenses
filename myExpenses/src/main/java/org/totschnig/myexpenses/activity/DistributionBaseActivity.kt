package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase

abstract class DistributionBaseActivity<T: DistributionViewModelBase<*>> : ProtectedFragmentActivity() {
    abstract val viewModel: T
    abstract val prefKey: PrefKey
    val expansionState
        get() = viewModel.expansionState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            viewModel.displaySubTitle.collect {
                supportActionBar?.subtitle = it
            }
        }
    }

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
            R.id.BACK_COMMAND -> {
                viewModel.backward()
                true
            }
            R.id.FORWARD_COMMAND -> {
                viewModel.forward()
                true
            }
            else -> false
        }

    protected fun reset() {
        expansionState.clear()
    }
}