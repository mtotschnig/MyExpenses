package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.Dot
import org.totschnig.myexpenses.designsystem.GroupItem
import org.totschnig.myexpenses.designsystem.GroupPosition
import org.totschnig.myexpenses.designsystem.TextBadge
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.icon

/**
 * One contract. Contracts of a section look like one card, like the days of the transaction list.
 * A tap opens the details of the contract. In edit mode, [editAction] replaces the amount.
 */
@Composable
internal fun ContractItem(
    contract: Contract,
    currency: CurrencyInfo,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    editAction: EditAction?,
    accessibilityActions: List<CustomAccessibilityAction>,
    isFaded: Boolean,
    /** Small box in the second line, e.g. the category of the contract */
    badge: String?,
    modifier: Modifier = Modifier,
    /** For suggestions: buttons to confirm or reject them, next to the amount */
    suggestionActions: SuggestionActions? = null,
    /** Shows a dot for unread news */
    hasNews: Boolean = false,
) {
    val formatter = LocalAmountFormatter.current
    val dateFormatter = rememberDateFormatter()
    GroupItem(GroupPosition(isFirst, isLast), modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { customActions = accessibilityActions }
                .padding(start = 16.dp, end = if (editAction != null || suggestionActions != null) 4.dp else 16.dp)
                .padding(vertical = if (editAction != null) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isFaded) 0.6f else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContractTile(contract)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contract.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (hasNews) Dot(Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                contract.cancelledOn != null -> stringResource(
                                    when {
                                        contract.isIncome -> R.string.next_income_cancelled_on
                                        contract.isReserve -> R.string.next_reserve_cancelled_on
                                        else -> R.string.next_contracts_cancelled_on
                                    },
                                    dateFormatter.format(contract.cancelledOn)
                                )

                                contract.isActive ->
                                    stringResource(R.string.next_contracts_next, dateFormatter.format(contract.nextExpectedDate))

                                else -> stringResource(R.string.next_contracts_last, dateFormatter.format(contract.lastDate))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (badge != null) {
                            TextBadge(
                                badge,
                                Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f, fill = false)
                            )
                        }
                    }
                }
                if (editAction == null) {
                    PriceChangeIndicator(contract, currency)
                    Text(
                        text = (if (contract.isIncome) "+ " else "") + formatter.format(contract.lastAmount, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (contract.isIncome) LocalSemanticColors.current.income else Color.Unspecified,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (editAction != null) {
                IconButton(onClick = editAction.onClick) {
                    Icon(
                        editAction.icon,
                        contentDescription = editAction.label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (suggestionActions != null) {
                Row(Modifier.padding(start = 4.dp)) {
                    IconButton(onClick = suggestionActions.reject.onClick) {
                        Icon(
                            suggestionActions.reject.icon,
                            contentDescription = suggestionActions.reject.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = suggestionActions.confirm.onClick) {
                        Icon(
                            suggestionActions.confirm.icon,
                            contentDescription = suggestionActions.confirm.label,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Arrow if the last debit differs from the one before
 */
@Composable
private fun PriceChangeIndicator(contract: Contract, currency: CurrencyInfo) {
    val previous = contract.previousAmount ?: return
    if (previous == contract.lastAmount) return
    val increased = contract.lastAmount > previous
    Icon(
        if (increased) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
        contentDescription = stringResource(
            R.string.next_contracts_previously,
            LocalAmountFormatter.current.format(previous, currency)
        ),
        // More money going out is bad, more coming in is good
        tint = if (increased != contract.isIncome) LocalSemanticColors.current.expense else LocalSemanticColors.current.income,
        modifier = Modifier.size(16.dp)
    )
}
