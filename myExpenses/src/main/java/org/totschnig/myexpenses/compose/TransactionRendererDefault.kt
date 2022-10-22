package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.viewmodel.data.Transaction2

object TransactionRendererDefault: ItemRenderer {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        Text(transaction.icon ?: "ICON")
        ColoredAmountText(money = transaction.amount)
    }
}