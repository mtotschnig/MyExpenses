package org.totschnig.myexpenses.activity

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
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.viewmodel.TemplateShortcutSelectViewModel

class TemplateShortcutSelect : AppCompatActivity() {
    private val viewModel: TemplateShortcutSelectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Vorlage auswählen"
        setContent {
            Column(modifier = Modifier.padding(dimensionResource(id = androidx.appcompat.R.dimen.abc_dialog_padding_material))) {
                LazyColumnWithSelection(Modifier.weight(1f, false))
                Button(onClick = { /*TODO*/ }) {
                    Text("Add shortcut")
                }
            }
        }
    }

    @Composable
    fun LazyColumnWithSelection(modifier: Modifier) {
        var selectedRow by remember { mutableStateOf(0L) }
        val data = viewModel.templates.collectAsState()
        LaunchedEffect(data.value) {
            if (data.value?.size == 0) {
                //configure button as Cancel
            }
            if (data.value != null) {
                //enable button
            }
        }
        data.value.let { list ->
            if (list == null) {
                Text("Loading")
            } else if (list.isEmpty()) {
                Text("No templates")
            } else {
                LazyColumn(modifier = modifier) {
                    items(list.size) { index ->
                        val item = list[index]
                        val selected = selectedRow == item.first
                        Row(modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth()
                            .selectable(selected) { selectedRow = item.first },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(item.second)
                        }
                    }
                }
            }
        }

    }

}