package org.totschnig.myexpenses.compose.transactions

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions

enum class Action(
    val imageVector: ImageVector,
    @param:StringRes val label: Int,
    @param:Transactions.TransactionType val type: Int = Transactions.TYPE_TRANSACTION
) {
    Expense(Icons.Default.Remove, R.string.expense),
    Income(Icons.Default.Add, R.string.income),
    Transfer(Icons.AutoMirrored.Default.ArrowForward, R.string.transfer, Transactions.TYPE_TRANSFER),
    Split(Icons.AutoMirrored.Default.CallSplit, R.string.split_transaction, Transactions.TYPE_SPLIT),
    Scan(Icons.Default.Scanner, R.string.button_scan);
}

@Composable
fun FloatingActionToolbar(
    modifier: Modifier = Modifier,
    lastAction: Action = Action.Expense,
    onAction: (action: Action) -> Unit,
) {
    val showMenu = remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    onAction(lastAction)
                },
            ) {
                Icon(
                    lastAction.imageVector,
                    contentDescription = stringResource(R.string.menu_create_transaction)
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
            )

            Box {
                val rotationAngle by animateFloatAsState(
                    targetValue = if (showMenu.value) 180F else 0F,
                    label = "DropdownArrowRotation"
                )

                // The "expand" part of the split button
                IconButton(onClick = { showMenu.value = true }) {
                    Icon(
                        modifier = Modifier.rotate(rotationAngle),
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
                    )
                }
                HierarchicalMenu(
                    expanded = showMenu,
                    menu = Action.entries.map {
                        MenuEntry(
                            icon = it.imageVector,
                            tint = Color.Unspecified,
                            label = it.label,
                            command = it.name,
                            action = { onAction(it) }
                        )
                    }
                )
            }
        }
    }
}