package org.totschnig.myexpenses.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.ChoiceMode
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.myexpenses.viewmodel.LoadingState
import org.totschnig.myexpenses.viewmodel.data.Category

abstract class SelectCategoryBaseDialogFragment : ComposeBaseDialogFragment3() {
    val viewModel: CategoryViewModel by activityViewModels()

    open val withRoot: Int? = null
    open val excludedSubTree: Long? = null

    abstract val titleResId: Int

    open fun isSelectable(id: Long) = true

    abstract fun actionButtonLabel(selection: Category?): String

    abstract fun onActionButtonClick(value: Category)

    override val horizontalPadding = 0.dp

    override val title: CharSequence
        get() = getString(titleResId)

    @Composable
    override fun ColumnScope.MainContent() {
        val selectionState: MutableState<Category?> = rememberSaveable {
            mutableStateOf(null)
        }

        val state = viewModel.categoryTreeForSelect.collectAsState(initial = LoadingState.Loading)

        (state.value as? LoadingState.Data)?.let { dataState ->
            Category(
                modifier = Modifier.weight(1f),
                category = withRoot?.let { dataState.data.copy(label = stringResource(id = it)) }
                    ?: dataState.data,
                expansionMode = ExpansionMode.DefaultExpanded(rememberMutableStateListOf()),
                choiceMode = ChoiceMode.SingleChoiceMode(
                    selectionState,
                    isSelectable = ::isSelectable
                ),
                excludedSubTree = excludedSubTree,
                withRoot = withRoot != null
            )
        }

        ButtonRow(modifier = Modifier.padding(horizontal = super.horizontalPadding)) {
            TextButton(onClick = { dismiss() }) {
                Text(stringResource(id = android.R.string.cancel))
            }
            TextButton(
                onClick = {
                    onActionButtonClick(selectionState.value!!)
                    dismiss()
                },
                enabled = selectionState.value != null
            ) {
                Text(actionButtonLabel(selectionState.value))
            }
        }
    }
}