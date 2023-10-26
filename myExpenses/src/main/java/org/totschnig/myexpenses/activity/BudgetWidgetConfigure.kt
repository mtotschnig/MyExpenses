package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.widget.BudgetWidget


class BudgetWidgetConfigure : BaseWidgetConfigure() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {
                var selectedItem by remember { mutableStateOf<Long?>(null) }
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
                    val data = viewModel.data.collectAsState(null).value
                    if (data == null) {
                        Text(stringResource(R.string.loading))
                    } else if (data.isEmpty()) {
                        Text(stringResource(id = R.string.no_budgets))
                    } else {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            items(data) { item ->
                                Row(
                                    modifier = Modifier.clickable { selectedItem = item.id },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedItem == item.id,
                                        onClick = null
                                    )
                                    Text(
                                        text = item.titleComplete(this@BudgetWidgetConfigure),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .align(Alignment.CenterHorizontally),
                        onClick = {
                            if (data?.isEmpty() == true) {
                                startActivity(
                                    Intent(
                                        this@BudgetWidgetConfigure,
                                        BudgetEdit::class.java
                                    )
                                )
                            } else {
                                selectedItem?.let { item ->
                                    appWidgetId?.let { widget ->
                                        saveSelectionPref(this@BudgetWidgetConfigure, widget, item)
                                        apply(BudgetWidget::class.java)
                                    }
                                } ?: run {
                                    finish()
                                }
                            }
                        }) {
                        if (data?.isEmpty() == true) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.menu_create_budget)
                            )
                        } else {
                            Text(
                                stringResource(
                                    id = if (selectedItem == null)
                                        android.R.string.cancel
                                    else
                                        R.string.add_widget
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "budget_widget"

        fun saveSelectionPref(context: Context, appWidgetId: Int, id: Long) {
            sharedPreferences(context).edit {
                putLong(selectionKey(appWidgetId), id)
            }
        }

        fun loadSelectionPref(context: Context, appWidgetId: Int) =
            sharedPreferences(context).getLong(selectionKey(appWidgetId), Long.MAX_VALUE)

        private fun selectionKey(appWidgetId: Int) = "BUDGET_WIDGET_SELECTION_$appWidgetId"

        private fun sharedPreferences(context: Context) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun clearPreferences(context: Context, appWidgetId: Int) {
            sharedPreferences(context).edit {
                remove(selectionKey(appWidgetId))
            }
        }
    }
}