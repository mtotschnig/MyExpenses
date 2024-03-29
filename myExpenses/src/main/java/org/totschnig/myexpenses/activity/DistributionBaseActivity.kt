package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.data.Category

abstract class DistributionBaseActivity<T : DistributionViewModelBase<*>> :
    ProtectedFragmentActivity() {
    abstract val viewModel: T
    val expansionState
        get() = viewModel.expansionState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.displaySubTitle.collect {
                supportActionBar?.subtitle = it
                }
            }
        }
    }

    fun setupView(): ActivityComposeBinding {
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        return binding
    }

    override val snackBarContainerId: Int = R.id.compose_container

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val grouped = viewModel.grouping != Grouping.NONE
        menu.findItem(R.id.FORWARD_COMMAND).setEnabledAndVisible(grouped)
        menu.findItem(R.id.BACK_COMMAND).setEnabledAndVisible(grouped)
        return true
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {

            R.id.BACK_COMMAND -> {
                viewModel.backward()
                true
            }

            R.id.FORWARD_COMMAND -> {
                viewModel.forward()
                true
            }
            R.id.AGGREGATE_COMMAND ->{
                lifecycleScope.launch {
                    viewModel.persistAggregateNeutral(tag as Boolean)
                    invalidateOptionsMenu()
                    reset()
                }
                true
            }

            else -> false
        }

    protected fun reset() {
        expansionState.clear()
    }

    suspend fun showTransactions(category: Category, incomeType: Boolean = false) {
        viewModel.accountInfo.value?.let { accountInfo ->
            TransactionListComposeDialogFragment.newInstance(
                TransactionListViewModel.LoadingInfo(
                    accountId = accountInfo.accountId,
                    currency = accountInfo.currencyUnit,
                    catId = category.id,
                    grouping = viewModel.grouping,
                    groupingClause = viewModel.filterClause,
                    groupingArgs = viewModel.whereFilter.value.getSelectionArgsList(true),
                    label = if (category.level == 0) accountInfo.label(this) else category.label,
                    type = incomeType,
                    aggregateNeutral = viewModel.aggregateNeutral.first(),
                    icon = category.icon
                )
            )
                .show(supportFragmentManager, "List")
        }
    }
}