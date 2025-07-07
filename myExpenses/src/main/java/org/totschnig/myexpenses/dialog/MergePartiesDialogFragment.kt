package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.fragment.PartiesList.Companion.DIALOG_MERGE_PARTY
import org.totschnig.myexpenses.viewmodel.MergeStrategy

class MergePartiesDialogFragment : ComposeBaseDialogFragment3() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ColumnScope.MainContent() {
        val options = requireArguments().getStringArray(KEY_PARTY_LIST)!!
        var expanded by remember { mutableStateOf(false) }
        var selectedPartyIndex by rememberSaveable { mutableIntStateOf(0) }
        var selectedMergeStrategy by rememberSaveable { mutableStateOf(MergeStrategy.DELETE) }
        val shape = if (expanded) RoundedCornerShape(8.dp).copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp)
        )
        else RoundedCornerShape(8.dp)

        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                value = options[selectedPartyIndex],
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = shape,
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedIndicatorColor = Transparent,
                    unfocusedIndicatorColor = Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEachIndexed { index, s ->
                    DropdownMenuItem(
                        text = { Text(s) },
                        onClick = {
                            selectedPartyIndex = index
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        Column(Modifier.selectableGroup()) {
            MergeStrategy.entries.forEach { strategy ->
                val isSelected = strategy == selectedMergeStrategy
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { selectedMergeStrategy = strategy },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null // null recommended for accessibility with screen readers
                    )
                    Text(
                        text = stringResource(id = strategy.description),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
        ButtonRow {
            TextButton(onClick = {
                (requireActivity() as BaseActivity).startActionView(
                    "https://faq.myexpenses.mobi/parties-merge"
                )
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(id = R.string.menu_help))
            }
            TextButton(onClick = {
                setFragmentResult(
                    DIALOG_MERGE_PARTY,
                    bundleOf(
                        KEY_POSITION to selectedPartyIndex,
                        KEY_STRATEGY to selectedMergeStrategy
                    )
                )
                dismiss()
            }) {
                Text(stringResource(id = R.string.menu_merge))
            }
        }
    }

    override val title: CharSequence
        get() = getString(R.string.merge_parties_select)

    companion object {
        const val KEY_PARTY_LIST = "partyList"
        const val KEY_POSITION = "position"
        const val KEY_STRATEGY = "strategy"
        fun newInstance(options: Array<String>) = MergePartiesDialogFragment().apply {
            arguments = Bundle().apply {
                putStringArray(KEY_PARTY_LIST, options)
            }
        }
    }
}