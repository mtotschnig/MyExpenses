package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.setFragmentResult
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.ChoiceMode
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.viewmodel.data.Category as DataCategory

class SetupCategoriesConfirmDialogFragment : ComposeBaseDialogFragment3() {

    override val horizontalPadding = 0.dp

    override val title: CharSequence
        get() = getString(R.string.menu_categories_setup_default)

    @Composable
    override fun ColumnScope.MainContent() {

        Category(
            modifier = Modifier.weight(1f),
            category = BundleCompat.getParcelable(
                requireArguments(),
                KEY_CATEGORY,
                DataCategory::class.java
            )!!,
            expansionMode = ExpansionMode.DefaultExpanded(rememberMutableStateListOf()),
            choiceMode = ChoiceMode.NoChoice
        )
        ButtonRow(modifier = Modifier.padding(horizontal = super.horizontalPadding)) {
            TextButton(onClick = { dismiss() }) {
                Text(stringResource(id = android.R.string.cancel))
            }
            TextButton(
                onClick = {
                    setFragmentResult(IMPORT_OK, Bundle())
                    dismiss()
                },
            ) {
                Text(getString(R.string.menu_import))
            }
        }
    }

    companion object {
        private const val KEY_CATEGORY = "category"
        const val IMPORT_OK = "importOK"
        fun newInstance(category: DataCategory) = SetupCategoriesConfirmDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_CATEGORY, category)
            }
        }
    }
}