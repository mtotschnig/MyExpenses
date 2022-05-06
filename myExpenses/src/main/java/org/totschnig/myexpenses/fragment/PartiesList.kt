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
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import eltos.simpledialogfragment.input.SimpleInputDialog
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.*
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.DebtOverview
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.databinding.PartiesListBinding
import org.totschnig.myexpenses.databinding.PayeeRowBinding
import org.totschnig.myexpenses.dialog.DebtDetailsDialogFragment
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.PartyListViewModel
import org.totschnig.myexpenses.viewmodel.data.Party
import javax.inject.Inject
import kotlin.math.sign

class PartiesList : Fragment(), OnDialogResultListener {

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    val manageParties: ManageParties
        get() = (activity as ManageParties)

    private var mergeMenuItem: MenuItem? = null

    inner class ViewHolder(val binding: PayeeRowBinding, private val itemCallback: ItemCallback) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        init {
            binding.checkBox.setOnCheckedChangeListener(this)
        }

        override fun onClick(view: View) {
            itemCallback.onItemClick(binding, bindingAdapterPosition)
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            itemCallback.onCheckedChanged(isChecked, bindingAdapterPosition)
        }

        fun bind(party: PartyWrapper, isChecked: Boolean) {
            binding.Payee.text = party.party.name
            with(binding.checkBox) {
                visibility = if (hasSelectMultiple()) View.VISIBLE else View.GONE
                this.isChecked = isChecked
            }
            binding.Debt.visibility = if (party.hasOpenDebts) View.VISIBLE else View.GONE
            binding.root.setOnClickListener(this)
        }
    }

    interface ItemCallback {
        fun onItemClick(binding: PayeeRowBinding, position: Int)
        fun onCheckedChanged(isChecked: Boolean, position: Int)
    }

    inner class PartyWrapper(val party: Party) {
        val hasOpenDebts =
            viewModel.getDebts(party.id)?.any { !it.isSealed && it.currentBalance != 0L } == true
    }

    inner class PayeeAdapter :
        ListAdapter<PartyWrapper, ViewHolder>(DIFF_CALLBACK), ItemCallback {
        private var checkStates: MutableSet<Long> = mutableSetOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(PayeeRowBinding.inflate(LayoutInflater.from(context), parent, false), this)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(getParty(position)) {
                holder.bind(this, checkStates.contains(this.party.id))
            }
        }

        override fun onCurrentListChanged(
            previousList: MutableList<PartyWrapper>,
            currentList: MutableList<PartyWrapper>
        ) {
            updateFabEnabled()
        }

        private fun getParty(position: Int): PartyWrapper = getItem(position)

        fun getSelected(): List<PartyWrapper> =
            currentList.filter { checkStates.contains(it.party.id) }

        val checkedCount: Int
            get() = getSelected().size

        override fun onItemClick(binding: PayeeRowBinding, position: Int) {
            if (hasSelectMultiple()) {
                binding.checkBox.toggle()
                return
            }
            val index2IdMap: MutableMap<Int, Long> = mutableMapOf()
            with(PopupMenu(requireContext(), binding.root)) {
                if (action == ACTION_SELECT_MAPPING) {
                    menu.add(Menu.NONE, SELECT_COMMAND, Menu.NONE, R.string.select)
                        .setIcon(R.drawable.ic_menu_done)
                }
                menu.add(Menu.NONE, EDIT_COMMAND, Menu.NONE, R.string.menu_edit)
                    .setIcon(R.drawable.ic_menu_edit)
                menu.add(Menu.NONE, DELETE_COMMAND, Menu.NONE, R.string.menu_delete)
                    .setIcon(R.drawable.ic_menu_delete)
                if (action == ACTION_MANAGE) {
                    val debts = viewModel.getDebts(getItem(position).party.id)
                    val subMenu = if (debts?.size ?: 0 > 0)
                        menu.addSubMenu(Menu.NONE, DEBT_SUB_MENU, Menu.NONE, R.string.debts)
                            .setIcon(R.drawable.balance_scale) else menu
                    debts?.forEachIndexed { index, debt ->
                        index2IdMap[index] = debt.id
                        val menuTitle = TextUtils.concat(
                            debt.label,
                            " ",
                            currencyFormatter.formatMoney(Money(debt.currency, debt.currentBalance))
                                .withAmountColor(resources, debt.currentBalance.sign)
                        )
                        val item = subMenu.add(Menu.NONE, index, Menu.NONE, menuTitle)
                        if (debt.isSealed) {
                            item.setIcon(R.drawable.ic_lock)
                        }
                    }
                    subMenu.add(Menu.NONE, NEW_DEBT_COMMAND, Menu.NONE, R.string.menu_new_debt)
                        .setIcon(R.drawable.ic_menu_add)
                }

                setOnMenuItemClickListener { item ->
                    val party = getItem(position).party
                    when (item.itemId) {
                        EDIT_COMMAND -> {
                            SimpleInputDialog.build()
                                .title(R.string.menu_edit_party)
                                .cancelable(false)
                                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                                .hint(R.string.full_name)
                                .text(party.name)
                                .pos(R.string.menu_save)
                                .neut()
                                .extra(Bundle().apply {
                                    putLong(KEY_ROWID, party.id)
                                })
                                .show(this@PartiesList, DIALOG_EDIT_PARTY)
                        }
                        DELETE_COMMAND -> {
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
                                manageParties.showSnackBar(message)
                            } else if (party.mappedDebts) {
                                SimpleDialog.build()
                                    .title(R.string.dialog_title_warning_delete_party)
                                    .extra(Bundle().apply {
                                        putLong(KEY_ROWID, party.id)
                                    })
                                    .msg(
                                        org.totschnig.myexpenses.util.TextUtils.concatResStrings(
                                            requireContext(),
                                            " ",
                                            R.string.warning_party_delete_debt,
                                            R.string.continue_confirmation
                                        )
                                    )
                                    .pos(R.string.response_yes)
                                    .neg(R.string.response_no)
                                    .show(this@PartiesList, DIALOG_DELETE_PARTY)
                            } else {
                                doDelete(party.id)
                            }
                        }
                        SELECT_COMMAND -> {
                            doSingleSelection(party)
                        }
                        DEBT_SUB_MENU -> { /*submenu*/
                        }
                        NEW_DEBT_COMMAND -> {
                            startActivity(Intent(context, DebtEdit::class.java).apply {
                                putExtra(KEY_PAYEEID, party.id)
                                putExtra(KEY_PAYEE_NAME, party.name)
                            })
                        }
                        else -> {
                            index2IdMap[item.itemId]?.also {
                                DebtDetailsDialogFragment.newInstance(it).show(
                                    parentFragmentManager, DIALOG_DEBT_DETAILS
                                )
                            } ?: run {
                                CrashHandler.report(IllegalStateException("debtId not found in map"))
                            }
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
            val party = getParty(position).party
            if (isChecked) {
                checkStates.add(party.id)
            } else {
                checkStates.remove(party.id)
            }
            updateFabEnabled()
        }

        fun onSaveInstanceState(state: Bundle) {
            state.putLongArray(STATE_CHECK_STATES, checkStates.toTypedArray().toLongArray())
        }

        fun onRestoreInstanceState(state: Bundle) {
            state.getLongArray(STATE_CHECK_STATES)?.let {
                checkStates = mutableSetOf(*it.toTypedArray())
            }
        }

        fun clearSelection() {
            checkStates.clear()
        }
    }

    private fun updateFabEnabled() {
        manageParties.setFabEnabled(
            adapter.checkedCount >=
                    if (mergeMode) 2 else if (action == ACTION_SELECT_FILTER) 1 else 0
        )
    }

    private fun doDelete(partyId: Long) {
        manageParties.showSnackBar(R.string.progress_dialog_deleting)
        viewModel.deleteParty(partyId)
            .observe(viewLifecycleOwner) { result ->
                result.onSuccess { count ->
                    manageParties.let {
                        it.showSnackBar(
                            it.resources.getQuantityString(
                                R.plurals.delete_success,
                                count,
                                count
                            )
                        )
                    }
                }.onFailure {
                    manageParties.showDeleteFailureFeedback(it.message)
                }
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
        viewModel = ViewModelProvider(this)[PartyListViewModel::class.java]
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@PartiesList)
            inject(viewModel)
        }
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
            mergeMenuItem = menu.add(Menu.NONE, R.id.MERGE_COMMAND, 0, R.string.menu_merge).apply {
                setIcon(R.drawable.ic_menu_split_transaction)
                isCheckable = true
            }
            menu.add(Menu.NONE, R.id.DEBT_COMMAND, 0, R.string.title_activity_debt_overview)
                .setIcon(R.drawable.balance_scale)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        configureSearch(requireActivity(), menu, ::onQueryTextChange)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.MERGE_COMMAND -> {
                mergeMode = !mergeMode
                updateUiMergeMode()
                resetAdapter()
                true
            }
            R.id.DEBT_COMMAND -> {
                startActivity(Intent(context, DebtOverview::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun updateUiMergeMode() {
        with(manageParties) {
            mergeMenuItem?.isChecked = mergeMode
            configureFabMergeMode(mergeMode)
            setFabEnabled(!mergeMode)
        }
    }

    private fun resetAdapter() {
        //noinspection NotifyDataSetChanged
        adapter.notifyDataSetChanged()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        mergeMenuItem?.let {
            it.isChecked = mergeMode
        }
        prepareSearch(menu, filter)
        menu.findItem(R.id.DEBT_COMMAND)?.let { menuItem ->
            menuItem.isVisible = adapter.currentList.any { it.party.mappedDebts }
        }
    }

    private fun onQueryTextChange(newText: String): Boolean {
        with(newText.takeIf { !it.isEmpty() }) {
            if (this != filter) {
                filter = this
                loadParties()
            }
        }
        return true
    }

    private fun loadParties() {
        viewModel.loadParties(
            filter,
            requireActivity().intent.getLongExtra(KEY_ACCOUNTID, 0)
        )
    }

    private val action: String
        get() = manageParties.action

    private fun doSingleSelection(party: Party) {
        requireActivity().apply {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(KEY_PAYEEID, party.id)
                putExtra(KEY_LABEL, party.name)
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
        viewModel.getParties().observe(viewLifecycleOwner) { parties: List<Party> ->
            if (filter.isNullOrEmpty()) {
                activity?.invalidateOptionsMenu()
            }
            if (action != ACTION_SELECT_FILTER) {
                binding.empty.visibility = if (parties.isEmpty()) View.VISIBLE else View.GONE
                binding.list.visibility = if (parties.isEmpty()) View.GONE else View.VISIBLE
            }
            val elements = parties.map { PartyWrapper(it) }
            adapter.submitList(
                if (action == ACTION_SELECT_FILTER)
                    listOf(
                        PartyWrapper(
                            Party(
                                NULL_ITEM_ID,
                                getString(R.string.unmapped),
                                mappedTransactions = false,
                                mappedTemplates = false,
                                mappedDebts = false
                            )
                        )
                    ).plus(elements)
                else
                    elements
            )
        }
        viewModel.loadDebts().observe(viewLifecycleOwner) {
            loadParties()
        }
        return binding.root
    }

    private fun hasSelectMultiple(): Boolean {
        return action == ACTION_SELECT_FILTER || mergeMode
    }

    companion object {
        const val DIALOG_DEBT_DETAILS = "DEBT_DETAILS"
        const val DIALOG_NEW_PARTY = "dialogNewParty"
        const val DIALOG_EDIT_PARTY = "dialogEditParty"
        const val DIALOG_MERGE_PARTY = "dialogMergeParty"
        const val DIALOG_DELETE_PARTY = "dialogDeleteParty"
        const val KEY_POSITION = "position"
        const val SELECT_COMMAND = -1
        const val EDIT_COMMAND = -2
        const val DELETE_COMMAND = -3
        const val NEW_DEBT_COMMAND = -4
        const val DEBT_SUB_MENU = -5
        const val STATE_CHECK_STATES = "checkStates"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PartyWrapper>() {
            override fun areItemsTheSame(oldItem: PartyWrapper, newItem: PartyWrapper): Boolean {
                return oldItem.party.id == newItem.party.id
            }

            override fun areContentsTheSame(oldItem: PartyWrapper, newItem: PartyWrapper): Boolean {
                return oldItem.party == newItem.party && oldItem.hasOpenDebts == newItem.hasOpenDebts
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_NEW_PARTY, DIALOG_EDIT_PARTY -> {
                    val name = extras.getString(SimpleInputDialog.TEXT)!!
                    viewModel.saveParty(
                        extras.getLong(KEY_ROWID),
                        name
                    ).observe(this) {
                        if (it == null)
                            manageParties.showSnackBar(
                                getString(
                                    R.string.already_defined,
                                    name
                                )
                            )
                    }
                    return true
                }
                DIALOG_MERGE_PARTY -> {
                    mergeMode = false
                    updateUiMergeMode()
                    val selectedItemIds = adapter.getSelected().map { it.party.id }
                    viewModel.mergeParties(
                        selectedItemIds.toLongArray(),
                        selectedItemIds[extras.getInt(KEY_POSITION)]
                    )
                    adapter.clearSelection()
                    resetAdapter()
                    return true
                }
                DIALOG_DELETE_PARTY -> {
                    doDelete(extras.getLong(KEY_ROWID))
                }
            }
        }
        return false
    }

    fun dispatchFabClick() {
        if (action == ACTION_SELECT_FILTER) {
            val selected = adapter.getSelected().map { it.party }
            val itemIds = selected.map { it.id }
            val labels = selected.map { it.name }
            if (itemIds.size != 1 && itemIds.contains(NULL_ITEM_ID)) {
                manageParties.showSnackBar(R.string.unmapped_filter_only_single)
            } else {
                requireActivity().apply {
                    setResult(Activity.RESULT_FIRST_USER, Intent().apply {
                        putExtra(KEY_PAYEEID, itemIds.toLongArray())
                        putExtra(KEY_LABEL, labels.joinToString(separator = ","))
                    })
                    finish()
                }
            }
        } else if (mergeMode) {
            val selected = adapter.getSelected().map { it.party.name }.toTypedArray()
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