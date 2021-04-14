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
import android.text.InputType
import android.text.TextUtils
import android.util.SparseBooleanArray
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.lifecycle.ViewModelProvider
import eltos.simpledialogfragment.input.SimpleInputDialog
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.CategoryTreeBaseAdapter
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.PartyListViewModel
import org.totschnig.myexpenses.viewmodel.data.Party
import java.util.*

class PartiesList : ContextualActionBarFragment() {
    lateinit var mAdapter: ArrayAdapter<Party>
    lateinit var viewModel: PartyListViewModel

    @State
    @JvmField
    var filter: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this).get(PartyListViewModel::class.java)
        Icepick.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun withCommonContext(): Boolean {
        return action != ACTION_SELECT_FILTER
    }

    override fun dispatchCommandSingle(command: Int, info: ContextMenuInfo?): Boolean {
        if (super.dispatchCommandSingle(command, info)) {
            return true
        }
        val menuInfo = info as AdapterContextMenuInfo?
        val party = mAdapter.getItem(menuInfo!!.position)
        if (command == R.id.EDIT_COMMAND) {
            val args = Bundle()
            args.putLong(DatabaseConstants.KEY_ROWID, menuInfo.id)
            SimpleInputDialog.build()
                    .title(R.string.menu_edit_party)
                    .cancelable(false)
                    .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                    .hint(R.string.label)
                    .text(party!!.name)
                    .pos(R.string.menu_save)
                    .neut()
                    .extra(args)
                    .show(this, DIALOG_EDIT_PARTY)
            return true
        } else if (command == R.id.SELECT_COMMAND) {
            doSingleSelection(party)
            finishActionMode()
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
        if (command == R.id.DELETE_COMMAND) {
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
                        activity.showDeleteFailureFeedback()
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
            return true
        } else if (command == R.id.SELECT_COMMAND_MULTIPLE) {
            if (itemIds.size == 1 || itemIds.contains(CategoryTreeBaseAdapter.NULL_ITEM_ID)) {
                val labelList = ArrayList<String?>()
                for (i in 0 until positions.size()) {
                    if (positions.valueAt(i)) {
                        mAdapter.getItem(positions.keyAt(i))?.let { labelList.add(it.name) }
                    }
                }
                val intent = Intent()
                intent.putExtra(DatabaseConstants.KEY_PAYEEID, itemIds)
                intent.putExtra(DatabaseConstants.KEY_LABEL, TextUtils.join(",", labelList))
                activity.setResult(Activity.RESULT_FIRST_USER, intent)
                activity.finish()
            } else {
                activity.showSnackbar(R.string.unmapped_filter_only_single)
            }
            return true
        }
        return false
    }

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
        filter = if (TextUtils.isEmpty(newText)) {
            ""
        } else {
            newText
        }
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
        intent.putExtra(DatabaseConstants.KEY_PAYEEID, party!!.id)
        intent.putExtra(DatabaseConstants.KEY_LABEL, party.name)
        ctx!!.setResult(Activity.RESULT_OK, intent)
        ctx.finish()
    }

    @SuppressLint("InlinedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.parties_list, container, false)
        val lv = v.findViewById<ListView>(R.id.list)
        lv.itemsCanFocus = false
        val action = action
        if (action != ACTION_MANAGE) {
            lv.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> doSingleSelection(mAdapter.getItem(position)) }
        }
        mAdapter = object : ArrayAdapter<Party>(requireContext(), android.R.layout.simple_list_item_activated_1) {
            override fun hasStableIds(): Boolean {
                return true
            }

            override fun getItemId(position: Int): Long {
                //BUG in AbsListView https://stackoverflow.com/a/15692815/1199911
                return if (position < count) getItem(position)!!.id else -1
            }
        }
        lv.adapter = mAdapter
        lv.emptyView = v.findViewById(R.id.empty)
        registerForContextualActionBar(lv)
        viewModel.getParties().observe(viewLifecycleOwner, { parties: List<Party> ->
            with(mAdapter) {
                setNotifyOnChange(false)
                clear()
                if (action == ACTION_SELECT_FILTER) {
                    add(Party(CategoryTreeBaseAdapter.NULL_ITEM_ID, getString(R.string.unmapped), mappedTransactions = false, mappedTemplates = false))
                }
                addAll(parties)
                notifyDataSetChanged()
            }
        })
        loadData()
        return v
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
    }

    private fun hasSelectSingle(): Boolean {
        return action == ACTION_SELECT_MAPPING
    }

    private fun hasSelectMultiple(): Boolean {
        return action == ACTION_SELECT_FILTER
    }

    companion object {
        const val DIALOG_EDIT_PARTY = "dialogEditParty"
    }
}