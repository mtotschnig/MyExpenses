package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface
import android.net.Uri
import androidx.fragment.app.activityViewModels
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import org.apache.commons.lang3.ArrayUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel

class SelectHiddenAccountDialogFragment : SelectMultipleDialogFragment(false) {
    private val viewModel: MyExpensesViewModel by activityViewModels()

    override val dialogTitle: Int
        get() = R.string.menu_hidden_accounts
    override val uri: Uri
        get() = TransactionProvider.ACCOUNTS_URI
    override val column: String
        get() = DatabaseConstants.KEY_LABEL

    override fun onResult(labelList: List<String>, itemIds: LongArray, which: Int): Boolean {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                if (itemIds.isNotEmpty()) {
                    viewModel.setAccountVisibility(false, *itemIds)
                }
                return true
            }
            DialogInterface.BUTTON_NEUTRAL -> {
                if (itemIds.isNotEmpty()) {
                    val message = Stream.of(labelList)
                        .map { label: String? -> getString(R.string.warning_delete_account, label) }
                        .collect(Collectors.joining(" ")) + " " + getString(R.string.continue_confirmation)
                    MessageDialogFragment.newInstance(
                        resources.getQuantityString(
                            R.plurals.dialog_title_warning_delete_account,
                            itemIds.size,
                            itemIds.size
                        ),
                        message,
                        MessageDialogFragment.Button(
                            R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                            ArrayUtils.toObject(itemIds)
                        ),
                        null,
                        MessageDialogFragment.noButton(), 0
                    )
                        .show(childFragmentManager, "DELETE_ACCOUNTS")
                    return false
                }
                return true
            }
        }
        return false
    }

    override val selection: String
        get() = DatabaseConstants.KEY_HIDDEN + " = 1"
    override val positiveButton: Int
        get() = R.string.button_label_show
    override val neutralButton: Int
        get() = R.string.menu_delete
    override val negativeButton: Int
        get() = 0

    companion object {
        @JvmStatic
        fun newInstance(): SelectHiddenAccountDialogFragment {
            return SelectHiddenAccountDialogFragment()
        }
    }
}