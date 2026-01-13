package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.main.EventHandler
import org.totschnig.myexpenses.compose.main.MainScreen
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel

class MyExpensesV2 : BaseMyExpenses<MyExpensesV2ViewModel>() {

    override val drawToTopEdge = true

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
                    onEvent = object : EventHandler {
                        override fun invoke(event: AppEvent) {
                            when (event) {
                                is AppEvent.DeleteAccount -> confirmAccountDelete(event.account)
                                is AppEvent.EditAccount -> editAccount(event.account)
                                AppEvent.NavigateToSettings -> dispatchCommand(
                                    R.id.SETTINGS_COMMAND,
                                    null
                                )

                                is AppEvent.SetFlag -> viewModel.setFlag(
                                    event.accountId,
                                    event.flagId
                                )

                                is AppEvent.ToggleDynamicExchangeRate ->
                                    toggleDynamicExchangeRate(event.account)

                                is AppEvent.ToggleExcludeFromTotals -> toggleExcludeFromTotals(event.account)
                                is AppEvent.ToggleSealed -> toggleAccountSealed(event.account)
                                AppEvent.CreateAccount -> createAccount.launch(Unit)
                                is AppEvent.CreateTransaction -> createRow(
                                    event.type,
                                    event.transferEnabled,
                                    event.isIncome
                                )
                                is AppEvent.SetAccountGrouping -> viewModel.setGrouping(event.newGrouping)
                                is AppEvent.SetTransactionGrouping -> viewModel.persistGroupingV2(event.grouping)
                                is AppEvent.SetTransactionSort -> viewModel.persistSortV2(event.transactionSort)
                                is AppEvent.ToggleCrStatus -> toggleCrStatus(event.transactionId)
                            }
                        }

                        override val canToggleDynamicExchangeRate =
                            viewModel.dynamicExchangeRatesPerAccount.collectAsState(true).value

                    },
                    flags = viewModel.accountFlags.collectAsState(emptyList()).value
                )
            }
        }
    }
}