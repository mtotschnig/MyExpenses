package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import icepick.Icepick
import icepick.State
import kotlinx.android.synthetic.main.one_budget.*
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.SelectFilterDialog
import org.totschnig.myexpenses.dialog.SelectMethodsAllDialogFragment
import org.totschnig.myexpenses.dialog.SelectPayeeAllDialogFragment
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.CategoryCriteria
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.ui.filter.ScrollingChip
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel.Companion.prefNameForCriteria
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.getLabelForBudgetType

class BudgetEdit : EditActivity(), AdapterView.OnItemSelectedListener, DatePicker.OnDateChangedListener,
        SelectFilterDialog.Host {

    private lateinit var viewModel: BudgetEditViewModel
    override fun getDiscardNewMessage() = R.string.dialog_confirm_discard_new_budget
    private var pendingBudgetLoad = 0L
    private var resumedP = false
    private var budget: Budget? = null
    private lateinit var typeSpinnerHelper: SpinnerHelper
    private lateinit var filterPersistence: FilterPersistence
    @JvmField
    @State
    var accountId: Long? = null

    private val allFilterChips: Array<ScrollingChip>
        get() = arrayOf(FILTER_CATEGORY_COMMAND, FILTER_PAYEE_COMMAND, FILTER_METHOD_COMMAND, FILTER_STATUS_COMMAND)

    override fun setupListeners() {
        val removeFilter: (View) -> Unit = { view -> removeFilter((view.parent as View).id) }
        val startFilterDialog: (View) -> Unit = { view -> startFilterDialog((view.parent as View).id) }
        allFilterChips.forEach {
            it.setOnCloseIconClickListener(removeFilter)
            it.setOnClickListener(startFilterDialog)
        }
        Title.addTextChangedListener(this)
        Description.addTextChangedListener(this)
        Amount.addTextChangedListener(this)
        typeSpinnerHelper.setOnItemSelectedListener(this)
        Accounts.setOnItemSelectedListener(this)
        (budget?.start ?: LocalDate.now()).let {
            DurationFrom.initWith(it, this)
        }
        (budget?.end ?: LocalDate.now()).let {
            DurationTo.initWith(it, this)
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
                else -> 0
            }.takeIf { it != 0 }?.let { text = getString(it) }
            isCloseIconVisible = false
        }
        configureFilterDependents()
    }

    fun startFilterDialog(id: Int) {
        when (id) {
            R.id.FILTER_CATEGORY_COMMAND -> {
                Intent(this, ManageCategories::class.java).apply {
                    action = ManageCategories.ACTION_SELECT_FILTER
                    startActivityForResult(this, ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST)
                }
            }
            R.id.FILTER_PAYEE_COMMAND -> {
                SelectPayeeAllDialogFragment()
                        .show(supportFragmentManager, "PAYER_FILTER")
            }
            R.id.FILTER_METHOD_COMMAND -> {
                SelectMethodsAllDialogFragment()
                        .show(supportFragmentManager, "METHOD_FILTER")
            }
            R.id.FILTER_STATUS_COMMAND -> {
                SelectCrStatusDialogFragment.newInstance()
                        .show(supportFragmentManager, "STATUS_FILTER")
            }
        }
    }

    private val budgetId
        get() = intent.extras?.getLong(KEY_ROWID) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
        setContentView(R.layout.one_budget)
        setupToolbar()
        viewModel = ViewModelProviders.of(this).get(BudgetEditViewModel::class.java)
        viewModel.accounts.observe(this, Observer {
            Accounts.adapter = AccountAdapter(this, it)
            linkInputWithLabel(Accounts, AccountsLabel)
            accountId?.let { populateAccount(it) }
        })
        viewModel.budget.observe(this, Observer { populateData(it) })
        mNewInstance = budgetId == 0L
        pendingBudgetLoad = if (savedInstanceState == null) budgetId else 0L
        viewModel.loadData(pendingBudgetLoad)
        viewModel.databaseResult.observe(this, Observer {
            if (it > -1) {
                finish()
            } else {
                Toast.makeText(this, "Error while saving budget", Toast.LENGTH_LONG).show()
            }
        })
        typeSpinnerHelper = SpinnerHelper(Type).apply {
            adapter = GroupingAdapter(this@BudgetEdit)
            setSelection(Grouping.MONTH.ordinal)
        }
        linkInputWithLabels()
        filterPersistence = FilterPersistence(prefHandler, prefNameForCriteria(budgetId), savedInstanceState, false, !mNewInstance)

        filterPersistence.whereFilter.criteria.forEach(this::showFilterCriteria)
        configureFilterDependents()
        setTitle(if (mNewInstance) R.string.menu_create_budget else R.string.menu_edit_budget)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        filterPersistence.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST && resultCode != Activity.RESULT_CANCELED) {
            val label = intent!!.getStringExtra(KEY_LABEL)
            if (resultCode == Activity.RESULT_OK) {
                val catId = intent.getLongExtra(KEY_CATID, 0)
                addCategoryFilter(label, catId)
            }
            if (resultCode == Activity.RESULT_FIRST_USER) {
                val catIds = intent.getLongArrayExtra(KEY_CATID)
                addCategoryFilter(label, *catIds)
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun addCategoryFilter(label: String, vararg catIds: Long) {
        (if (catIds.size == 1 && catIds[0] == -1L) CategoryCriteria()
        else CategoryCriteria(label, *catIds)).let {
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

    private fun linkInputWithLabels() {
        linkInputWithLabel(Title, TitleLabel)
        linkInputWithLabel(Description, DescriptionLabel)
        linkInputWithLabel(Amount, AmountLabel)
        linkInputWithLabel(typeSpinnerHelper.spinner, TypeLabel)
        linkInputWithLabel(DefaultBudget, TypeLabel)
        linkInputWithLabel(DurationFrom, DurationFromLabel)
        linkInputWithLabel(DurationTo, DurationToLabel)
        allFilterChips.forEach {
            linkInputWithLabel(it, FilterLabel)
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
        Title.setText(budget.title)
        Description.setText(budget.description)
        populateAccount(budget.accountId)
        configureAmount(budget.currency)
        Amount.setAmount(budget.amount.amountMajor)
        typeSpinnerHelper.setSelection(budget.grouping.ordinal)
        configureTypeDependents(budget.grouping)
        DefaultBudget.isChecked = budget.default
        if (resumedP) setupListeners()
        pendingBudgetLoad = 0L
    }

    private fun populateAccount(accountId: Long) {
        with(Accounts) {
            (adapter as AccountAdapter).getPosition(accountId).takeIf { it > -1 }?.let {
                setSelection(it)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        //noop
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        setDirty()
        when (parent.id) {
            R.id.Type -> {
                configureTypeDependents(Type.adapter.getItem(position) as Grouping)
            }
            R.id.Accounts -> {
                configureAccount(selectedAccount())
            }
        }
    }

    private fun configureAccount(account: Account) {
        accountId = account.id
        configureAmount(currencyContext[account.currency])
    }

    private fun configureAmount(currencyUnit: CurrencyUnit) {
        Amount.setFractionDigits(currencyUnit.fractionDigits())
    }

    private fun configureTypeDependents(grouping: Grouping) {
        DurationFromRow.isVisible = grouping == Grouping.NONE
        DurationToRow.isVisible = grouping == Grouping.NONE
        DefaultBudget.isVisible = grouping != Grouping.NONE
    }

    private fun configureFilterDependents() {
        with(DefaultBudget) {
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
        if (command == R.id.SAVE_COMMAND) {
            validateAmountInput(Amount, true)?.let { amount ->
                val grouping = typeSpinnerHelper.selectedItem as Grouping
                val start = if (grouping == Grouping.NONE) DurationFrom.getDate() else null
                val end = if (grouping == Grouping.NONE) DurationTo.getDate() else null
                if (end != null && start != null && end < start) {
                    showDismissableSnackbar(R.string.budget_date_end_after_start)
                } else {
                    val account: Account = selectedAccount()
                    val currencyUnit = currencyContext[account.currency]
                    val budget = Budget(budgetId, account.id,
                            Title.text.toString(), Description.text.toString(), currencyUnit,
                            Money(currencyUnit, amount),
                            grouping,
                            -1,
                            start,
                            end, null, DefaultBudget.isChecked)
                    viewModel.saveBudget(budget, filterPersistence.whereFilter)
                }

            }
            return true
        }
        return super.dispatchCommand(command, tag)
    }

    private fun selectedAccount() = Accounts.selectedItem as Account
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

class AccountAdapter(context: Context, accounts: List<Account>) : ArrayAdapter<Account>(
        context, android.R.layout.simple_spinner_item, android.R.id.text1, accounts) {
    override fun hasStableIds(): Boolean = true
    override fun getItemId(position: Int): Long = getItem(position)!!.id
    fun getPosition(accountId: Long): Int {
        for (i in 0 until count) {
            if (getItem(i)!!.id == accountId) return i
        }
        return -1
    }
}

fun DatePicker.initWith(date: LocalDate, listener: DatePicker.OnDateChangedListener) {
    with(date) {
        init(year, monthValue - 1, dayOfMonth, listener)
    }
}

fun DatePicker.getDate(): LocalDate = LocalDate.of(year, month + 1, dayOfMonth)