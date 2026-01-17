package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.EventHandler
import org.totschnig.myexpenses.compose.main.MainScreen
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel.SelectionInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount

enum class StartScreen {
    LastVisited, Accounts, Transactions, BalanceSheet
}

class MyExpensesV2 : BaseMyExpenses<MyExpensesV2ViewModel>() {

    override fun handleRootWindowInsets() {}

    override val currentAccount: FullAccount?
        get() = viewModel.accountDataV2.value?.getOrNull()?.find { it.id == selectedAccountId }

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

                MainScreen(
                    viewModel,
                    startScreen,
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
                                is AppEvent.SetTransactionGrouping -> viewModel.persistGroupingV2(
                                    event.grouping
                                )

                                is AppEvent.SetTransactionSort -> viewModel.persistSortV2(event.transactionSort)
                                is AppEvent.ToggleCrStatus -> toggleCrStatus(event.transactionId)
                                AppEvent.PrintBalanceSheet -> printBalanceSheet()
                                is AppEvent.ShowDetails -> {
                                    showDetails(
                                        event.transaction.id,
                                        event.transaction.isArchive,
                                        currentFilter.takeIf { event.transaction.isArchive },
                                        currentAccount?.sortOrder.takeIf { event.transaction.isArchive }
                                    )
                                }

                                is AppEvent.Unarchive -> unarchive(event.transactionId)
                                is AppEvent.Delete -> lifecycleScope.launch {
                                    if (event.transaction.isArchive) {
                                        deleteArchive(event.transaction)
                                    } else {
                                        delete(listOf(event.transaction.id to event.transaction.crStatus))
                                    }
                                }

                                is AppEvent.Edit -> edit(event.transaction, event.clone)
                                is AppEvent.CreateTemplate -> createTemplate(event.transaction)
                                is AppEvent.UnDelete -> undelete(listOf(event.transactionId))
                                is AppEvent.Select -> viewModel.selectionState.value =
                                    listOf(SelectionInfo(event.transaction))

                                is AppEvent.Ungroup -> ungroupSplit(event.transaction)
                                is AppEvent.Unlink -> unlinkTransfer(event.transaction)
                                is AppEvent.TransformToTransfer -> transformToTransfer(event.transaction)
                                is AppEvent.AddFilter -> addFilterCriterion(event.criterion)
                                is AppEvent.ContextMenuItemClicked -> onContextItemClicked(event.itemId)
                                AppEvent.SelectAllListTooLarge -> selectAllListTooLarge()
                            }
                        }
                    },
                    rendererFactory,
                    onPrepareMenuItem = ::shouldShowCommand,
                    flags = viewModel.accountFlags.collectAsState(emptyList()).value
                )
            }
        }
    }
}