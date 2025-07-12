package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.WindowInsets
import android.view.WindowInsets.Type.systemBars
import android.view.WindowInsetsController.BEHAVIOR_DEFAULT
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.data.Category
import kotlin.math.absoluteValue

abstract class DistributionBaseActivity<T : DistributionViewModelBase<*>> :
    ProtectedFragmentActivity() {
    abstract val viewModel: T
    val expansionState
        get() = viewModel.expansionState

    protected val showChart = mutableStateOf(false)

    abstract val showChartPrefKey: PrefKey

    abstract val showChartDefault: Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showChart.value = prefHandler.getBoolean(showChartPrefKey, showChartDefault)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.displaySubTitle.collect {
                    supportActionBar?.subtitle = it
                }
            }
        }
        configureFullScreen()
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun configureFullScreen() {
        if (resources.getBoolean(R.bool.allowFullScreen)) {
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                if (insets.isVisible(WindowInsets.Type.navigationBars())
                    || insets.isVisible(WindowInsets.Type.statusBars())
                ) {
                    if (supportFragmentManager.fragments.isEmpty()) {
                        onToggleFullScreen(false)
                    }
                } else {
                    onToggleFullScreen(true)
                }
                v.onApplyWindowInsets(insets)
            }
        }
    }

    open fun onToggleFullScreen(fullScreen: Boolean) {
        if (fullScreen) supportActionBar?.hide() else supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.distribution_base, menu)
        super.onCreateOptionsMenu(menu)
        return true
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
        menu.findItem(R.id.TOGGLE_CHART_COMMAND)?.let {
            it.isChecked = showChart.value
            checkMenuIcon(it, R.drawable.ic_menu_chart)
        }
        return true
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {

            R.id.BACK_COMMAND -> {
                viewModel.backward()
            }

            R.id.FORWARD_COMMAND -> {
                viewModel.forward()
            }

            R.id.AGGREGATE_COMMAND -> {
                lifecycleScope.launch {
                    viewModel.persistAggregateNeutral(tag as Boolean)
                    invalidateOptionsMenu()
                    reset()
                }
            }

            R.id.TOGGLE_CHART_COMMAND -> {
                showChart.value = tag as Boolean
                prefHandler.putBoolean(showChartPrefKey, showChart.value)
                invalidateOptionsMenu()
            }

            R.id.FULL_SCREEN_COMMAND -> {
                goFullScreen()
            }

            else -> return false
        }
        return true
    }

    @SuppressLint("InlinedApi")
    @TargetApi(Build.VERSION_CODES.R)
    private fun goFullScreen() {
        window.insetsController?.apply {
            hide(systemBars())
            systemBarsBehavior = BEHAVIOR_DEFAULT
        }
    }

    protected fun reset() {
        expansionState.clear()
    }

    suspend fun showTransactions(category: Category, incomeType: Boolean = false) {
        viewModel.accountInfo.value?.let { accountInfo ->
            TransactionListComposeDialogFragment.newInstance(
                TransactionListViewModel.LoadingInfo(
                    accountId = accountInfo.accountId,
                    currency = currencyContext[accountInfo.currency],
                    catId = category.id.absoluteValue,
                    grouping = viewModel.grouping,
                    groupingClause = viewModel.filterClause,
                    groupingArgs = viewModel.whereFilter.value?.getSelectionArgs(true) ?: emptyArray(),
                    label = if (category.level == 0) accountInfo.label(this) else category.label,
                    type = incomeType,
                    aggregateNeutral = viewModel.aggregateNeutral.first(),
                    icon = category.icon
                )
            )
                .show(supportFragmentManager, "List")
        }
    }

    @Composable
    fun ColumnScope.LayoutHelper(
        data: @Composable (Modifier, Boolean) -> Unit,
        chart: @Composable (Modifier) -> Unit
    ) {

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(modifier = Modifier.weight(1f)) {
                    data(Modifier.weight(0.6f), false)
                    if (showChart.value) {
                        chart(
                            Modifier
                                .weight(0.4f)
                                .fillMaxSize()
                        )
                    }
                }
            }

            else -> {
                data(Modifier.weight(0.5f), !showChart.value)
                if (showChart.value) {
                    chart(
                        Modifier
                            .weight(0.5f)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}