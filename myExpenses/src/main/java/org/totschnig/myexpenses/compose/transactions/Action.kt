package org.totschnig.myexpenses.compose.transactions

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.main.FabMenuAction
import org.totschnig.myexpenses.contract.TransactionsContract

enum class Action(
    override val imageVector: ImageVector,
    @param:StringRes override val label: Int,
    @param:TransactionsContract.Transactions.TransactionType val type: Int = TransactionsContract.Transactions.TYPE_TRANSACTION,
) : FabMenuAction {
    Expense(Icons.Default.Remove, R.string.expense),
    Income(Icons.Default.Add, R.string.income),
    Transfer(
        Icons.AutoMirrored.Default.ArrowForward,
        R.string.transfer,
        TransactionsContract.Transactions.TYPE_TRANSFER
    ),
    Split(
        Icons.AutoMirrored.Default.CallSplit,
        R.string.split_transaction,
        TransactionsContract.Transactions.TYPE_SPLIT
    ),
    Scan(Icons.Default.Scanner, R.string.button_scan),
    Buy(Icons.Default.ArrowUpward, R.string.trade_buy),
    Sell(Icons.Default.ArrowDownward, R.string.trade_sell);


    val tint: Color?
        @Composable get() = when (this) {
            Expense -> LocalColors.current.expense
            Income -> LocalColors.current.income
            Transfer -> LocalColors.current.transfer
            else -> null
        }


    override val contentDescription: String
        @Composable get() = when (this) {
            Expense, Income -> stringResource(R.string.menu_create_transaction) + " (" + stringResource(
                label
            ) + ")"

            Transfer -> stringResource(R.string.menu_create_transfer)
            Split -> stringResource(R.string.menu_create_split)
            else -> stringResource(label)
        }

    companion object {
        val PORTFOLIO_ACTIONS = listOf(Buy, Sell)
        val STANDARD_ACTIONS = listOf(Expense, Income, Transfer, Split, Scan)
    }
}