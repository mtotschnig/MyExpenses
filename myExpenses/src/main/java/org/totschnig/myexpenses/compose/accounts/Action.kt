package org.totschnig.myexpenses.compose.accounts

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.main.FabMenuAction

enum class Action(
    override val imageVector: ImageVector,
    @param:StringRes override val label: Int,
) : FabMenuAction {
    AddAccount(Icons.Default.AccountBalance, R.string.menu_create_account),
    AddPortfolio(Icons.AutoMirrored.Default.ShowChart, R.string.menu_create_portfolio)
    ;

    override val contentDescription: String
        @Composable get() = stringResource(label)
}