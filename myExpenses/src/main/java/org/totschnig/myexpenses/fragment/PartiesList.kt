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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import eltos.simpledialogfragment.input.SimpleInputDialog
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.adapter.CategoryTreeBaseAdapter
import org.totschnig.myexpenses.adapter.ChoiceCapableAdapter
import org.totschnig.myexpenses.adapter.MultiChoiceMode
import org.totschnig.myexpenses.databinding.PartiesListBinding
import org.totschnig.myexpenses.databinding.PayeeRowBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.PartyListViewModel
import org.totschnig.myexpenses.viewmodel.data.Party
import java.util.*

class PartiesList : Fragment(), OnDialogResultListener {
    val manageParties: ManageParties?
        get() = (activity as? ManageParties)

    inner class ViewHolder(val binding: PayeeRowBinding, private val itemCallback: ItemCallback) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        init {
            binding.checkBox.setOnCheckedChangeListener(this)
        }

        override fun onClick(view: View) {
            itemCallback.onItemClick(view, bindingAdapterPosition)
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            itemCallback.onCheckedChanged(isChecked, bindingAdapterPosition)
        }

        fun bind(name: String, checked: Boolean) {
            binding.Payee.text = name
            with(binding.checkBox) {
                visibility = if (hasSelectMultiple()) View.VISIBLE else View.GONE
                isChecked = checked
            }
            binding.root.setOnClickListener(if (hasSelectMultiple()) null else this)
        }
    }

    interface ItemCallback {
        fun onItemClick(view: View, position: Int)
        fun onCheckedChanged(isChecked: Boolean, position: Int)
    }

    inner class PayeeAdapter : ChoiceCapableAdapter<Party, ViewHolder>(MultiChoiceMode(), DIFF_CALLBACK), ItemCallback {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(PayeeRowBinding.inflate(LayoutInflater.from(context), parent, false), this)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position).name, isChecked(position))
        }

        fun getParty(position: Int): Party = getItem(position)

        override fun onItemClick(view: View, position: Int) {
            with(PopupMenu(requireContext(), view)) {
                inflate(R.menu.parties_context)
                menu.findItem(
                    if (action == ACTION_SELECT_MAPPING) R.id.SELECT_COMMAND else R.id.DEBT_COMMAND
                ).isVisible = true
                setOnMenuItemClickListener { item ->
                    val party = getItem(position)
                    when (item.itemId) {
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
                                    putLong(DatabaseConstants.KEY_ROWID, party.id)
                                })
                                .show(this@PartiesList, DIALOG_EDIT_PARTY)
                        }
                        R.id.DELETE_COMMAND -> {
                            if (party.mappedTransactions || party.mappedTemplates) {
                                var message = ""
                                if (party.mappedTransactions) {
                                    message += resources.getQuantityString(
                                        R.plurals.not_deletable_mapped_transactions, 1, 1
                                    )
                                }
                                if (party.mappedTemplates) {
                                    message += resources.getQuantityString(
                                        R.plurals.not_deletable_mapped_templates, 1, 1
                                    )
                                }
                                manageParties?.showSnackbar(message)
                            } else {
                                manageParties?.showSnackbar(R.string.progress_dialog_deleting)
                                viewModel.deleteParty(party.id)
                                    .observe(viewLifecycleOwner) { result ->
                                        result.onSuccess { count ->
                                            manageParties?.let {
                                                it.showSnackbar(
                                                    it.resources.getQuantityString(
                                                        R.plurals.delete_success,
                                                        count,
                                                        count
                                                    )
                                                )
                                            }
                                        }.onFailure {
                                            manageParties?.showDeleteFailureFeedback(
                                                it.message
                                            )
                                        }
                                    }
                            }
                        }
                        R.id.DEBT_COMMAND -> {
                            startActivity(Intent(context, DebtEdit::class.java).apply {
                                putExtra(KEY_PAYEEID, party.id)
                                putExtra(KEY_PAYEE_NAME, party.name)
                            })
                        }
                        R.id.SELECT_COMMAND -> {
                            doSingleSelection(party)
                        }
                    }
                    true
                }
                //noinspection RestrictedApi
                (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
                show()
            }
        }

        override fun onCheckedChanged(isChecked: Boolean, position: Int) {
            onChecked(position, isChecked)
            manageParties?.setFabEnabled(checkedCount >=
                    if (mergeMode) 2 else if (action == ACTION_SELECT_FILTER)  1 else 0)

        }
    }

    lateinit var adapter: PayeeAdapter
    lateinit var viewModel: PartyListViewModel
    private var _binding: PartiesListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @State
    @JvmField
    var filter: String? = null

    @State
    @JvmField
    var mergeMode: Boolean = false


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (mergeMode) {
            updateUiMergeMode()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
        adapter.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity == null) return
        inflater.inflate(R.menu.search, menu)
        if (action == ACTION_MANAGE) {
            menu.add(Menu.NONE, R.id.MERGE_COMMAND, 0, R.string.menu_merge)
                .setIcon(R.drawable.ic_menu_split_transaction)
                .setCheckable(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        configureSearch(requireActivity(), menu) { newText: String? -> onQueryTextChange(newText) }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.MERGE_COMMAND) {
            mergeMode = !mergeMode
            updateUiMergeMode()
            adapter.clearChecks()
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged()
            true
        } else
            super.onOptionsItemSelected(item)

    private fun updateUiMergeMode() {
        with(manageParties!!) {
            invalidateOptionsMenu()
            configureFabMergeMode(mergeMode)
            setFabEnabled(!mergeMode)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.MERGE_COMMAND)?.let {
            it.isVisible = adapter.itemCount >= 2
            it.isChecked = mergeMode
        }
        prepareSearch(menu, filter)
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        filter = if (TextUtils.isEmpty(newText)) "" else newText
        loadData()
        return true
    }

    private fun loadData() {
        viewModel.loadParties(
            filter,
            requireActivity().intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0)
        )
    }

    private val action: String
        get() = manageParties!!.action

    private fun doSingleSelection(party: Party) {
        requireActivity().apply {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(KEY_PAYEEID, party.id)
                putExtra(DatabaseConstants.KEY_LABEL, party.name)
            })
            finish()
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PartiesListBinding.inflate(inflater, container, false)
        adapter = PayeeAdapter()
        savedInstanceState?.let { adapter.onRestoreInstanceState(it) }
        binding.list.adapter = adapter
        viewModel.getParties().observe(viewLifecycleOwner, { parties: List<Party> ->
            if (parties.size < 2 && mergeMode) {
                mergeMode = false
                updateUiMergeMode()
            }
            binding.empty.visibility = if (parties.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(if (action == ACTION_SELECT_FILTER)
                listOf(Party(
                    CategoryTreeBaseAdapter.NULL_ITEM_ID, getString(R.string.unmapped),
                    mappedTransactions = false, mappedTemplates = false, mappedDebts = 0
                )).plus(parties)
            else
                parties)
            activity?.invalidateOptionsMenu()
        })
        loadData()
        return binding.root
    }

    private fun hasSelectMultiple(): Boolean {
        return action == ACTION_SELECT_FILTER || mergeMode
    }

    companion object {
        const val DIALOG_NEW_PARTY = "dialogNewParty"
        const val DIALOG_EDIT_PARTY = "dialogEditParty"
        const val DIALOG_MERGE_PARTY = "dialogMergeParty"
        const val KEY_POSITION = "position"
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Party>() {
            override fun areItemsTheSame(oldItem: Party, newItem: Party): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Party, newItem: Party): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_NEW_PARTY, DIALOG_EDIT_PARTY -> {
                    val name = extras.getString(SimpleInputDialog.TEXT)!!
                    viewModel.saveParty(
                        extras.getLong(DatabaseConstants.KEY_ROWID),
                        name
                    ).observe(this) {
                        if (it == null)
                            manageParties?.showSnackbar(
                                getString(
                                    R.string.already_defined,
                                    name
                                )
                            )
                    }
                    return true
                }
                DIALOG_MERGE_PARTY -> {
                    val selectedItemIds = adapter.checkedPositions.map { adapter.getParty(it).id }
                    viewModel.mergeParties(selectedItemIds.toLongArray(),
                        selectedItemIds[extras.getInt(KEY_POSITION)])
                    return true
                }
            }
        }
        return false
    }

    fun dispatchFabClick() {
        if (action == ACTION_SELECT_FILTER) {
            val selected = adapter.checkedPositions.map { adapter.getParty(it) }
            val itemIds = selected.map { it.id }
            val labels = selected.map { it.name }
            if (itemIds.size != 1 && itemIds.contains(CategoryTreeBaseAdapter.NULL_ITEM_ID)) {
                manageParties?.showSnackbar(R.string.unmapped_filter_only_single)
            } else {
                requireActivity().apply {
                    setResult(Activity.RESULT_FIRST_USER, Intent().apply {
                        putExtra(KEY_PAYEEID, itemIds.toLongArray())
                        putExtra(DatabaseConstants.KEY_LABEL, labels.joinToString(separator = ","))
                    })
                    finish()
                }
            }
        } else if (mergeMode) {
            val selected = adapter.checkedPositions.map { adapter.getParty(it).name }.toTypedArray()
            SimpleFormDialog.build()
                .fields(
                    Hint.plain(R.string.merge_parties_select),
                    Spinner.plain(KEY_POSITION).items(*selected).required().preset(0)
                )
                .autofocus(false)
                .show(this, DIALOG_MERGE_PARTY)
        } else {
            SimpleInputDialog.build()
                .title(R.string.menu_create_party)
                .cancelable(false)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                .hint(R.string.full_name)
                .pos(R.string.dialog_button_add)
                .neut()
                .show(this, DIALOG_NEW_PARTY)
        }
    }
}