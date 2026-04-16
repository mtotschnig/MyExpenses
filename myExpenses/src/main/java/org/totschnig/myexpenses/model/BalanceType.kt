package org.totschnig.myexpenses.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Functions
import androidx.compose.ui.graphics.vector.ImageVector
import org.totschnig.myexpenses.util.TextUtils.joinEnum

enum class BalanceType(
    val icon: ImageVector,
) {
    CURRENT(Icons.Default.DragHandle),
    TOTAL(Icons.Default.Functions),
    CLEARED(Icons.Default.Check),
    RECONCILED(Icons.Default.DoneAll),
    DELTA(Icons.Default.ChangeHistory);

    companion object {
        val JOIN: String = joinEnum<BalanceType>()
    }
}