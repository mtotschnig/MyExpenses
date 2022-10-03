package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

interface ExpansionHandler {
    @Composable
    fun collapsedIds(): State<Set<String>>
    fun toggle(id: String)
}