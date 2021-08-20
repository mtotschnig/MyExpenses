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
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.adapter.CategoryTreeBaseAdapter
import org.totschnig.myexpenses.databinding.PartiesListBinding
import org.totschnig.myexpenses.databinding.PayeeRowBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.PartyListViewModel
import org.totschnig.myexpenses.viewmodel.data.Party
import java.util.*

class PartiesList : Fragment(), OnDialogResultListener {
    var parties: MutableList<Party> = mutableListOf()
    inner class ViewHolder(val binding: PayeeRowBinding) : RecyclerView.ViewHolder(binding.root)

    inner class PayeeAdapter: RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(PayeeRowBinding.inflate(LayoutInflater.from(context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.Payee.text = parties[position].name
        }

        override fun getItemCount() = parties.size

    }
    lateinit var mAdapter: RecyclerView.Adapter<ViewHolder>
    lateinit var viewModel: PartyListViewModel
    private var _binding: PartiesListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @State
    @JvmField
    var filter: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this).get(PartyListViewModel::class.java)
        (requireActivity().application as MyApplication).appComponent.inject(viewModel)
        Icepick.restoreInstanceState(this, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }
/*
    override fun dispatchCommandSingle(command: Int, info: ContextMenuInfo?): Boolean {
        if (super.dispatchCommandSingle(command, info)) {
            return true
        }
        val menuInfo = info as AdapterContextMenuInfo
        mAdapter.getItem(menuInfo.position)?.let { party ->
            when (command) {
                R.id.EDIT_COMMAND -> {
                    SimpleInputDialog.build()
                            .title(R.string.menu_edit_party)
                            .cancelable(false)
                            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            .hint(R.string.full_name)
                            .text(party.name)
                            .pos(R.string.menu_save)
                            .neut()
                            .extra(Bundle().apply {
                                putLong(DatabaseConstants.KEY_ROWID, menuInfo.id)
                            })
                            .show(this, DIALOG_EDIT_PARTY)
                }
                R.id.SELECT_COMMAND -> {
                    doSingleSelection(party)
                    finishActionMode()
                }
                R.id.DEBT_COMMAND -> {
                    finishActionMode()
                    startActivity(Intent(context, DebtEdit::class.java).apply {
                        putExtra(KEY_PAYEEID, party.id)
                        putExtra(KEY_PAYEE_NAME, party.name)
                    })
                }
            }
            return true
        }
        return false
    }

    override fun dispatchCommandMultiple(command: Int,
                                         positions: SparseBooleanArray, itemIds: LongArray): Boolean {
        if (super.dispatchCommandMultiple(command, positions, itemIds)) {
            return true
        }
        val activity = requireActivity() as ProtectedFragmentActivity
        return when (command) {
            R.id.MERGE_COMMAND -> {
                val selected = with(positions.asTrueSequence().iterator()) { Array(itemIds.size) { (mAdapter.getItem(next()) as Party).name } }
                SimpleFormDialog.build()
                        .fields(
                                Hint.plain(R.string.merge_parties_select),
                                Spinner.plain(KEY_PAYEEID).items(*selected).required().preset(0))
                        .autofocus(false)
                        .show(this, DIALOG_MERGE_PARTY)
                true
            }
            R.id.DELETE_COMMAND -> {
                var hasMappedTransactionsCount = 0
                var hasMappedTemplatesCount = 0
                val idList = mutableListOf<Long>()
                for (i in 0 until positions.size()) {
                    if (positions.valueAt(i)) {
                        var deletable = true
                        mAdapter.getItem(positions.keyAt(i))?.let {
                            if (it.mappedTransactions) {
                                hasMappedTransactionsCount++
                                deletable = false
                            }
                            if (it.mappedTemplates) {
                                hasMappedTemplatesCount++
                                deletable = false
                            }
                            if (deletable) {
                                idList.add(it.id)
                            }
                        }
                    }
                }
                if (idList.isNotEmpty()) {
                    activity.showSnackbar(R.string.progress_dialog_deleting)
                    viewModel.deleteParties(idList).observe(viewLifecycleOwner) { result ->
                        result.onSuccess {
                            activity.showSnackbar(activity.resources.getQuantityString(R.plurals.delete_success, it, it))
                        }.onFailure {
                            activity.showDeleteFailureFeedback(it.message)
                        }
                    }
                }
                if (hasMappedTransactionsCount > 0 || hasMappedTemplatesCount > 0) {
                    var message = ""
                    if (hasMappedTransactionsCount > 0) {
                        message += resources.getQuantityString(
                                R.plurals.not_deletable_mapped_transactions,
                                hasMappedTransactionsCount,
                                hasMappedTransactionsCount)
                    }
                    if (hasMappedTemplatesCount > 0) {
                        message += resources.getQuantityString(
                                R.plurals.not_deletable_mapped_templates,
                                hasMappedTemplatesCount,
                                hasMappedTemplatesCount)
                    }
                    activity.showSnackbar(message)
                }
                true
            }
            R.id.SELECT_COMMAND_MULTIPLE -> {
                if (itemIds.size != 1 && itemIds.contains(CategoryTreeBaseAdapter.NULL_ITEM_ID)) {
                    activity.showSnackbar(R.string.unmapped_filter_only_single)
                } else {
                    val labelList = ArrayList<String?>()
                    for (i in 0 until positions.size()) {
                        if (positions.valueAt(i)) {
                            mAdapter.getItem(positions.keyAt(i))?.let { labelList.add(it.name) }
                        }
                    }
                    val intent = Intent()
                    intent.putExtra(KEY_PAYEEID, itemIds)
                    intent.putExtra(DatabaseConstants.KEY_LABEL, TextUtils.join(",", labelList))
                    activity.setResult(Activity.RESULT_FIRST_USER, intent)
                    activity.finish()
                }
                true
            }
            else -> false
        }
    }*/

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity == null) return
        inflater.inflate(R.menu.search, menu)
        configureSearch(requireActivity(), menu) { newText: String? -> onQueryTextChange(newText) }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        prepareSearch(menu, filter)
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        filter = if (TextUtils.isEmpty(newText)) "" else newText
        loadData()
        return true
    }

    private fun loadData() {
        viewModel.loadParties(filter, requireActivity().intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0))
    }

    private val action: String
        get() = (activity as ManageParties?)!!.action

    private fun doSingleSelection(party: Party?) {
        val ctx: Activity? = activity
        val intent = Intent()
        intent.putExtra(KEY_PAYEEID, party!!.id)
        intent.putExtra(DatabaseConstants.KEY_LABEL, party.name)
        ctx!!.setResult(Activity.RESULT_OK, intent)
        ctx.finish()
    }

    @SuppressLint("InlinedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PartiesListBinding.inflate(inflater, container, false)
        val action = action
/*        if (action != ACTION_MANAGE) {
            binding.list.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> doSingleSelection(mAdapter.getItem(position)) }
        }*/
        mAdapter = PayeeAdapter()
        binding.list.adapter = mAdapter
        //binding.list.emptyView = binding.empty
        //registerForContextualActionBar(binding.list)
        viewModel.getParties().observe(viewLifecycleOwner, { parties: List<Party> ->
            with(this@PartiesList.parties) {
                clear()
                if (action == ACTION_SELECT_FILTER) {
                    add(Party(CategoryTreeBaseAdapter.NULL_ITEM_ID, getString(R.string.unmapped),
                        mappedTransactions = false, mappedTemplates = false, mappedDebts = 0))
                }
                addAll(parties)
                mAdapter.notifyDataSetChanged()
            }
        })
        loadData()
        return binding.root
    }

/*    override fun configureMenu(menu: Menu, lv: AbsListView) {
        super.configureMenu(menu, lv)
        menu.findItem(R.id.MERGE_COMMAND).isVisible = action == ACTION_MANAGE && lv.checkedItemCount >= 2
    }

    override fun inflateContextualActionBar(menu: Menu, listId: Int) {
        super.inflateContextualActionBar(menu, listId)
        val inflater = requireActivity().menuInflater
        if (hasSelectSingle()) {
            inflater.inflate(R.menu.select, menu)
        }
        if (hasSelectMultiple()) {
            inflater.inflate(R.menu.select_multiple, menu)
        }
    }*/

    private fun hasSelectSingle(): Boolean {
        return action == ACTION_SELECT_MAPPING
    }

    private fun hasSelectMultiple(): Boolean {
        return action == ACTION_SELECT_FILTER
    }

    companion object {
        const val DIALOG_EDIT_PARTY = "dialogEditParty"
        const val DIALOG_MERGE_PARTY = "dialogMergeParty"
    }

   override fun onResult(dialogTag: String, which: Int, extras: Bundle) = true
    /*     if (dialogTag == DIALOG_MERGE_PARTY && which == OnDialogResultListener.BUTTON_POSITIVE) {
            val index = extras.getInt(KEY_PAYEEID)
            val position = binding.list.checkedItemPositions.asTrueSequence().elementAt(index)
            val selected = mAdapter.getItem(position) as Party
            viewModel.mergeParties(binding.list.checkedItemIds, selected.id)
            true
        } else false*/
}