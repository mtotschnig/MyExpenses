package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.viewmodel.data.Transaction2

object NewTransactionRenderer: ItemRenderer {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        transaction.icon?.let { Icon(icon = it) }
        Column {
            Text(text = transaction.label ?: "")
        }
        ColoredAmountText(money = transaction.amount)
    }
}