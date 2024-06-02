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
import androidx.core.content.pm.ShortcutManagerCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.util.ShortcutHelper.buildTemplateShortcut
import org.totschnig.myexpenses.viewmodel.TemplateInfo
import org.totschnig.myexpenses.viewmodel.TemplateShortcutSelectViewModel

class TemplateShortcutSelect : AppCompatActivity() {
    private val viewModel: TemplateShortcutSelectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.select_template)
        setContent {
            AppTheme {
                SelectionScreen(
                    data = viewModel.templates.collectAsState().value,
                    onCancel = {
                        finish()
                    },
                    onSubmit = {
                        val templateShortcut = buildTemplateShortcut(this, it)
                        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
                            setResult(
                                RESULT_OK,
                                ShortcutManagerCompat.createShortcutResultIntent(
                                    this,
                                    templateShortcut
                                )
                            )
                        } else {
                            ShortcutManagerCompat.requestPinShortcut(this, templateShortcut, null)
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding,
            )
    ) {
        var selectedItem by remember { mutableStateOf<TemplateInfo?>(null) }
        if (data == null) {
            Text(stringResource(R.string.loading))
        } else if (data.isEmpty()) {
            Text(stringResource(id = R.string.no_templates))
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
                        R.string.add_shortcut
                )
            )
        }
    }
}

@Preview
@Composable
private fun SelectionScreenPreview() {
    SelectionScreen(
        data = listOf(
            TemplateInfo(1, "Template 1", Template.Action.SAVE),
            TemplateInfo(2, "Template 2", Template.Action.SAVE)
        )
    )
}