package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.service.BudgetWidgetUpdateWorker
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.widget.BudgetWidget


class BudgetWidgetConfigure : BaseWidgetConfigure() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {
                var selectedItemPosition by remember { mutableStateOf<Int?>(null) }
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
                            itemsIndexed(data) { index, item ->
                                Row(
                                    modifier = Modifier.clickable { selectedItemPosition = index },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedItemPosition == index,
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
                                selectedItemPosition?.let { position ->
                                    appWidgetId?.let { widget ->
                                        val budget = data!![position]
                                        saveSelectionPref(
                                            this@BudgetWidgetConfigure,
                                            widget,
                                            budget
                                        )
                                        apply(BudgetWidget::class.java)
                                        BudgetWidgetUpdateWorker.enqueueSelf(
                                            applicationContext,
                                            budget.grouping,
                                        )
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
                                    id = if (selectedItemPosition == null)
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

        fun saveSelectionPref(context: Context, appWidgetId: Int, budget: Budget) {
            sharedPreferences(context).edit {
                putLong(selectionKey(appWidgetId), budget.id)
                saveGrouping(appWidgetId, budget.grouping)
            }
        }

        fun saveGrouping(context: Context, appWidgetId: Int, grouping: Grouping) {
            sharedPreferences(context).edit {
                saveGrouping(appWidgetId, grouping)
            }
        }

        private fun SharedPreferences.Editor.saveGrouping(appWidgetId: Int, grouping: Grouping) {
            putString(selectionKeyGrouping(appWidgetId), grouping.name)
        }

        fun savePeriod(context: Context, appWidgetId: Int, year: Int, second: Int) {
            sharedPreferences(context).edit {
                putInt(selectionKeyYear(appWidgetId), year)
                putInt(selectionKeySecond(appWidgetId), second)
            }
        }

        private fun SharedPreferences.budgetId(appWidgetId: Int) =
            getLong(selectionKey(appWidgetId), Long.MAX_VALUE)

        fun loadBudgetId(context: Context, appWidgetId: Int) =
            sharedPreferences(context).budgetId(appWidgetId)

        fun loadGrouping(context: Context, appWidgetId: Int) =
            Grouping.valueOf(loadGroupingString(context, appWidgetId))

        fun loadGroupingString(context: Context, appWidgetId: Int) = sharedPreferences(context)
            .getString(selectionKeyGrouping(appWidgetId), Grouping.NONE.name)!!

        fun loadPeriod(context: Context, appWidgetId: Int) = with(sharedPreferences(context)) {
            getInt(selectionKeyYear(appWidgetId), 0).takeIf { it > 0 }?.let {
                it to getInt(selectionKeySecond(appWidgetId), 0)
            }
        }

        fun clearPeriod(context: Context, appWidgetId: Int) {
            sharedPreferences(context).edit {
                clearPeriod(appWidgetId)
            }
        }

        private fun SharedPreferences.Editor.clearPeriod(appWidgetId: Int) {
            remove(selectionKeyYear(appWidgetId))
            remove(selectionKeySecond(appWidgetId))
        }

        private fun selectionKey(appWidgetId: Int) = "BUDGET_WIDGET_SELECTION_$appWidgetId"
        private fun selectionKeyGrouping(appWidgetId: Int) = "BUDGET_WIDGET_GROUPING_$appWidgetId"

        private fun selectionKeyYear(appWidgetId: Int) = "BUDGET_WIDGET_YEAR_$appWidgetId"
        private fun selectionKeySecond(appWidgetId: Int) = "BUDGET_WIDGET_SECOND_$appWidgetId"


        private fun sharedPreferences(context: Context) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun clearPreferences(context: Context, appWidgetId: Int) {
            BudgetWidgetUpdateWorker.enqueueSelf(
                context,
                loadGrouping(context, appWidgetId)
            )
            sharedPreferences(context).edit {
                remove(selectionKey(appWidgetId))
                remove(selectionKeyGrouping(appWidgetId))
                clearPeriod(appWidgetId)
            }
        }
    }
}