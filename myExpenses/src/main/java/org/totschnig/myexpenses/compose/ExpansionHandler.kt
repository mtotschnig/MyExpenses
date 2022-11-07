package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow

interface ExpansionHandler {
    val collapsedIds: Flow<Set<String>?>
    fun toggle(id: String)
}