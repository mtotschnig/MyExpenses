package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase
import org.totschnig.myexpenses.viewmodel.data.Category

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
        setAggregateTypesFromPreferences()
    }

    fun setupView(): ActivityComposeBinding {
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        return binding
    }

    override val snackBarContainerId: Int = R.id.compose_container

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES)?.let {
            it.isChecked = viewModel.aggregateTypes
        }
        val grouped = viewModel.grouping != Grouping.NONE
        Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.FORWARD_COMMAND), grouped)
        Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BACK_COMMAND), grouped)
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

    private fun setAggregateTypesFromPreferences() {
        val aggregateTypesFromPreference =
            if (prefHandler.isSet(prefKey)) prefHandler.getBoolean(prefKey, false) else null
        viewModel.setAggregateTypes(aggregateTypesFromPreference == null)
        if (aggregateTypesFromPreference != null) {
            viewModel.setIncomeType(aggregateTypesFromPreference)
        }
    }

    fun showTransactions(category: Category) {
        viewModel.accountInfo.value?.let { accountInfo ->
            TransactionListDialogFragment.newInstance(
                accountInfo.accountId,
                category.id,
                viewModel.grouping,
                viewModel.filterClause,
                viewModel.filterPersistence.value?.whereFilter?.getSelectionArgs(true),
                if (category.level == 0) accountInfo.label(this) else category.label,
                if (viewModel.aggregateTypes) 0 else (if (viewModel.incomeType) 1 else -1),
                true,
                category.icon?.let { resources.getIdentifier(it, "drawable", packageName) }
            )
                .show(supportFragmentManager, TransactionListDialogFragment::class.java.name)
        }
    }
}