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
package org.totschnig.myexpenses.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.EDIT_REQUEST
import org.totschnig.myexpenses.activity.EditActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.databinding.SplitPartsListBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.SplitPartListViewModel
import org.totschnig.myexpenses.viewmodel.data.Account
import javax.inject.Inject

class SplitPartList : Fragment() {
    private var _binding: SplitPartsListBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: SplitPartListViewModel
    lateinit var adapter: SplitPartRVAdapter
    private var transactionSum: Long = 0
    private var unsplitAmount: Money? = null

    @JvmField
    @State
    var parentId: Long = 0

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState == null) {
            parentId = requireArguments().getLong(DatabaseConstants.KEY_PARENTID)
        } else {
            Icepick.restoreInstanceState(this, savedInstanceState)
        }
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(this)[SplitPartListViewModel::class.java]
        appComponent.inject(viewModel)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireActivity() as ProtectedFragmentActivity
        _binding = SplitPartsListBinding.inflate(inflater, container, false)
        val currencyUnit = requireArguments().getSerializable(KEY_CURRENCY) as CurrencyUnit
        adapter = SplitPartRVAdapter(
            requireContext(),
            currencyUnit,
            currencyFormatter
        ) { view, _ -> requireActivity().openContextMenu(view) }
        binding.list.adapter = adapter
        viewModel.getSplitParts().observe(
            viewLifecycleOwner, { transactions ->
                binding.empty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
                binding.list.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
                transactionSum = transactions.sumOf { it.amountRaw }
                adapter.submitList(transactions)
                updateBalance()
            })
        loadParts()
        registerForContextMenu(binding.list)
        binding.CREATEPARTCOMMAND.contentDescription = concatResStrings(
            ctx, ". ",
            R.string.menu_create_split_part_category, R.string.menu_create_split_part_transfer
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.add(0, R.id.EDIT_COMMAND, 0, R.string.menu_edit)
        menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as ContextAwareRecyclerView.RecyclerContextMenuInfo
        return when (item.itemId) {
            R.id.EDIT_COMMAND -> {
                startActivityForResult(Intent(requireContext(), ExpenseEdit::class.java).apply {
                    putExtra(if (parentIsTemplate()) KEY_TEMPLATEID else KEY_ROWID, info.id)
                }, EDIT_REQUEST)
                true
            }
            R.id.DELETE_COMMAND -> {
                val resultObserver = Observer { result: Int ->
                    val activity = requireActivity() as EditActivity
                    if (result > 0) {
                        activity.showSnackbar(
                            activity.resources.getQuantityString(
                                R.plurals.delete_success,
                                result,
                                result
                            )
                        )
                        activity.setDirty()
                    } else {
                        activity.showDeleteFailureFeedback(null)
                    }
                }
                if (parentIsTemplate()) {
                    viewModel.deleteTemplates(longArrayOf(info.id), false)
                        .observe(viewLifecycleOwner, resultObserver)
                } else {
                    viewModel.deleteTransactions(longArrayOf(info.id), false).observe(
                        viewLifecycleOwner, resultObserver
                    )
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun parentIsTemplate(): Boolean {
        return requireArguments().getBoolean(KEY_PARENT_IS_TEMPLATE)
    }

    fun updateBalance() {
        (activity as? ExpenseEdit)?.amount?.let {
            unsplitAmount = Money(it.currencyUnit, it.amountMinor - transactionSum)
            binding.end.text = unsplitAmountFormatted
        }
    }

    val unsplitAmountFormatted
        get() = unsplitAmount?.let { currencyFormatter.formatMoney(it) }

    val splitComplete
        get() = unsplitAmount?.amountMinor == 0L

    val splitCount: Int
        get() = adapter.itemCount

    @SuppressLint("NotifyDataSetChanged")
    fun updateAccount(account: Account) {
        adapter.currencyUnit = account.currency
        adapter.notifyDataSetChanged()
        updateBalance()
    }

    fun updateParent(parentId: Long) {
        this.parentId = parentId
        loadParts()
    }

    private fun loadParts() {
        viewModel.loadSplitParts(parentId, parentIsTemplate())
    }

    companion object {
        private const val KEY_PARENT_IS_TEMPLATE = "parentIsTemplate"
        fun newInstance(
            transactionId: Long,
            isTemplate: Boolean,
            currencyUnit: CurrencyUnit
        ) = SplitPartList().apply {
            arguments = Bundle().apply {
                putLong(DatabaseConstants.KEY_PARENTID, transactionId)
                putSerializable(KEY_CURRENCY, currencyUnit)
                putBoolean(KEY_PARENT_IS_TEMPLATE, isTemplate)
            }
        }
    }
}