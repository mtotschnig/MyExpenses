/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.dialog.select

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion

class SelectCrStatusDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
    val items: List<CrStatus>
        get() = buildList {
            add(CrStatus.UNRECONCILED)
            add(CrStatus.CLEARED)
            add(CrStatus.RECONCILED)
            if (requireArguments().getBoolean(KEY_WITH_VOID)) add(CrStatus.VOID)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.search_status)
            .setMultiChoiceItems(
                items.map { getString(it.toStringRes()) }.toTypedArray(),
                null,
                null
            )
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val result = items.filterIndexed { index, _ ->
            (getDialog() as AlertDialog).listView.checkedItemPositions.get(index)
        }
        if (result.isNotEmpty()) {
            (activity as SelectFilterDialog.Host).addFilterCriterion(
                CrStatusCriterion(result.toTypedArray())
            )
        }
        dismiss()
    }

    companion object {
        private const val KEY_WITH_VOID = "withVoid"
        fun newInstance(withVoid: Boolean = true): SelectCrStatusDialogFragment {
            return SelectCrStatusDialogFragment().apply {
                arguments = Bundle(1).apply {
                    putBoolean(KEY_WITH_VOID, withVoid)
                }
            }
        }
    }
}