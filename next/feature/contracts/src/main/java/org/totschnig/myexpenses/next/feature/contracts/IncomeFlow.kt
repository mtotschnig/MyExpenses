package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.next.core.model.ledger.valueOrNull
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions
import org.totschnig.myexpenses.next.data.contracts.contractTransactions

/**
 * The regular incomes and their details, e.g. as a page of the balance, which gets it from the app
 * so that it does not depend on the screens of the contracts
 */
@Composable
fun IncomeFlow(viewModel: ContractsViewModel, fixedTransactions: FixedTransactions, onBack: () -> Unit) {
    NextContractsScreen(
        state = viewModel.incomeState.collectAsStateWithLifecycle().value,
        currency = viewModel.homeCurrency.collectAsStateWithLifecycle().value,
        isIncome = true,
        onBack = onBack,
        actions = viewModel,
        news = viewModel.news.collectAsStateWithLifecycle().value,
        contractTransactions = { contract, contentModifier ->
            ContractTransactionList(viewModel, fixedTransactions, contract, contentModifier)
        }
    )
}

/** Chooses the salary the period of the balance starts with, nothing while the incomes load */
@Composable
fun SalaryChoiceSheet(viewModel: ContractsViewModel, onDismiss: () -> Unit) {
    val incomes = viewModel.incomeState.collectAsStateWithLifecycle().value.valueOrNull ?: return
    SalarySheet(
        incomes = incomes.active,
        choice = incomes.salaryChoice,
        onSelect = {
            viewModel.setSalary(it)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
