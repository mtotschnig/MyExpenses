package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.accounts.AccountEvent
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.MainScreen
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount

enum class StartScreen {
    LastVisited, Accounts, Transactions, BalanceSheet
}

class MyExpensesV2 : BaseMyExpenses<MyExpensesV2ViewModel>() {

    override fun handleRootWindowInsets() {}

    override val currentAccount: FullAccount?
        get() = viewModel.accountDataV2.value?.getOrNull()?.find { it.id == selectedAccountId }

    @get:Composable
    override val transactionListWindowInsets: WindowInsets
        get() = WindowInsets()

    override fun finishActionMode() {
        viewModel.selectionState.value = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MyExpensesV2ViewModel::class.java]
        with(injector) {
            inject(viewModel)
        }
        val startScreen = viewModel.startScreen.let {
            if (it == StartScreen.LastVisited) {
                prefHandler.enumValueOrDefault(PrefKey.UI_SCREEN_LAST_VISITED, StartScreen.Accounts)
            } else {
                it
            }
        }

        setContent {

            AppTheme {
                LaunchedEffect(currentAccount?.id) {
                    with(currentAccount) {
                        if (this != null) {
                            sumInfo.value = SumInfo.EMPTY
                            viewModel.sumInfo(toPageAccount).collect {
                                sumInfo.value = it
                            }
                        }
                    }
                }

                MainScreen(
                    viewModel,
                    startScreen,
                    onAppEvent = object : AppEventHandler {
                        override fun invoke(event: AppEvent) {
                            when (event) {

                                AppEvent.CreateAccount -> createAccount.launch(Unit)
                                is AppEvent.CreateTransaction -> when(event.action) {
                                    Action.Scan -> contribFeatureRequested(ContribFeature.OCR, true)
                                    else -> createRow(
                                        event.action.type,
                                        event.transferEnabled,
                                        event.action == Action.Income
                                    )
                                }

                                is AppEvent.SetAccountGrouping -> viewModel.setGrouping(event.newGrouping)
                                is AppEvent.SetTransactionGrouping -> viewModel.persistGroupingV2(
                                    event.grouping
                                )

                                is AppEvent.SetTransactionSort -> viewModel.persistSortV2(event.transactionSort)
                                AppEvent.PrintBalanceSheet -> printBalanceSheet()
                                is AppEvent.ContextMenuItemClicked -> onContextItemClicked(event.itemId)
                                AppEvent.Search -> showFilterDialog = true

                                is AppEvent.MenuItemClicked -> dispatchCommand(event.itemId, null)

                            }
                        }
                    },
                    onAccountEvent = object  : AccountEventHandler {
                        override fun invoke(
                            event: AccountEvent,
                            account: FullAccount,
                        ) {
                            when(event) {
                                is AccountEvent.Delete -> confirmAccountDelete(account)
                                is AccountEvent.Edit -> editAccount(account)
                                is AccountEvent.SetFlag -> viewModel.setFlag(
                                    account.id,
                                    event.flagId
                                )

                                is AccountEvent.ToggleDynamicExchangeRate ->
                                    toggleDynamicExchangeRate(account)

                                is AccountEvent.ToggleExcludeFromTotals -> toggleExcludeFromTotals(account)
                                is AccountEvent.ToggleSealed -> toggleAccountSealed(account)
                            }
                        }

                    },
                    onPrepareMenuItem = ::shouldShowCommand,
                    flags = viewModel.accountFlags.collectAsState(emptyList()).value
                ) { pageAccount, accountCount -> Page(pageAccount, accountCount) }
            }
        }
    }
}