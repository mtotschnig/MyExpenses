package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.util.Optional

enum class StartScreen {
    LastVisited, Accounts, Transactions, BalanceSheet
}

/**
 * TBD: ReviewManager, AdManager, Tests, WebUI, Status Handle configuration, Upgrade Handling,
 * New balance, Manage types and flags, Help, Reconciliation, Tell a friend,
 * Copy balance to clipboard, Budget progress, Bank icon
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
            if (it == StartScreen.LastVisited)
                prefHandler.enumValueOrDefault(PrefKey.UI_SCREEN_LAST_VISITED, StartScreen.Accounts)
            else it
        }

        setContent {
            AppTheme {
                val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value
                val availableFilters =
                    viewModel.availableGroupFilters.collectAsStateWithLifecycle().value
                when {
                    result?.isFailure == true -> {
                        val (message, forceQuit) = result.exceptionOrNull()!!
                            .processDataLoadingFailure()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(R.dimen.padding_main_screen)),
                            // These two lines replace the Box's contentAlignment and the Column's horizontalAlignment
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(message)
                            // Applying spacing manually since we are using Arrangement.Center
                            // which takes precedence over Arrangement.spacedBy
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!forceQuit) {
                                Button(onClick = {
                                    dispatchCommand(
                                        R.id.SAFE_MODE_COMMAND,
                                        null
                                    )
                                }) {
                                    Text(stringResource(R.string.safe_mode))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Button(onClick = { dispatchCommand(R.id.QUIT_COMMAND, null) }) {
                                Text(stringResource(R.string.button_label_close))
                            }
                        }

                    }

                    result == null || availableFilters == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        val selectedAccountIdFromState =
                            viewModel.selectedAccountId.collectAsState().value
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
                        val accounts = result.getOrThrow()
                        val banks = viewModel.banks.collectAsState()
                        MainScreen(
                            viewModel,
                            startScreen,
                            accounts,
                            availableFilters,
                            selectedAccountId = selectedAccountIdFromState,
                            onAppEvent = object : AppEventHandler {
                                override fun invoke(event: AppEvent) {
                                    when (event) {

                                        AppEvent.CreateAccount -> createAccount()
                                        is AppEvent.CreateTransaction ->
                                            if (preCreateRowCheckForSealed()) {
                                                when (event.action) {
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
                            flags = viewModel.accountFlags.collectAsState(emptyList()).value,
                            bankIcon = { modifier, id ->
                                banks.value.find { it.id == id }
                                    ?.let { bank ->
                                        bankingFeature.bankIconRenderer?.invoke(
                                            modifier,
                                            bank
                                        )
                                    }
                            }
                        ) { pageAccount -> Page(pageAccount, accounts.size, v2 = true) }
                    }
                }
            }
        }
    }

    override suspend fun accountForNewTransaction() = Optional.ofNullable(
        currentAccount as? FullAccount ?:
        viewModel.accountDataV2.value?.getOrNull()?.maxByOrNull { it.lastUsed }
    )
}