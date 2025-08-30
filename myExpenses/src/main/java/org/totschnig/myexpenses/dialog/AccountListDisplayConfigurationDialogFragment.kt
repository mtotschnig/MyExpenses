package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.SortableItem
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.CheckBoxWithLabel
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.triggerAccountListRefresh
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel

class AccountListDisplayConfigurationDialogFragment : ComposeBaseDialogFragment3() {

    val viewModel: ContentResolvingAndroidViewModel by viewModels()

    val accountGroupingKey: Preferences.Key<String>
        get() = prefHandler.getStringPreferencesKey(PrefKey.ACCOUNT_GROUPING)

    val sortByFlagKey: Preferences.Key<Boolean>
        get() = prefHandler.getBooleanPreferencesKey(PrefKey.SORT_ACCOUNT_LIST_BY_FLAG_FIRST)



    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(viewModel)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ColumnScope.MainContent() {

        var selectedGrouping by rememberSaveable {
            mutableStateOf(AccountGrouping.TYPE)
        }
        var sortByFlagFirst by rememberSaveable {
            mutableStateOf(false)
        }

        LaunchedEffect(Unit) {
            with(dataStore.data.first()) {
                selectedGrouping =  enumValueOrDefault(
                    this[accountGroupingKey],
                    AccountGrouping.TYPE
                )
                sortByFlagFirst = this[sortByFlagKey] != false
            }
        }

        var selectedSort by rememberSaveable {
            mutableStateOf(
                prefHandler.enumValueOrDefault(
                    PrefKey.SORT_ORDER_ACCOUNTS,
                    Sort.USAGES
                )
            )
        }

        Text(stringResource(R.string.menu_grouping), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        RadioGroupSection(
            options = AccountGrouping.entries.map { it.title },
            selectedIndex = selectedGrouping.ordinal,
            onOptionSelected = { selectedGrouping = AccountGrouping.entries[it] }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.display_options_sort_list_by),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        CheckBoxWithLabel(
            label = "Sort by flag first",
            checked = sortByFlagFirst
        ) {
            sortByFlagFirst = it
        }
        val scope = rememberCoroutineScope()
        RadioGroupSection(
            options = Sort.accountSortLabels,
            selectedIndex = Sort.accountSort.indexOf(selectedSort),
            onOptionSelected = { selectedSort = Sort.accountSort[it] },
            editableIndex = Sort.accountSort.indexOf(Sort.CUSTOM) to {
                scope.launch {
                    SortUtilityDialogFragment.newInstance(
                        ArrayList(
                            viewModel.accountsMinimal(withAggregates = false, sortOrder = KEY_SORT_KEY).first()
                                .map { SortableItem(it.id, it.label) }
                        ))
                        .show(childFragmentManager, "SORT_ACCOUNTS")
                }
            }
        )
        ButtonRow {
            TextButton(onClick = {
                lifecycleScope.launch {
                    prefHandler.putString(PrefKey.SORT_ORDER_ACCOUNTS, selectedSort.name)
                    dataStore.edit { preference ->
                        preference[accountGroupingKey] = selectedGrouping.name
                        preference[sortByFlagKey] = sortByFlagFirst
                    }
                    requireContext().contentResolver.triggerAccountListRefresh()
                    dismiss()
                }
            }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    }
}

@Composable
private fun RadioGroupSection(
    options: List<Int>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    editableIndex: Pair<Int, () -> Unit>? = null
) {
    Column(modifier.selectableGroup()) {
        options.forEachIndexed { index, text ->
            val selected = index == selectedIndex
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onOptionSelected(index) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 0.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )
                Text(
                    text = stringResource(text),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
                if (editableIndex?.first == index && selected) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = editableIndex.second) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.menu_edit)
                        )
                    }
                }
            }
        }
    }
}
