package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.viewmodel.data.Transaction2

object NewTransactionRenderer : ItemRenderer {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        transaction.icon?.let { Icon(icon = it) }
        Column(modifier = Modifier.padding(horizontal = 5.dp).weight(1f)) {
            Text(text = transaction.buildPrimaryInfo(LocalContext.current))
            transaction.buildSecondaryInfo().takeIf { it.isNotEmpty() }?.let {
                Text(text = it)
            }
        }
        ColoredAmountText(money = transaction.amount)
    }
}