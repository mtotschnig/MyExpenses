package org.totschnig.myexpenses.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Functions
import androidx.compose.ui.graphics.vector.ImageVector
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.TextUtils.joinEnum

enum class BalanceType(
    @param:StringRes val resourceId: Int,
    val icon: ImageVector,
) {
    CURRENT(R.string.current_balance, Icons.Default.DragHandle),
    TOTAL(R.string.menu_aggregates, Icons.Default.Functions),
    CLEARED(R.string.total_cleared, Icons.Default.Check),
    RECONCILED(R.string.total_reconciled, Icons.Default.DoneAll);

    companion object {
        val JOIN: String = joinEnum<BalanceType>()
    }
}