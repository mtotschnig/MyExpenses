package org.totschnig.myexpenses.next.feature.contracts

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Period
import kotlin.math.absoluteValue
import org.totschnig.myexpenses.designsystem.Banner
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.DetailScaffold
import org.totschnig.myexpenses.designsystem.DialogActionRow
import org.totschnig.myexpenses.designsystem.InfoRow
import org.totschnig.myexpenses.designsystem.LabelChip
import org.totschnig.myexpenses.designsystem.LinkRow
import org.totschnig.myexpenses.designsystem.NameDialog
import org.totschnig.myexpenses.designsystem.SectionTitle
import org.totschnig.myexpenses.designsystem.Shapes
import org.totschnig.myexpenses.designsystem.SingleChoiceDialog
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.ui.icons.CatalogIconPicker
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberAmountFormat
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter
import org.totschnig.myexpenses.next.data.contracts.AreaChoice
import org.totschnig.myexpenses.next.data.contracts.BuiltInArea
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractArea
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.ContractTransaction
import org.totschnig.myexpenses.next.data.contracts.CustomArea
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.next.data.contracts.labelRes

/**
 * Details of one contract: amounts, next debit, the debits it was detected from and
 * further information taken from the last debit.
 */
@Composable
fun ContractDetailScreen(
    contract: Contract,
    currency: CurrencyInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the user can do with the contract, see [ContractActions] */
    actions: ContractActions = object : ContractActions {},
    /** Categories the contract can be put into */
    areas: List<ContractArea> = BuiltInArea.entries,
    /** Opens the debits the contract was detected from */
    onShowTransactions: () -> Unit = {},
    /** For incomes: whether it is the salary */
    isSalary: Boolean = false,
    onSalaryClick: () -> Unit = {},
    /** Contracts that can be joined into this one, see [ContractRuleSettings] */
    mergeCandidates: List<Contract> = emptyList(),
    /** Unread news of this contract, shown at the top */
    news: List<ContractNews> = emptyList(),
    /** All news of this contract, newest first */
    history: List<ContractNews> = emptyList(),
) {
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    if (showRenameDialog) {
        RenameContractDialog(contract, actions, onDismiss = { showRenameDialog = false })
    }
    var showAreaDialog by rememberSaveable { mutableStateOf(false) }
    if (showAreaDialog) {
        AreaDialogs(contract, areas, actions, onClose = { showAreaDialog = false })
    }
    var showIconDialog by rememberSaveable { mutableStateOf(false) }
    if (showIconDialog) {
        CatalogIconPicker(
            title = stringResource(R.string.next_icons_title, contract.title),
            selected = contract.rule?.icon,
            suggestions = contract.iconSuggestions,
            onSelect = {
                actions.setIcon(contract, it)
                showIconDialog = false
            },
            onDismiss = { showIconDialog = false },
            automatic = { AutomaticContractTile(contract) },
            automaticHint = stringResource(R.string.next_contracts_icon_automatic)
        )
    }
    DetailScaffold(onBack = onBack, modifier = modifier) {
        if (news.isNotEmpty()) {
            NewsBanner(news, currency, onCancelAgain = { actions.cancel(contract) }.takeIf { !contract.isCancelled })
        }
        if (!contract.isConfirmed) {
            SuggestionQuestion(contract.isIncome, { actions.confirm(contract) }, { actions.dismiss(contract) })
        }
        AmountsCard(
            contract, currency, isSalary,
            onRename = { showRenameDialog = true },
            onIconClick = { showIconDialog = true },
            onSalaryClick = onSalaryClick,
            onAreaClick = { showAreaDialog = true },
            onShowTransactions = onShowTransactions
        )
        DetailsSection(contract)
        contract.rule?.let { rule ->
            ContractRuleSettings(
                contract = contract,
                rule = rule,
                currency = currency,
                mergeCandidates = mergeCandidates,
                onSetInterval = { actions.setInterval(contract, it) },
                onRemovePayee = { actions.removePayee(contract, it) },
                onRemoveTargetAccount = { actions.removeTargetAccount(contract, it) },
                onSetAmountRange = { actions.setAmountRange(contract, it) },
                onMerge = { actions.merge(contract, it) },
                onSetReserve = { actions.setReserve(contract, it) }
            )
            ActionsCard(contract, actions)
        }
        if (history.isNotEmpty()) {
            NewsHistorySection(history, currency)
        }
    }
}

/**
 * Asks whether a suggestion is a contract (or regular income)
 */
/**
 * Lets the user choose the category of [contract], create a new one or edit an own one, see [AreaDialog]
 */
@Composable
private fun AreaDialogs(
    contract: Contract,
    areas: List<ContractArea>,
    actions: ContractActions,
    onClose: () -> Unit,
) {
    var showChoice by rememberSaveable { mutableStateOf(true) }
    var showCreateAreaDialog by rememberSaveable { mutableStateOf(false) }
    /** Key of the custom category being edited */
    var editedAreaKey by rememberSaveable { mutableStateOf<String?>(null) }
    // Back to the choice after editing, so that the user sees the result
    areas.filterIsInstance<CustomArea>().find { it.key == editedAreaKey }?.let { area ->
        EditAreaDialog(
            area = area,
            onRename = {
                actions.renameArea(area, it)
                editedAreaKey = null
                showChoice = true
            },
            onDelete = {
                actions.deleteArea(area)
                editedAreaKey = null
                showChoice = true
            },
            onDismiss = {
                editedAreaKey = null
                showChoice = true
            }
        )
    }
    if (showChoice) {
        AreaDialog(
            contract = contract,
            areas = areas,
            onSelect = {
                actions.setArea(contract, it)
                onClose()
            },
            onCreate = {
                showChoice = false
                showCreateAreaDialog = true
            },
            onEdit = {
                showChoice = false
                editedAreaKey = it.key
            },
            onDismiss = onClose
        )
    }
    if (showCreateAreaDialog) {
        NameDialog(
            title = stringResource(R.string.next_contracts_new_area),
            initialName = "",
            onConfirm = {
                actions.setArea(contract, AreaChoice.Fixed(actions.createArea(it)))
                onClose()
            },
            onDismiss = onClose
        )
    }
}

/**
 * Name, category or salary, amounts and dates of [contract], and the debits it was detected from
 */
@Composable
private fun AmountsCard(
    contract: Contract,
    currency: CurrencyInfo,
    isSalary: Boolean,
    onRename: () -> Unit,
    onIconClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onAreaClick: () -> Unit,
    onShowTransactions: () -> Unit,
) {
    val amount = rememberAmountFormat(currency)
    val dateFormatter = rememberDateFormatter()
    fun debit(value: Long) = (if (contract.isIncome) "+ " else "− ") + amount(value)
    DetailCard {
        Header(contract, onRename = onRename, onIconClick = onIconClick)
        val chipModifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        if (contract.isIncome) LabelChip(
            text = if (isSalary) stringResource(R.string.next_salary) else null,
            placeholder = stringResource(R.string.next_salary_set),
            onClick = onSalaryClick,
            modifier = chipModifier
        ) else LabelChip(
            text = contract.area?.label(),
            placeholder = stringResource(R.string.next_contracts_area),
            onClick = onAreaClick,
            modifier = chipModifier
        )
        InfoRow(stringResource(contract.interval.labelRes), debit(contract.lastAmount))
        contract.previousAmount?.takeIf { it != contract.lastAmount }?.let {
            InfoRow(stringResource(R.string.next_contracts_previous_amount), debit(it))
        }
        InfoRow(
            stringResource(R.string.next_contracts_total),
            debit(contract.transactions.sumOf { it.amount.absoluteValue })
        )
        contract.cancelledOn?.let {
            InfoRow(
                stringResource(
                    when {
                        contract.isIncome -> R.string.next_income_cancelled_row
                        contract.isReserve -> R.string.next_reserve_cancelled_row
                        else -> R.string.next_contracts_cancelled_row
                    }
                ),
                dateFormatter.format(it)
            )
        }
        if (contract.isActive) {
            InfoRow(
                stringResource(if (contract.isIncome) R.string.next_income_next else R.string.next_contracts_next_debit),
                dateFormatter.format(contract.nextExpectedDate)
            )
        } else {
            InfoRow(
                stringResource(R.string.next_contracts_last_debit),
                dateFormatter.format(contract.lastDate)
            )
        }
        // Payments older than the analysed period cannot be shown
        if (contract.hasPayments) LinkRow(
            pluralStringResource(
                R.plurals.next_contracts_based_on,
                contract.transactions.size,
                contract.transactions.size
            ),
            onClick = onShowTransactions
        )
    }
}

/** Account, category and purpose of the payments of [contract] */
@Composable
private fun DetailsSection(contract: Contract) {
    SectionTitle(stringResource(R.string.next_contracts_details))
    DetailCard {
        InfoRow(
            stringResource(if (contract.isIncome) R.string.next_income_account else R.string.next_contracts_account),
            contract.transactions.mapNotNull { it.accountLabel }.distinct().joinToString()
                .ifEmpty { NO_NAME },
            isFirst = true
        )
        contract.categoryPath?.let {
            InfoRow(stringResource(R.string.next_contracts_transaction_category), it)
        }
        contract.lastTransaction.comment?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(R.string.next_contracts_purpose), it)
        }
    }
}

/** Cancelling or declaring as no contract, for a confirmed contract */
@Composable
private fun ActionsCard(contract: Contract, actions: ContractActions) {
    DetailCard(Modifier.padding(top = 16.dp)) {
        if (contract.isCancelled) LinkRow(
            stringResource(
                when {
                    contract.isIncome -> R.string.next_income_uncancel
                    contract.isReserve -> R.string.next_reserve_uncancel
                    else -> R.string.next_contracts_uncancel
                }
            ),
            onClick = { actions.revokeCancellation(contract) },
            isFirst = true
        ) else LinkRow(
            stringResource(
                when {
                    contract.isIncome -> R.string.next_income_cancel
                    contract.isReserve -> R.string.next_reserve_cancel
                    else -> R.string.next_contracts_cancel
                }
            ),
            onClick = { actions.cancel(contract) },
            isFirst = true
        )
        LinkRow(
            stringResource(if (contract.isIncome) R.string.next_income_dismiss else R.string.next_contracts_dismiss),
            onClick = { actions.dismiss(contract) }
        )
    }
}

@Composable
private fun SuggestionQuestion(isIncome: Boolean, onConfirm: () -> Unit, onReject: () -> Unit) {
    Banner(
        title = stringResource(if (isIncome) R.string.next_income_question else R.string.next_contracts_question),
        messages = listOf(
            stringResource(if (isIncome) R.string.next_income_question_hint else R.string.next_contracts_question_hint)
        )
    ) {
        TextButton(onClick = onReject) {
            Text(stringResource(R.string.next_reject))
        }
        Button(onClick = onConfirm, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.next_confirm))
        }
    }
}

@Composable
private fun Header(contract: Contract, onRename: () -> Unit, onIconClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContractTile(
            contract,
            Modifier.clickable(onClickLabel = stringResource(R.string.next_icons_change), onClick = onIconClick),
            isLarge = true
        )
        Spacer(Modifier.width(16.dp))
        // Tapping the name renames the contract, the pencil shows that this is possible
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(Shapes.chip)
                .clickable(onClickLabel = stringResource(R.string.next_contracts_rename), onClick = onRename)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                contract.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

/**
 * Lets the user choose the area of [contract], or go back to the one suggested by its category
 */
@Composable
private fun AreaDialog(
    contract: Contract,
    areas: List<ContractArea>,
    onSelect: (AreaChoice) -> Unit,
    onCreate: () -> Unit,
    onEdit: (CustomArea) -> Unit,
    onDismiss: () -> Unit,
) {
    val choices = listOf(AreaChoice.Automatic, AreaChoice.Fixed(null)) + areas.map { AreaChoice.Fixed(it) }
    SingleChoiceDialog(
        title = stringResource(R.string.next_contracts_area),
        options = choices,
        selected = contract.areaChoice,
        label = { choice ->
            when (choice) {
                AreaChoice.Automatic -> stringResource(R.string.next_contracts_area_automatic, contract.suggestedArea.label())
                is AreaChoice.Fixed -> choice.area.label()
            }
        },
        onSelect = onSelect,
        onDismiss = onDismiss,
        trailing = { choice ->
            ((choice as? AreaChoice.Fixed)?.area as? CustomArea)?.let { custom ->
                IconButton(onClick = { onEdit(custom) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.next_contracts_edit_area),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        footer = { DialogActionRow(Icons.Default.Add, stringResource(R.string.next_contracts_new_area), onCreate) }
    )
}

@Preview(name = "Light", showBackground = true, heightDp = 900)
@Preview(name = "Dark", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContractDetailScreenPreview() {
    val last = LocalDate.now().minusDays(5)
    val amounts = listOf(3499L, 3499, 3499, 3999, 3999, 3999, 3999)
    NextPreviewTheme {
        ContractDetailScreen(
            contract = Contract(
                signature = "p1|MONTHLY",
                name = "Vodafone",
                interval = ContractInterval.MONTHLY,
                transactions = amounts.mapIndexed { i, amount ->
                    ContractTransaction(
                        id = i.toLong(),
                        date = last.minus(Period.ofMonths(amounts.size - 1 - i)),
                        amount = -amount,
                        accountId = 1,
                        accountLabel = "Girokonto",
                        comment = "Kundennr. 0889797",
                        categoryPath = "Wohnen > Internet & Telefon"
                    )
                },
                nextExpectedDate = last.plusMonths(1),
                isActive = true
            ),
            currency = CurrencyInfo.Preview,
            onBack = {}
        )
    }
}
