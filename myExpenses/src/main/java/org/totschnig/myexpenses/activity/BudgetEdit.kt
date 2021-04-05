package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import icepick.State
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.AccountAdapter
import org.totschnig.myexpenses.adapter.CategoryTreeBaseAdapter.NULL_ITEM_ID
import org.totschnig.myexpenses.databinding.OneBudgetBinding
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectFilterDialog
import org.totschnig.myexpenses.dialog.select.SelectMethodsAllDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.fragment.KEY_TAG_LIST
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.CategoryCriteria
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.PayeeCriteria
import org.totschnig.myexpenses.provider.filter.TagCriteria
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.ui.filter.ScrollingChip
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel.Companion.prefNameForCriteria
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.getLabelForBudgetType

class BudgetEdit : EditActivity(), AdapterView.OnItemSelectedListener, DatePicker.OnDateChangedListener,
        SelectFilterDialog.Host {

    private lateinit var viewModel: BudgetEditViewModel
    private lateinit var binding: OneBudgetBinding
    override fun getDiscardNewMessage() = R.string.dialog_confirm_discard_new_budget
    private var pendingBudgetLoad = 0L
    private var resumedP = false
    private var budget: Budget? = null
    private lateinit var typeSpinnerHelper: SpinnerHelper
    private lateinit var accountSpinnerHelper: SpinnerHelper
    private lateinit var filterPersistence: FilterPersistence
    @JvmField
    @State
    var accountId: Long = 0

    private val allFilterChips: Array<ScrollingChip>
        get() = with(binding) { arrayOf( FILTERCATEGORYCOMMAND, FILTERPAYEECOMMAND, FILTERMETHODCOMMAND, FILTERSTATUSCOMMAND, FILTERTAGCOMMAND, FILTERACCOUNTCOMMAND) }

    override fun setupListeners() {
        val removeFilter: (View) -> Unit = { view -> removeFilter((view.parent as View).id) }
        val startFilterDialog: (View) -> Unit = { view -> startFilterDialog((view.parent as View).id) }
        allFilterChips.forEach {
            it.setOnCloseIconClickListener(removeFilter)
            it.setOnClickListener(startFilterDialog)
        }
        binding.Title.addTextChangedListener(this)
        binding.Description.addTextChangedListener(this)
        binding.Amount.addTextChangedListener(this)
        typeSpinnerHelper.setOnItemSelectedListener(this)
        accountSpinnerHelper.setOnItemSelectedListener(this)
        (budget?.start ?: LocalDate.now()).let {
            binding.DurationFrom.initWith(it, this)
        }
        (budget?.end ?: LocalDate.now()).let {
            binding.DurationTo.initWith(it, this)
        }
    }

    private fun removeFilter(id: Int) {
        setDirty()
        filterPersistence.removeFilter(id)
        findViewById<ScrollingChip>(id)?.apply {
            when (id) {
                R.id.FILTER_CATEGORY_COMMAND -> R.string.budget_filter_all_categories
                R.id.FILTER_PAYEE_COMMAND -> R.string.budget_filter_all_parties
                R.id.FILTER_METHOD_COMMAND -> R.string.budget_filter_all_methods
                R.id.FILTER_STATUS_COMMAND -> R.string.budget_filter_all_states
                R.id.FILTER_TAG_COMMAND -> R.string.budget_filter_all_tags
                R.id.FILTER_ACCOUNT_COMMAND -> R.string.budget_filter_all_accounts
                else -> 0
            }.takeIf { it != 0 }?.let { text = getString(it) }
            isCloseIconVisible = false
        }
        configureFilterDependents()
    }

    private fun startFilterDialog(id: Int) {
        when (id) {
            R.id.FILTER_CATEGORY_COMMAND -> {
                Intent(this, ManageCategories::class.java).apply {
                    action = ACTION_SELECT_FILTER
                    startActivityForResult(this, FILTER_CATEGORY_REQUEST)
                }
            }
            R.id.FILTER_TAG_COMMAND -> {
                Intent(this, ManageTags::class.java).apply {
                    action = ACTION_SELECT_FILTER
                    startActivityForResult(this, FILTER_TAGS_REQUEST)
                }
            }
            R.id.FILTER_PAYEE_COMMAND -> {
                Intent(this, ManageParties::class.java).apply {
                    action = ACTION_SELECT_FILTER
                    startActivityForResult(this, FILTER_PAYEE_REQUEST)
                }
            }
            R.id.FILTER_METHOD_COMMAND -> {
                SelectMethodsAllDialogFragment()
                        .show(supportFragmentManager, "METHOD_FILTER")
            }
            R.id.FILTER_STATUS_COMMAND -> {
                SelectCrStatusDialogFragment.newInstance()
                        .show(supportFragmentManager, "STATUS_FILTER")
            }
            R.id.FILTER_ACCOUNT_COMMAND -> {
                SelectMultipleAccountDialogFragment.newInstance(selectedAccount().currency)
                        .show(supportFragmentManager, "ACCOUNT_FILTER")
            }
        }
    }

    private val budgetId
        get() = intent.extras?.getLong(KEY_ROWID) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        viewModel = ViewModelProvider(this).get(BudgetEditViewModel::class.java)
        pendingBudgetLoad = if (savedInstanceState == null) budgetId else 0L
        viewModel.accountsMinimal.observe(this, { list ->
            binding.Accounts.adapter = AccountAdapter(this, list)
            (accountId.takeIf { it != 0L } ?: list.getOrNull(0)?.id)?.let { populateAccount(it) }
            if (pendingBudgetLoad != 0L) {
                viewModel.loadBudget(pendingBudgetLoad, true)
            }
        })
        viewModel.budget.observe(this, { populateData(it) })
        mNewInstance = budgetId == 0L
        viewModel.databaseResult.observe(this, {
            if (it > -1) {
                finish()
            } else {
                Toast.makeText(this, "Error while saving budget", Toast.LENGTH_LONG).show()
            }
        })
        typeSpinnerHelper = SpinnerHelper(binding.Type).apply {
            adapter = GroupingAdapter(this@BudgetEdit)
            setSelection(Grouping.MONTH.ordinal)
        }
        accountSpinnerHelper = SpinnerHelper(binding.Accounts)
        filterPersistence = FilterPersistence(prefHandler, prefNameForCriteria(budgetId), savedInstanceState, false, !mNewInstance)

        filterPersistence.whereFilter.criteria.forEach(this::showFilterCriteria)
        configureFilterDependents()
        setTitle(if (mNewInstance) R.string.menu_create_budget else R.string.menu_edit_budget)
        linkInputsWithLabels()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        filterPersistence.onSaveInstanceState(outState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_CANCELED) {
            when (requestCode) {
                FILTER_CATEGORY_REQUEST -> {
                    intent?.getStringExtra(KEY_LABEL)?.let { label ->
                        if (resultCode == Activity.RESULT_OK) {
                            intent.getLongExtra(KEY_CATID, 0).takeIf { it > 0 }?.let {
                                addCategoryFilter(label, it)
                            }
                        }
                        if (resultCode == Activity.RESULT_FIRST_USER) {
                            intent.getLongArrayExtra(KEY_CATID)?.let {
                                addCategoryFilter(label, *it)
                            }
                        }
                    }
                }
                FILTER_TAGS_REQUEST -> {
                    intent?.getParcelableArrayListExtra<Tag>(KEY_TAG_LIST)?.takeIf { it.size > 0 }?.let {
                        val tagIds = it.map(Tag::id).toLongArray()
                        val label = it.map(Tag::label).joinToString(", ")
                        addFilterCriteria(TagCriteria(label, *tagIds))
                    }
                }
                FILTER_PAYEE_REQUEST -> {
                    intent?.getStringExtra(KEY_LABEL)?.let { label ->
                        if (resultCode == Activity.RESULT_OK) {
                            intent.getLongExtra(KEY_PAYEEID, 0).takeIf { it > 0 }?.let {
                                addPayeeFilter(label, it)
                            }
                        }
                        if (resultCode == Activity.RESULT_FIRST_USER) {
                            intent.getLongArrayExtra(KEY_PAYEEID)?.let {
                                addPayeeFilter(label, *it)
                            }
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun addCategoryFilter(label: String, vararg catIds: Long) {
        (if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) CategoryCriteria()
        else CategoryCriteria(label, *catIds)).let {
            addFilterCriteria(it)
        }
    }

    private fun addPayeeFilter(label: String, vararg payeeIds: Long) {
        (if (payeeIds.size == 1 && payeeIds[0] == NULL_ITEM_ID) PayeeCriteria()
        else PayeeCriteria(label, *payeeIds)).let {
            addFilterCriteria(it)
        }
    }

    override fun addFilterCriteria(c: Criteria) {
        setDirty()
        filterPersistence.addCriteria(c)
        showFilterCriteria(c)
        configureFilterDependents()
    }

    private fun showFilterCriteria(c: Criteria) {
        findViewById<ScrollingChip>(c.id)?.apply {
            text = c.prettyPrint(this@BudgetEdit)
            isCloseIconVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        resumedP = true
        if (pendingBudgetLoad == 0L) setupListeners()
    }

    override fun onPause() {
        super.onPause()
        resumedP = false
    }

    private fun populateData(budget: Budget) {
        this.budget = budget
        binding.Title.setText(budget.title)
        binding.Description.setText(budget.description)
        populateAccount(budget.accountId)
        binding.Amount.setAmount(budget.amount.amountMajor)
        typeSpinnerHelper.setSelection(budget.grouping.ordinal)
        configureTypeDependents(budget.grouping)
        binding.DefaultBudget.isChecked = budget.default
        if (resumedP) setupListeners()
        pendingBudgetLoad = 0L
    }

    private fun populateAccount(accountId: Long) {
        this.accountId = accountId
        with(accountSpinnerHelper) {
            (adapter as AccountAdapter).getPosition(accountId).takeIf { it > -1 }?.let {
                setSelection(it)
            }
        }
        configureAccount()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        //noop
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        setDirty()
        when (parent.id) {
            R.id.Type -> {
                configureTypeDependents(binding.Type.adapter.getItem(position) as Grouping)
            }
            R.id.Accounts -> {
                configureAccount()
                removeFilter(R.id.FILTER_ACCOUNT_COMMAND)
            }
        }
    }

    private fun configureAccount() {
        val account = selectedAccount()
        accountId = account.id
        binding.FILTERACCOUNTCOMMAND.visibility = if (accountId < 0) View.VISIBLE else View.GONE
        configureAmount(currencyContext[account.currency])
    }

    private fun configureAmount(currencyUnit: CurrencyUnit) {
        binding.Amount.setFractionDigits(currencyUnit.fractionDigits)
    }

    private fun configureTypeDependents(grouping: Grouping) {
        binding.DurationFromRow.isVisible = grouping == Grouping.NONE
        binding.DurationToRow.isVisible = grouping == Grouping.NONE
        binding.DefaultBudget.isVisible = grouping != Grouping.NONE
    }

    private fun configureFilterDependents() {
        with(binding.DefaultBudget) {
            filterPersistence.whereFilter.isEmpty.let {
                if (!it) {
                    isChecked = false
                }
                isEnabled = it
            }
        }
    }

    override fun onDateChanged(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        setDirty()
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.CREATE_COMMAND) {
            validateAmountInput(binding.Amount, true)?.let { amount ->
                val grouping = typeSpinnerHelper.selectedItem as Grouping
                val start = if (grouping == Grouping.NONE) binding.DurationFrom.getDate() else null
                val end = if (grouping == Grouping.NONE) binding.DurationTo.getDate() else null
                if (end != null && start != null && end < start) {
                    showDismissibleSnackbar(R.string.budget_date_end_after_start)
                } else {
                    val account: AccountMinimal = selectedAccount()
                    val currencyUnit = currencyContext[account.currency]
                    val budget = Budget(budgetId, account.id,
                            binding.Title.text.toString(), binding.Description.text.toString(), currencyUnit,
                            Money(currencyUnit, amount),
                            grouping,
                            -1,
                            start,
                            end, null, binding.DefaultBudget.isChecked)
                    viewModel.saveBudget(budget, filterPersistence.whereFilter)
                }

            }
            return true
        }
        return super.dispatchCommand(command, tag)
    }

    private fun selectedAccount() = binding.Accounts.selectedItem as AccountMinimal
}

class GroupingAdapter(context: Context) : ArrayAdapter<Grouping>(context, android.R.layout.simple_spinner_item, android.R.id.text1, Grouping.values()) {

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        setText(position, row)
        return row
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getDropDownView(position, convertView, parent)
        setText(position, row)
        return row
    }

    private fun setText(position: Int, row: View) {
        (row.findViewById<View>(android.R.id.text1) as TextView).setText(getItem(position)!!.getLabelForBudgetType())
    }
}

fun DatePicker.initWith(date: LocalDate, listener: DatePicker.OnDateChangedListener) {
    with(date) {
        init(year, monthValue - 1, dayOfMonth, listener)
    }
}

fun DatePicker.getDate(): LocalDate = LocalDate.of(year, month + 1, dayOfMonth)