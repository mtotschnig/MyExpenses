package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.TemplateInfo
import org.totschnig.myexpenses.viewmodel.TemplateShortcutSelectViewModel
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY

class TemplateShortcutSelect : AppCompatActivity() {
    private val viewModel: TemplateShortcutSelectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Vorlage auswählen"
        setContent {
            AppTheme {
                SelectionScreen(
                    data = viewModel.templates.collectAsState().value,
                    onCancel = {
                        finish()
                    },
                    onSubmit = {
                        val intent = if (true) Intent(this, TemplateSaver::class.java).apply {
                            action = Intent.ACTION_INSERT
                            putExtra(DatabaseConstants.KEY_TEMPLATEID, it.rowId)
                        } else Intent(this, ExpenseEdit::class.java).apply {
                            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(DatabaseConstants.KEY_TEMPLATEID, it.rowId)
                            putExtra(EXTRA_START_FROM_WIDGET, true)
                            putExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
                        }
                        val id = "template-$it"

                        val shortcut = ShortcutInfoCompat.Builder(this, id)
                            .setShortLabel(it.title)
                            .setIntent(intent)
                            .setIcon(
                                IconCompat.createWithResource(
                                    this,
                                    R.drawable.ic_action_apply_edit
                                )
                            )
                            .build()

                        setResult(
                            RESULT_OK,
                            ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SelectionScreen(
    data: List<TemplateInfo>?,
    onCancel: () -> Unit = {},
    onSubmit: (TemplateInfo) -> Unit = {}
) {
    val horizontalPadding = dimensionResource(id = R.dimen.padding_dialog_side)
    val verticalPadding = dimensionResource(id = R.dimen.padding_dialog_content_top)
    Column(
        modifier = Modifier.padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        )
    ) {
        var selectedItem by remember { mutableStateOf<TemplateInfo?>(null) }
        if (data == null) {
            Text("Loading")
        } else if (data.isEmpty()) {
            Text("No templates")
        } else {
            LazyColumn(modifier = Modifier.weight(1f, false)) {
                items(data.size) { index ->
                    val item = data[index]
                    val selected = selectedItem?.rowId == item.rowId
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth()
                            .selectable(selected) { selectedItem = item },
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(item.title)
                    }
                }
            }
        }
        Button(
            modifier = Modifier
                .padding(top = 12.dp)
                .align(Alignment.CenterHorizontally),
            onClick = {
                selectedItem?.also(onSubmit) ?: onCancel()
            }) {
            Text(
                stringResource(
                    id = if (selectedItem == null)
                        android.R.string.cancel
                    else
                        R.string.pref_shortcut_summary
                )
            )
        }
    }
}

@Preview
@Composable
fun SelectionScreenPreview() {
    SelectionScreen(
        data = listOf(
            TemplateInfo(1, "Template 1"),
            TemplateInfo(2, "Template 2")
        )
    )
}