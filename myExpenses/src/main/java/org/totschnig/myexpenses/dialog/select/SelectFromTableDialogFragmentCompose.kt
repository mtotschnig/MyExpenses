package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.widget.AbsListView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment
import org.totschnig.myexpenses.viewmodel.LoadState
import org.totschnig.myexpenses.viewmodel.SelectFromTableViewModel

abstract class SelectFromTableDialogFragmentCompose(withNullItem: Boolean) :
    ComposeBaseDialogFragment(), DialogInterface.OnClickListener {

    private val dataViewModel: SelectFromTableViewModel by viewModels()

    protected open val dialogTitle: Int = 0
    abstract val uri: Uri
    abstract val column: String
    protected open val selectionArgs: Array<String>? = null
    protected open val selection: String? = null
    protected open val neutralButton: Int
        get() = 0
    protected open val negativeButton: Int
        get() = android.R.string.cancel
    protected open val positiveButton: Int
        get() = android.R.string.ok
    protected open val choiceMode: Int
        get() = AbsListView.CHOICE_MODE_MULTIPLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(dataViewModel)
        dataViewModel.loadData(uri, column, selection, selectionArgs)
    }

    override fun initBuilder(): AlertDialog.Builder = super.initBuilder().apply {
        if (dialogTitle != 0) setTitle(dialogTitle)
        setPositiveButton(positiveButton, null)
        if (neutralButton != 0) setNeutralButton(neutralButton, null)
        if (negativeButton != 0) setNegativeButton(negativeButton, null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    onClick(this, AlertDialog.BUTTON_POSITIVE)
                }
                val neutral = getButton(AlertDialog.BUTTON_NEUTRAL)
                neutral?.setOnClickListener {
                    onClick(this, AlertDialog.BUTTON_NEUTRAL)
                }
            }
        }

    @Composable
    override fun BuildContent() {
        when (val data = dataViewModel.data.value) {
            LoadState.Loading -> TODO()
            is LoadState.Result -> {
                if (data.items.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.dialogPadding()) {
                        items(data.items.size) {
                            val dataHolder = data.items[it]
                            fun toggle() {
                                dataViewModel.selectionState.toggle(dataHolder)
                            }
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clickable(onClick = ::toggle),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(modifier = Modifier.weight(1f),
                                    text = dataHolder.label)
                                val selected = dataViewModel.selectionState.value.contains(dataHolder)
                                if (choiceMode == AbsListView.CHOICE_MODE_MULTIPLE) {
                                    Checkbox(checked = selected, onCheckedChange = { toggle() })
                                } else {
                                    RadioButton(selected = selected, onClick = ::toggle)
                                }
                            }
                        }
                    }
                } else {
                    Text("No Data", modifier = Modifier.dialogPadding())
                }
            }
        }
    }

    fun Modifier.dialogPadding() = padding(start = 24.dp, end = 24.dp, top = 0.dp)

    abstract override fun onClick(dialog: DialogInterface, which: Int)

    companion object {
        private const val KEY_CHECKED_POSITIONS = "checked_positions"
        const val KEY_DIALOG_TITLE = "dialog_tile"
        const val KEY_EMPTY_MESSAGE = "empty_message"
        const val NULL_ITEM_ID = 0L
        protected const val EMPTY_ITEM_ID = -1L
    }
}