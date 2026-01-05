package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.MainScreen
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel

class MyExpensesV2: BaseMyExpenses<MyExpensesV2ViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MyExpensesV2ViewModel::class.java]
        with(injector) {
            inject(viewModel)
        }
        setContent {

            AppTheme {
                MainScreen(
                    viewModel,
                    onNavigateToSettings = {
                        dispatchCommand(R.id.SETTINGS_COMMAND, null)
                    },
                    onEdit = ::editAccount,
                    onDelete = ::confirmAccountDelete,
                    onSetFlag = viewModel::setFlag,
                    onToggleSealed = ::toggleAccountSealed,
                    onToggleExcludeFromTotals = ::toggleExcludeFromTotals,
                    onToggleDynamicExchangeRate = ::toggleDynamicExchangeRate,
                    flags = viewModel.accountFlags.collectAsState(emptyList()).value
                )
            }
        }
    }
}