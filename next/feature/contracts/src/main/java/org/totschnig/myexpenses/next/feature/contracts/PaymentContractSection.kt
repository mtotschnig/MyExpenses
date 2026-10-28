package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.designsystem.ActionChip
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.InfoRow
import org.totschnig.myexpenses.designsystem.LinkRow
import org.totschnig.myexpenses.designsystem.SectionTitle
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.PaymentContractInfo
import org.totschnig.myexpenses.next.data.contracts.icon

/** What is known about the contract of [transaction], transfers are not analysed */
@Composable
private fun rememberPaymentInfo(viewModel: ContractsViewModel, transaction: ShownTransaction): PaymentContractInfo {
    val info by remember(transaction.id) { viewModel.paymentInfo(transaction.id) }
        .collectAsStateWithLifecycle(if (transaction.isTransfer) PaymentContractInfo.Unavailable else PaymentContractInfo.Loading)
    return info
}

/**
 * Part of the details of a transaction: the contract (or regular income) it belongs to, if there
 * is one or one is suggested. Declaring it as one is offered by [PaymentContractChip].
 */
@Composable
fun PaymentContractSection(viewModel: ContractsViewModel, transaction: ShownTransaction) {
    PaymentContractSection(
        info = rememberPaymentInfo(viewModel, transaction),
        onConfirm = viewModel::confirm,
        onRestore = viewModel::restore
    )
}

@Composable
fun PaymentContractSection(
    info: PaymentContractInfo,
    onConfirm: (Contract) -> Unit,
    onRestore: (Contract) -> Unit,
) {
    val contract = when (info) {
        is PaymentContractInfo.Confirmed -> info.contract
        is PaymentContractInfo.Suggested -> info.contract
        is PaymentContractInfo.Dismissed -> info.contract
        else -> return
    }
    val isIncome = contract.isIncome
    SectionTitle(stringResource(if (isIncome) R.string.next_income_title else R.string.next_tab_contracts))
    DetailCard {
        when (info) {
            is PaymentContractInfo.Suggested -> {
                InfoRow(stringResource(R.string.next_payment_suggested), contract.title, isFirst = true)
                LinkRow(
                    stringResource(if (isIncome) R.string.next_income_confirm else R.string.next_contracts_confirm),
                    onClick = { onConfirm(contract) }
                )
            }

            is PaymentContractInfo.Dismissed -> {
                InfoRow(
                    stringResource(if (isIncome) R.string.next_income_dismiss else R.string.next_contracts_dismiss),
                    contract.title,
                    isFirst = true
                )
                LinkRow(
                    stringResource(if (isIncome) R.string.next_income_confirm else R.string.next_contracts_confirm),
                    onClick = { onRestore(contract) }
                )
            }

            else -> InfoRow(stringResource(R.string.next_payment_part_of), contract.title, isFirst = true)
        }
    }
}

/**
 * Chip for the [org.totschnig.myexpenses.designsystem.ActionChipRow] below the details of a
 * transaction that is not part of a contract yet: declares it as one, after asking for the interval
 */
@Composable
fun PaymentContractChip(viewModel: ContractsViewModel, transaction: ShownTransaction) {
    val info = rememberPaymentInfo(viewModel, transaction)
    PaymentContractChip(info) { viewModel.markAsContract(transaction.id, it) }
}

@Composable
fun PaymentContractChip(info: PaymentContractInfo, onMark: (ContractInterval) -> Unit) {
    if (info !is PaymentContractInfo.Markable) return
    var showIntervalDialog by rememberSaveable { mutableStateOf(false) }
    if (showIntervalDialog) {
        IntervalDialog(
            current = ContractInterval.MONTHLY,
            title = stringResource(R.string.next_payment_interval_question),
            onSelect = {
                onMark(it)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
    ActionChip(
        text = stringResource(if (info.isIncome) R.string.next_payment_mark_income else R.string.next_payment_mark_contract),
        icon = Icons.Default.EventRepeat,
        onClick = { showIntervalDialog = true }
    )
}
