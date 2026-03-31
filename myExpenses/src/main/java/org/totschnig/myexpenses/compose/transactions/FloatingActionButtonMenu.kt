package org.totschnig.myexpenses.compose.transactions

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.calculateOnColor
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions

enum class Action(
    val imageVector: ImageVector,
    @param:StringRes val label: Int,
    @param:Transactions.TransactionType val type: Int = Transactions.TYPE_TRANSACTION,
) {
    Expense(Icons.Default.Remove, R.string.expense),
    Income(Icons.Default.Add, R.string.income),
    Transfer(
        Icons.AutoMirrored.Default.ArrowForward,
        R.string.transfer,
        Transactions.TYPE_TRANSFER
    ),
    Split(
        Icons.AutoMirrored.Default.CallSplit,
        R.string.split_transaction,
        Transactions.TYPE_SPLIT
    ),
    Scan(Icons.Default.Scanner, R.string.button_scan);


    val tint: Color?
        @Composable get() = when (this) {
            Expense -> LocalColors.current.expense
            Income -> LocalColors.current.income
            Transfer -> LocalColors.current.transfer
            else -> null
        }


    val contentDescription: String
        @Composable get() = when (this) {
            Expense, Income -> stringResource(R.string.menu_create_transaction) + " (" + stringResource(
                label
            ) + ")"

            Transfer -> stringResource(R.string.menu_create_transfer)
            Split -> stringResource(R.string.menu_create_split)
            Scan -> stringResource(label)
        }
}


@Composable
fun FloatingActionButtonMenu(
    isStandard: Boolean = true,
    lastAction: Action = Action.Expense,
    containerColor: Color,
    onAction: (Action) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = if (expanded) CircleShape else FloatingActionButtonDefaults.shape,
        color = containerColor,
        contentColor = containerColor.calculateOnColor(),
        shadowElevation = 6.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                    .combinedClickable(
                        onClick = {
                            if (expanded) expanded = false else onAction(lastAction)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = !expanded
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val rotation by animateFloatAsState(if (expanded) 45f else 0f)
                Icon(
                    imageVector = if (expanded) Icons.Default.Add else lastAction.imageVector,
                    contentDescription = lastAction.contentDescription,
                    modifier = Modifier
                        .rotate(rotation)
                )
            }

            if (isStandard && !expanded) {

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp),
                    color = LocalContentColor.current
                )

                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = !expanded
                        }
                        .padding(start = 8.dp, end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropUp,
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description),
                    )
                }
            }
        }
    }
    if (expanded) {
        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ) = IntOffset(
                    anchorBounds.right - popupContentSize.width,
                    anchorBounds.top - popupContentSize.height
                )

            },
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Action.entries.reversed().forEach { action ->
                    Surface(
                        onClick = {
                            onAction(action)
                            expanded = false
                        },
                        shape = CircleShape,
                        color = containerColor,
                        contentColor = containerColor.calculateOnColor(),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(action.imageVector, null, Modifier.size(20.dp))
                            Text(
                                stringResource(action.label),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}