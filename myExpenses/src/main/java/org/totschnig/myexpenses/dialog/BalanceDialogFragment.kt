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
package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.databinding.BalanceBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.UiUtils

class BalanceDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
    private var _binding: BalanceBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilderWithBinding {
            BalanceBinding.inflate(materialLayoutInflater).also { _binding = it }
        }
        UiUtils.configureAmountTextViewForHebrew(binding.TotalReconciled)
        binding.TotalReconciled.text = requireArguments().getString(DatabaseConstants.KEY_RECONCILED_TOTAL)
        UiUtils.configureAmountTextViewForHebrew(binding.TotalCleared)
        binding.TotalCleared.text = requireArguments().getString(DatabaseConstants.KEY_CLEARED_TOTAL)
        binding.balanceDelete.setOnCheckedChangeListener { _, isChecked ->
            binding.balanceDeleteWarning.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        return builder
                .setTitle(getString(R.string.dialog_title_balance_account, requireArguments().getString(DatabaseConstants.KEY_LABEL)))
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, this)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx = activity as MyExpenses? ?: return
        requireArguments().putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.BALANCE_COMMAND_DO)
        ctx.onPositive(requireArguments(), binding.balanceDelete.isChecked)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle?): BalanceDialogFragment {
            val dialogFragment = BalanceDialogFragment()
            dialogFragment.arguments = bundle
            return dialogFragment
        }
    }
}