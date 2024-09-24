package org.totschnig.myexpenses.dialog.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment3
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel

class NewProgressDialogFragment : ComposeBaseDialogFragment3() {
    val viewmodel by activityViewModels<ModalProgressViewModel>()

    @Composable
    override fun ColumnScope.MainContent() {
        val completed = viewmodel.completed.collectAsStateWithLifecycle().value
        Row(modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!completed) {
                CircularProgressIndicator()
            }
            Text(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                text = viewmodel.message.collectAsStateWithLifecycle().value
            )
        }
        ButtonRow {
            Button(
                enabled = completed,
                onClick = {
                    dismiss()
                }
            ) {
                Text("OK")
            }
        }
    }

    override val title: CharSequence
        get() = "Not yet implemented"
}