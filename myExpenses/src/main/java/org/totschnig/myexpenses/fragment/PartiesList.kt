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
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.DebtOverview
import org.totschnig.myexpenses.activity.HELP_VARIANT_MANGE
import org.totschnig.myexpenses.activity.HELP_VARIANT_MERGE_MODE
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.activity.asAction
import org.totschnig.myexpenses.databinding.PartiesListBinding
import org.totschnig.myexpenses.databinding.PayeeRowBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.DebtDetailsDialogFragment
import org.totschnig.myexpenses.dialog.MergePartiesDialogFragment
import org.totschnig.myexpenses.dialog.MergePartiesDialogFragment.Companion.KEY_POSITION
import org.totschnig.myexpenses.dialog.MergePartiesDialogFragment.Companion.KEY_STRATEGY
import org.totschnig.myexpenses.dialog.buildPartyEditDialog
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.filter.preSelected
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.MergeStrategy
import org.totschnig.myexpenses.viewmodel.PartyListViewModel
import org.totschnig.myexpenses.viewmodel.data.Party
import javax.inject.Inject
import kotlin.math.sign

class PartiesList : Fragment(), OnDialogResultListener {

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    val manageParties: ManageParties
        get() = (activity as ManageParties)

    private fun toggleShowDuplicates(partyId: Long) {
        viewModel.expandedItem = if (viewModel.expandedItem == partyId) null else partyId
        resetAdapter()
    }

    inner class ViewHolder(val binding: PayeeRowBinding, private val itemCallback: ItemCallback) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(party: Party, isChecked: Boolean) {
            binding.Payee.text = party.name
            binding.Payee.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (party.isDuplicate) R.drawable.ic_group else 0, 0, 0, 0
            )
            with(binding.checkBox) {
                visibility = if (hasSelectMultiple()) View.VISIBLE else View.INVISIBLE
                this.isChecked = isChecked
                setOnCheckedChangeListener { view, isChecked ->
                    if (view.isShown) {
                        itemCallback.onCheckedChanged(
                            isChecked,
                            party
                        )
                    }
                }
            }
            with(binding.groupIndicator) {
                if (hasSelectMultiple() || party.duplicates.isEmpty()) {
                    isVisible = false
                } else {
                    isVisible = true
                    setImageResource(if (viewModel.expandedItem == party.id) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                    contentDescription = getString(
                        if (viewModel.expandedItem == party.id) R.string.collapse else R.string.show_duplicates
                    )
                    setOnClickListener {
                        toggleShowDuplicates(party.id)
                    }
                }
            }
            binding.Debt.isVisible = party.hasOpenDebts()
            with(binding.BankDetails) {
                val hasBankDetails = party.iban != null || party.bic != null
                isVisible = hasBankDetails
                if (hasBankDetails) {
                    //noinspection SetTextI18n
                    text = "${party.bic} : ${party.iban}"
                }
            }
            binding.Unused.isVisible = party.isUnused

            binding.root.setOnClickListener { itemCallback.onItemClick(binding, party) }
        }

        private fun Party.hasOpenDebts() =
            viewModel.getDebts(id)?.any { !it.isSealed && it.currentBalance != 0L } == true
    }

    interface ItemCallback {
        fun onItemClick(binding: PayeeRowBinding, party: Party)
        fun onCheckedChanged(isChecked: Boolean, party: Party)
    }

    inner class PayeeAdapter :
        ListAdapter<Party, ViewHolder>(DIFF_CALLBACK), ItemCallback {

        init {
            setHasStableIds(true)
        }

        private var checkStates: MutableSet<Long> = mutableSetOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(PayeeRowBinding.inflate(LayoutInflater.from(context), parent, false), this)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(getParty(position)) {
                holder.bind(this, checkStates.contains(id))
            }
        }

        override fun getItemId(position: Int) = getItem(position).id

        override fun onCurrentListChanged(
            previousList: MutableList<Party>,
            currentList: MutableList<Party>
        ) {
            updateFabEnabled()
        }

        private fun getParty(position: Int): Party = getItem(position)

        fun getSelected(): List<Party> =
            currentList.filter { checkStates.contains(it.id) }

        val checkedCount: Int
            get() = getSelected().size

        override fun onItemClick(binding: PayeeRowBinding, party: Party) {
            if (hasSelectMultiple()) {
                binding.checkBox.toggle()
                return
            }
            val index2IdMap: MutableMap<Int, Long> = mutableMapOf()
            with(PopupMenu(requireContext(), binding.root)) {
                if (action == Action.SELECT_MAPPING) {
                    menu.add(Menu.NONE, SELECT_COMMAND, Menu.NONE, R.string.select)
                        .setIcon(R.drawable.ic_menu_done)
                }
                if (party.id != NULL_ITEM_ID) {
                    menu.add(Menu.NONE, EDIT_COMMAND, Menu.NONE, R.string.menu_edit)
                        .setIcon(R.drawable.ic_menu_edit)
                    menu.add(Menu.NONE, DELETE_COMMAND, Menu.NONE, R.string.menu_delete)
                        .setIcon(R.drawable.ic_menu_delete)
                }
                if (party.isDuplicate) {
                    menu.add(
                        Menu.NONE, REMOVE_FROM_GROUP_COMMAND, Menu.NONE,
                        getString(R.string.remove_from_group)
                    )
                        .setIcon(R.drawable.ic_group_remove)
                }
                if (action == Action.MANAGE) {
                    val debts = viewModel.getDebts(party.id)
                    val subMenu = if ((debts?.size ?: 0) > 0)
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
                    when (item.itemId) {
                        EDIT_COMMAND -> showEditDialog(party)
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
                                (requireActivity() as BaseActivity).showConfirmationDialog(
                                    tag = "DELETE_PARTY",
                                    message = concatResStrings(
                                        requireContext(),
                                        " ",
                                        R.string.warning_party_delete_debt,
                                        R.string.continue_confirmation
                                    ),
                                    commandPositive = R.id.DELETE_COMMAND,
                                    tagPositive = Bundle(1).apply {
                                        putLong(KEY_ROWID, party.id)
                                    }
                                )
                            } else {
                                doDelete(party.id)
                            }
                        }

                        SELECT_COMMAND -> doSingleSelection(party)
                        DEBT_SUB_MENU -> { /*submenu*/
                        }

                        NEW_DEBT_COMMAND -> {
                            startActivity(Intent(context, DebtEdit::class.java).apply {
                                putExtra(KEY_PAYEEID, party.id)
                                putExtra(KEY_PAYEE_NAME, party.name)
                            })
                        }

                        REMOVE_FROM_GROUP_COMMAND -> {
                            viewModel.removeDuplicateFromGroup(party.id)
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

        override fun onCheckedChanged(isChecked: Boolean, party: Party) {
            if (isChecked) {
                checkStates.add(party.id)
            } else {
                checkStates.remove(party.id)
            }
            updateFabEnabled()
        }

        fun check(id: Long) {
            checkStates.add(id)
        }

        fun check(ids: List<Long>) {
            checkStates.addAll(ids)
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
        (activity as? ManageParties)?.setFabEnabled(
            adapter.checkedCount >=
                    if (mergeMode) 2 else if (action == Action.SELECT_FILTER) 1 else 0
        )
    }

    fun doDelete(partyId: Long) {
        manageParties.showSnackBar(R.string.progress_dialog_deleting)
        viewModel.deleteParty(partyId)
            .observe(viewLifecycleOwner) { result ->
                result.onSuccess { count ->
                    manageParties.showSnackBar(
                        resources.getQuantityString(
                            R.plurals.delete_success,
                            count,
                            count
                        )
                    )
                }.onFailure {
                    manageParties.showDeleteFailureFeedback(it.message)
                }
            }
    }

    lateinit var adapter: PayeeAdapter
    private val viewModel: PartyListViewModel by activityViewModels()
    private var _binding: PartiesListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @State
    var mergeMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@PartiesList)
            inject(viewModel)
        }
        childFragmentManager.setFragmentResultListener(DIALOG_MERGE_PARTY, this) { _, bundle ->
            mergeMode = false
            updateUiMergeMode()
            val selectedItemIds = adapter.getSelected().map { it.id }
            val idToKeep = selectedItemIds[bundle.getInt(KEY_POSITION)]
            viewModel.mergeParties(
                selectedItemIds.subtract(setOf(idToKeep)),
                idToKeep,
                bundle.getSerializable(KEY_STRATEGY) as MergeStrategy
            )
            adapter.clearSelection()
            resetAdapter()
        }

        StateSaver.restoreInstanceState(this, savedInstanceState)
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
        StateSaver.saveInstanceState(this, outState)
        adapter.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity == null) return
        inflater.inflate(R.menu.search, menu)
        if (action == Action.MANAGE) {
            menu.add(Menu.NONE, R.id.MERGE_COMMAND, 0, R.string.menu_merge).apply {
                setIcon(R.drawable.ic_menu_split_transaction)
                isCheckable = true
            }
            menu.add(Menu.NONE, R.id.DEBT_COMMAND, 0, R.string.title_activity_debt_overview)
                .setIcon(R.drawable.balance_scale)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            menu.add(Menu.NONE, R.id.CLEANUP_COMMAND, 0, R.string.menu_cleanup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        configureSearch(requireActivity(), menu, callback = ::onQueryTextChange)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.MERGE_COMMAND -> {
                mergeMode = !mergeMode
                viewModel.expandedItem = null
                updateUiMergeMode()
                resetAdapter()
                true
            }

            R.id.DEBT_COMMAND -> {
                startActivity(Intent(context, DebtOverview::class.java))
                true
            }

            R.id.CLEANUP_COMMAND -> {
                val unusedCount = viewModel.unusedCount.value
                (requireActivity() as BaseActivity).showConfirmationDialog(
                    tag = "CLEANUP_PARTIES",
                    message =
                        resources.getQuantityString(
                            R.plurals.warning_cleanup_parties,
                            unusedCount,
                            unusedCount
                        ) +
                                " " + getString(R.string.continue_confirmation),
                    commandPositive = R.id.CLEANUP_COMMAND_DO,
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun updateUiMergeMode() {
        with(manageParties) {
            invalidateOptionsMenu()
            configureFloatingActionButton()
            setFabEnabled(!mergeMode)
            setHelpVariant(if (mergeMode) HELP_VARIANT_MERGE_MODE else HELP_VARIANT_MANGE)
        }
    }

    private fun resetAdapter() {
        //noinspection NotifyDataSetChanged
        adapter.notifyDataSetChanged()
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.MERGE_COMMAND)?.let { menuItem ->
            menuItem.setEnabledAndVisible(adapter.currentList.count { !it.isDuplicate } > 1)
            menuItem.isChecked = mergeMode
        }
        menu.prepareSearch(viewModel.filter)
        menu.findItem(R.id.DEBT_COMMAND)?.let { menuItem ->
            menuItem.isVisible = viewModel.hasDebts
        }
        menu.findItem(R.id.CLEANUP_COMMAND)?.let { menuItem ->
            menuItem.isVisible = viewModel.unusedCount.value > 0
        }
    }

    private fun onQueryTextChange(newText: String): Boolean {
        viewModel.filter = newText
        return true
    }

    private val action
        get() = requireActivity().intent.asAction

    private fun doSingleSelection(party: Party) {
        requireActivity().apply {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(KEY_ROWID, party.id)
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
        if (savedInstanceState == null) {
            requireActivity().preSelected?.let { adapter.check(it) }
        } else {
            adapter.onRestoreInstanceState(savedInstanceState)
        }
        binding.list.adapter = adapter
        viewModel.loadDebts().observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.parties(action != Action.SELECT_FILTER)
                        .collect { parties: List<Party> ->
                            if (action != Action.SELECT_FILTER) {
                                binding.empty.visibility =
                                    if (parties.isEmpty()) View.VISIBLE else View.GONE
                                binding.list.visibility =
                                    if (parties.isEmpty()) View.GONE else View.VISIBLE
                            }
                            adapter.submitList(
                                if (action != Action.MANAGE)
                                    listOf(Party(NULL_ITEM_ID, getString(R.string.unmapped))).plus(
                                        parties
                                    )
                                else
                                    parties
                            ) {
                                if (viewModel.filter.isNullOrEmpty()) {
                                    activity?.invalidateOptionsMenu()
                                }
                            }
                        }
                }
            }
        }
        return binding.root
    }

    private fun hasSelectMultiple(): Boolean {
        return action == Action.SELECT_FILTER || mergeMode
    }

    companion object {
        const val DIALOG_DEBT_DETAILS = "DEBT_DETAILS"
        const val DIALOG_EDIT_PARTY = "dialogEditParty"
        const val DIALOG_MERGE_PARTY = "dialogMergeParty"
        const val SELECT_COMMAND = -1
        const val EDIT_COMMAND = -2
        const val DELETE_COMMAND = -3
        const val NEW_DEBT_COMMAND = -4
        const val DEBT_SUB_MENU = -5
        const val REMOVE_FROM_GROUP_COMMAND = -7
        const val STATE_CHECK_STATES = "checkStates"

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
                DIALOG_EDIT_PARTY -> {
                    val name = extras.getString(KEY_PAYEE_NAME)!!
                    viewModel.saveParty(
                        extras.getLong(KEY_ROWID),
                        name,
                        extras.getString(KEY_SHORT_NAME)
                    ).observe(this) {
                        if (!it)
                            manageParties.showSnackBar(
                                getString(
                                    R.string.already_defined,
                                    name
                                )
                            )
                    }
                    return true
                }
            }
        }
        return false
    }

    fun dispatchFabClick() {
        if (action == Action.SELECT_FILTER) {
            val selected = adapter.getSelected()
            val itemIds = selected.map { it.id }
            val labels = selected.map { it.name }
            if (itemIds.size != 1 && itemIds.contains(NULL_ITEM_ID)) {
                manageParties.showSnackBar(R.string.unmapped_filter_only_single)
            } else {
                requireActivity().apply {
                    setResult(Activity.RESULT_FIRST_USER, Intent().apply {
                        putExtra(
                            KEY_ACCOUNTID,
                            requireActivity().intent.getLongExtra(KEY_ACCOUNTID, 0)
                        )
                        putExtra(KEY_ROWID, itemIds.toLongArray())
                        putExtra(KEY_LABEL, labels.joinToString(separator = ","))
                    })
                    finish()
                }
            }
        } else if (mergeMode) {
            val selected = adapter.getSelected().map { it.name }.toTypedArray()
            MergePartiesDialogFragment.newInstance(selected)
                .show(childFragmentManager, DIALOG_MERGE_PARTY)
        } else {
            showEditDialog(null)
        }
    }

    private fun showEditDialog(party: Party?) {
        buildPartyEditDialog(party?.id, party?.name, party?.shortName)
            .show(this, DIALOG_EDIT_PARTY)
    }

    fun doCleanup() {
        viewModel.cleanup()
    }
}