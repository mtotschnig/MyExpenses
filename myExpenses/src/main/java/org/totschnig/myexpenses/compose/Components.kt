package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        content = content
    )
}

@Preview
@Composable
private fun ButtonRowDemo() {
    ButtonRow(modifier = Modifier.width(150.dp)) {
        TextButton(onClick = { }) {
            Text("Cancel")
        }
        TextButton(onClick = {}) {
            Text("Load")
        }
    }
}