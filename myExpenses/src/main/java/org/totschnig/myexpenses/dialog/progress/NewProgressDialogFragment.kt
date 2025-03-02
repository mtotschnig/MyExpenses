package org.totschnig.myexpenses.dialog.progress

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment3
import org.totschnig.myexpenses.viewmodel.CloseAction
import org.totschnig.myexpenses.viewmodel.CompletedAction
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.TargetAction

class NewProgressDialogFragment : ComposeBaseDialogFragment3() {

    interface Host {
        fun onAction(action: CompletedAction, index: Int? = null)
    }

    val viewmodel by activityViewModels<ModalProgressViewModel>()

    val host: Host?
        get() = requireActivity() as? Host

    @Composable
    override fun ColumnScope.MainContent() {
        val completed = viewmodel.completed.collectAsStateWithLifecycle().value
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, false),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (completed == null) {
                CircularProgressIndicator()
            }
            Text(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                text = viewmodel.message.collectAsStateWithLifecycle().value
            )
        }
        completed?.let { actions ->
            ButtonRow {
                TextButton(
                    onClick = {
                        viewmodel.onDismissMessage()
                        host?.onAction(CloseAction)
                        dismiss()
                    }
                ) {
                    Text(stringResource(R.string.menu_close))
                }
                actions.forEach {
                    if (it is TargetAction) {
                        Box {
                            val showOptions = remember { mutableStateOf(false) }
                            TextButton(
                                onClick = {
                                    if ( it.bulk || it.targets.size == 1) {
                                        host?.onAction(it)
                                    } else {
                                        showOptions.value = true
                                    }
                                }
                            ) {
                                Text(stringResource(it.label))
                            }
                            it.takeIf { !it.bulk && it.targets.size > 1 }?.let { action ->
                                HierarchicalMenu(expanded = showOptions, menu = Menu(
                                    action.targets.mapIndexed { index, uri ->
                                        MenuEntry(
                                            command = action::class.simpleName ?: "action",
                                            label = UiText.StringValue(
                                                uri.lastPathSegment ?: uri.toString()
                                            )
                                        ) { host?.onAction(action, index) }
                                    }
                                ))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder {
        return super.initBuilder().setCancelable(false)
    }

    override val title: CharSequence
        get() = requireArguments().getCharSequence(KEY_TITLE) ?: ""

    companion object {
        private const val KEY_TITLE = "title"

        fun newInstance(title: CharSequence) = NewProgressDialogFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(KEY_TITLE, title)
            }
        }

    }
}