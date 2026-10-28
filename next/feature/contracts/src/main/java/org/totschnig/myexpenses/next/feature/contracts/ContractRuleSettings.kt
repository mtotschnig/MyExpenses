package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.DestructiveTextButton
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.EditableRow
import org.totschnig.myexpenses.designsystem.FormDialog
import org.totschnig.myexpenses.designsystem.LinkRow
import org.totschnig.myexpenses.designsystem.PickDialog
import org.totschnig.myexpenses.designsystem.RemovableRow
import org.totschnig.myexpenses.designsystem.SectionTitle
import org.totschnig.myexpenses.designsystem.SingleChoiceDialog
import org.totschnig.myexpenses.designsystem.SwitchRow
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractRule
import org.totschnig.myexpenses.next.data.contracts.labelRes

/**
 * Which payments belong to a confirmed contract, as defined by its [ContractRule]:
 * interval, payees and amount range can be changed, and other contracts can be joined into it.
 *
 * @param mergeCandidates contracts and suggestions that can be joined into this one
 */
@Composable
internal fun ContractRuleSettings(
    contract: Contract,
    rule: ContractRule,
    currency: CurrencyInfo,
    mergeCandidates: List<Contract>,
    onSetInterval: (ContractInterval) -> Unit,
    onRemovePayee: (Long) -> Unit,
    onRemoveTargetAccount: (Long) -> Unit = {},
    onSetAmountRange: (LongRange?) -> Unit,
    onMerge: (Contract) -> Unit,
    onSetReserve: (Boolean) -> Unit = {},
) {
    var showIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var showAmountRangeDialog by rememberSaveable { mutableStateOf(false) }
    var showMergeDialog by rememberSaveable { mutableStateOf(false) }
    val formatter = LocalAmountFormatter.current

    if (showIntervalDialog) {
        IntervalDialog(
            current = rule.interval,
            onSelect = {
                onSetInterval(it)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
    if (showAmountRangeDialog) {
        AmountRangeDialog(
            range = rule.amountRange,
            currency = currency,
            onConfirm = {
                onSetAmountRange(it)
                showAmountRangeDialog = false
            },
            onDismiss = { showAmountRangeDialog = false }
        )
    }
    if (showMergeDialog) {
        MergeDialog(
            isIncome = contract.isIncome,
            candidates = mergeCandidates,
            currency = currency,
            onSelect = {
                onMerge(it)
                showMergeDialog = false
            },
            onDismiss = { showMergeDialog = false }
        )
    }

    SectionTitle(stringResource(R.string.next_contracts_matching))
    DetailCard {
        EditableRow(
            label = stringResource(R.string.next_contracts_interval),
            value = stringResource(rule.interval.labelRes),
            onClick = { showIntervalDialog = true },
            isFirst = true
        )
        val payeeLabel = stringResource(if (contract.isIncome) R.string.next_income_payers else R.string.next_contracts_payees)
        val removePayeeLabel =
            stringResource(if (contract.isIncome) R.string.next_income_remove_payer else R.string.next_contracts_remove_payee)
        val accountLabel = stringResource(R.string.next_contracts_target_account)
        val removeAccountLabel = stringResource(R.string.next_contracts_remove_target_account)
        // At least one payee, account or the template has to remain, or the rule would match nothing
        val canRemove = rule.payeeIds.size + rule.targetAccountIds.size > 1 || rule.templateId != null
        rule.payeeIds.sortedBy { it }.forEach { payeeId ->
            RemovableRow(
                label = payeeLabel,
                value = contract.transactions.firstOrNull { it.payeeId == payeeId && it.targetAccountId == null }?.payeeName
                    ?: NO_NAME,
                removeLabel = removePayeeLabel.takeIf { canRemove },
                onRemove = { onRemovePayee(payeeId) }
            )
        }
        rule.targetAccountIds.sortedBy { it }.forEach { accountId ->
            RemovableRow(
                label = accountLabel,
                // Movements are named after the account they go to
                value = contract.transactions.firstOrNull { it.targetAccountId == accountId }?.payeeName ?: NO_NAME,
                removeLabel = removeAccountLabel.takeIf { canRemove },
                onRemove = { onRemoveTargetAccount(accountId) }
            )
        }
        if (!contract.isIncome) {
            SwitchRow(
                label = stringResource(R.string.next_reserve),
                checked = contract.isReserve,
                onCheckedChange = onSetReserve,
                supportingText = stringResource(
                    if (contract.reserveChoice == null) R.string.next_reserve_automatic else R.string.next_reserve_hint
                )
            )
        }
        rule.amountRange?.let { range ->
            EditableRow(
                label = stringResource(R.string.next_contracts_amount_range),
                value = stringResource(
                    R.string.next_contracts_amount_range_value,
                    formatter.format(range.first, currency),
                    formatter.format(range.last, currency)
                ),
                onClick = { showAmountRangeDialog = true }
            )
        }
        LinkRow(
            stringResource(if (contract.isIncome) R.string.next_income_merge else R.string.next_contracts_merge),
            onClick = { showMergeDialog = true }
        )
    }
}

@Composable
internal fun IntervalDialog(
    current: ContractInterval,
    onSelect: (ContractInterval) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.next_contracts_interval),
) {
    SingleChoiceDialog(
        title = title,
        options = ContractInterval.entries,
        selected = current,
        label = { stringResource(it.labelRes) },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

/**
 * Plain decimal number for editing, e.g. "7.19" for 719 minor units in EUR
 */
private fun Long.toDecimalString(currency: CurrencyInfo) =
    BigDecimal.valueOf(this, currency.fractionDigits).toPlainString()

private fun String.toMinorUnits(currency: CurrencyInfo): Long? =
    trim().replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it.signum() >= 0 }
        ?.movePointRight(currency.fractionDigits)
        ?.takeIf { it.stripTrailingZeros().scale() <= 0 }
        ?.toLong()

@Composable
private fun AmountRangeDialog(
    range: LongRange?,
    currency: CurrencyInfo,
    onConfirm: (LongRange?) -> Unit,
    onDismiss: () -> Unit,
) {
    var min by rememberSaveable { mutableStateOf(range?.first?.toDecimalString(currency) ?: "") }
    var max by rememberSaveable { mutableStateOf(range?.last?.toDecimalString(currency) ?: "") }
    val minValue = min.toMinorUnits(currency)
    val maxValue = max.toMinorUnits(currency)
    val isValid = minValue != null && maxValue != null && minValue <= maxValue
    FormDialog(
        title = stringResource(R.string.next_contracts_amount_range),
        onConfirm = { onConfirm(minValue!!..maxValue!!) },
        onDismiss = onDismiss,
        confirmEnabled = isValid,
        intro = stringResource(R.string.next_contracts_amount_range_hint)
    ) {
        OutlinedTextField(
            value = min,
            onValueChange = { min = it },
            label = { Text(stringResource(R.string.next_contracts_amount_min)) },
            suffix = { Text(currency.symbol) },
            singleLine = true,
            isError = min.isNotEmpty() && minValue == null,
            supportingText = if (min.isNotEmpty() && minValue == null) {
                { Text(stringResource(R.string.next_amount_invalid)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = max,
            onValueChange = { max = it },
            label = { Text(stringResource(R.string.next_contracts_amount_max)) },
            suffix = { Text(currency.symbol) },
            singleLine = true,
            isError = max.isNotEmpty() && (maxValue == null || minValue != null && maxValue < minValue),
            supportingText = when {
                max.isEmpty() -> null
                maxValue == null -> {
                    { Text(stringResource(R.string.next_amount_invalid)) }
                }
                minValue != null && maxValue < minValue -> {
                    { Text(stringResource(R.string.next_contracts_amount_max_below_min)) }
                }
                else -> null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        DestructiveTextButton(stringResource(R.string.next_contracts_amount_range_remove), onClick = { onConfirm(null) })
    }
}

@Composable
private fun MergeDialog(
    isIncome: Boolean,
    candidates: List<Contract>,
    currency: CurrencyInfo,
    onSelect: (Contract) -> Unit,
    onDismiss: () -> Unit,
) {
    val formatter = LocalAmountFormatter.current
    val suggestion = stringResource(R.string.next_contracts_suggestion)
    PickDialog(
        title = stringResource(if (isIncome) R.string.next_income_merge else R.string.next_contracts_merge),
        options = candidates,
        label = { it.title },
        onSelect = onSelect,
        onDismiss = onDismiss,
        intro = stringResource(if (isIncome) R.string.next_income_merge_hint else R.string.next_contracts_merge_hint),
        supportingText = { stringResource(it.interval.labelRes) + if (it.isConfirmed) "" else " · $suggestion" },
        value = { formatter.format(it.lastAmount, currency) },
        emptyText = stringResource(R.string.next_contracts_merge_empty)
    )
}
