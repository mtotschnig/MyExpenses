package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.ChoiceMode
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

class SelectCategoryMoveTargetDialogFragment : ComposeBaseDialogFragment() {
    lateinit var viewModel: CategoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]
    }

    @Composable
    override fun BuildContent() {
        val category = requireArguments().getParcelable<Category>(KEY_SOURCE)!!
        Column(modifier = Modifier.padding(8.dp)) {
            val selectionState: MutableState<Category?> = rememberSaveable {
                mutableStateOf(null)
            }

            Text(
                style = MaterialTheme.typography.h6,
                text = stringResource(id = R.string.dialog_title_select_target)
            )

            Category(
                modifier = Modifier.weight(1f),
                category = viewModel.categoryTree.collectAsState(initial = Category.EMPTY).value,
                expansionState = null,
                choiceMode = ChoiceMode.SingleChoiceMode(selectionState, false)
            )

            Button(modifier = Modifier.align(Alignment.End), onClick = { dismiss() }) {
                Text("Move ${category.label} to ${selectionState.value?.label ?: "?"}")
            }
        }
    }

    companion object {
        const val KEY_SOURCE = "source"
        fun newInstance(category: Category) = SelectCategoryMoveTargetDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable("source", category)
            }
        }
    }
}