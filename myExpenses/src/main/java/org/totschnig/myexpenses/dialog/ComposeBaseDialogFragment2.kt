package org.totschnig.myexpenses.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import org.totschnig.myexpenses.compose.AppTheme

/**
 * variant of [ComposeBaseDialogFragment] which is not affected by https://issuetracker.google.com/issues/194754697
 * We live without the material dialogue styling for the moment.
 */
abstract class ComposeBaseDialogFragment2: BaseDialogFragment() {

    val dialogPadding = 24.dp

    @Composable
    abstract fun BuildContent()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                BuildContent()
            }
        }
    }
}