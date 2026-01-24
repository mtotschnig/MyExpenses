package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount

enum class StartScreen {
    LastVisited, Accounts, Transactions, BalanceSheet
}

/**
 * TBD: ReviewManager, AdManager, Tests, WebUI, Status Handle configuration, Upgrade Handling, New balance
 */
class MyExpensesV2 : BaseMyExpenses<MyExpensesV2ViewModel>() {

    override fun handleRootWindowInsets() {}

    override val currentAccount: BaseAccount?
        get() = viewModel.accountList.value.find { it.id == selectedAccountId }

    @get:Composable
    override val transactionListWindowInsets: WindowInsets
        get() = WindowInsets()

    override val accountCount: Int
        get() = viewModel.accountList.value.size

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
                val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value
                val availableFilters =
                    viewModel.availableGroupFilters.collectAsStateWithLifecycle().value
                when {
                    result == null || availableFilters == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    result.isFailure -> {
                        Text("Error: ${result.exceptionOrNull()}")
                    }

                    else -> {
                        val selectedAccountIdFromState = viewModel.selectedAccountId.collectAsState().value
                        LaunchedEffect(
                            viewModel.accountList.collectAsState().value.isNotEmpty(),
                            selectedAccountIdFromState
                        ) {
                            with(currentAccount) {
                                if (this != null) {
                                    sumInfo.value = SumInfo.EMPTY
                                    viewModel.sumInfo(toPageAccount(this@MyExpensesV2)).collect {
                                        sumInfo.value = it
                                    }
                                }
                            }
                        }
                        MainScreen(
                            viewModel,
                            startScreen,
                            result.getOrThrow(),
                            availableFilters,
                            selectedAccountId = selectedAccountIdFromState,
                            onAppEvent = object : AppEventHandler {
                                override fun invoke(event: AppEvent) {
                                    when (event) {

                                        AppEvent.CreateAccount -> createAccount.launch(Unit)
                                        is AppEvent.CreateTransaction -> when (event.action) {
                                            Action.Scan -> contribFeatureRequested(
                                                ContribFeature.OCR,
                                                true
                                            )

                                            else -> createRow(
                                                event.action.type,
                                                event.transferEnabled,
                                                event.action == Action.Income
                                            )
                                        }

                                        is AppEvent.SetAccountGrouping -> viewModel.setGrouping(
                                            event.newGrouping
                                        )

                                        is AppEvent.SetTransactionGrouping -> viewModel.persistGroupingV2(
                                            event.grouping
                                        )

                                        is AppEvent.SetTransactionSort -> viewModel.persistSortV2(
                                            event.transactionSort
                                        )

                                        AppEvent.PrintBalanceSheet -> printBalanceSheet()
                                        is AppEvent.ContextMenuItemClicked -> onContextItemClicked(
                                            event.itemId
                                        )

                                        AppEvent.Search -> showFilterDialog = true

                                        is AppEvent.MenuItemClicked -> dispatchCommand(
                                            event.itemId,
                                            null
                                        )

                                    }
                                }
                            },
                            onAccountEvent = object : AccountEventHandler {
                                override fun invoke(
                                    event: AccountEvent,
                                    account: FullAccount,
                                ) {
                                    when (event) {
                                        is AccountEvent.Delete -> confirmAccountDelete(account)
                                        is AccountEvent.Edit -> editAccount(account)
                                        is AccountEvent.SetFlag -> viewModel.setFlag(
                                            account.id,
                                            event.flagId
                                        )

                                        is AccountEvent.ToggleDynamicExchangeRate ->
                                            toggleDynamicExchangeRate(account)

                                        is AccountEvent.ToggleExcludeFromTotals -> toggleExcludeFromTotals(
                                            account
                                        )

                                        is AccountEvent.ToggleSealed -> toggleAccountSealed(account)
                                    }
                                }

                            },
                            onPrepareContextMenuItem = ::isContextMenuItemVisible,
                            onPrepareMenuItem = { itemId -> currentAccount.isMenuItemVisible(itemId) },
                            flags = viewModel.accountFlags.collectAsState(emptyList()).value
                        ) { pageAccount, accountCount -> Page(pageAccount, accountCount, v2 = true) }
                    }
                }
            }
        }
    }

    private val accountForNewTransaction: FullAccount?
        get() = currentAccount as? FullAccount ?: viewModel.accountDataV2.value?.getOrNull()
            ?.maxByOrNull { it.lastUsed }


    override suspend fun getEditIntent(): Intent? {
        val candidate = accountForNewTransaction
        return if (candidate != null) {
            super.getEditIntent()!!.apply {
                candidate.let {
                    putExtra(KEY_ACCOUNTID, it.id)
                    putExtra(KEY_CURRENCY, it.currency)
                    putExtra(KEY_COLOR, it.color)
                }
                val accountId = selectedAccountId
                if (isAggregate(accountId)) {
                    putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
                }
            }
        } else {
            showSnackBar(R.string.no_accounts)
            null
        }
    }
}