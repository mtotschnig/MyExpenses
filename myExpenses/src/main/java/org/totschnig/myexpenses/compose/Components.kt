package org.totschnig.myexpenses.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        content = content
    )
}

data class ButtonDefinition(
    @param:StringRes val text: Int,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun ButtonRow2(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    positiveButton: ButtonDefinition?,
    cancelEnabled: Boolean = true
) {
    val buttonRowTopPadding = 12.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = buttonRowTopPadding)
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            enabled = cancelEnabled,
            onClick = onCancel
        ) {
            Text(stringResource(id = android.R.string.cancel))
        }

        if (positiveButton != null) {
            TextButton(
                modifier = Modifier
                    .testTag(TEST_TAG_POSITIVE_BUTTON)
                    .weight(1f),
                enabled = positiveButton.enabled,
                onClick = positiveButton.onClick
            ) {
                Text(stringResource(id = positiveButton.text))
            }
        }
    }
}

@Composable
fun DialogFrame(
    title: String,
    onDismissRequest: () -> Unit = {},
    cancelEnabled: Boolean = true,
    positiveButton: ButtonDefinition?,
    content: @Composable ColumnScope.() -> Unit
) {
    val titleBottomPadding = 12.dp
    Dialog(
        onDismissRequest = { }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState())) {
                Text(
                    modifier = Modifier.padding(bottom = titleBottomPadding),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                content()
                ButtonRow2(
                    onCancel = onDismissRequest,
                    positiveButton = positiveButton,
                    cancelEnabled = cancelEnabled
                )
            }
        }
    }
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