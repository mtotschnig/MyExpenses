package org.totschnig.myexpenses.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.evernote.android.state.State
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdAdapter
import org.totschnig.myexpenses.databinding.OneBudgetBinding
import org.totschnig.myexpenses.dialog.KEY_RESULT_FILTER
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodsAllDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.ui.filter.ScrollingChip
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.getLabelForBudgetType
import java.time.LocalDate

class BudgetEdit : EditActivity(), AdapterView.OnItemSelectedListener,
    DatePicker.OnDateChangedListener {

    private val viewModel: BudgetEditViewModel by viewModels()
    private lateinit var binding: OneBudgetBinding
    private var pendingBudgetLoad = 0L
    private var resumedP = false
    private var budget: Budget? = null
    private lateinit var typeSpinnerHelper: SpinnerHelper
    private lateinit var accountSpinnerHelper: SpinnerHelper

    val filterRequestKey = "confirmFilterBudget"

    @State
    var accountId: Long = 0

    private val allFilterChips: Array<ScrollingChip>
        get() = with(binding) {
            arrayOf(
                FILTERCATEGORYCOMMAND,
                FILTERPAYEECOMMAND,
                FILTERMETHODCOMMAND,
                FILTERSTATUSCOMMAND,
                FILTERTAGCOMMAND,
                FILTERACCOUNTCOMMAND
            )
        }

    private fun setupListeners() {
        val removeFilter: (View) -> Unit = { view -> removeFilter((view.parent as View).id) }
        val startFilterDialog: (View) -> Unit =
            { view -> startFilterDialog((view.parent as View).id) }
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
        viewModel.removeFilter(id)
    }

    private fun startFilterDialog(id: Int) {
        val edit = viewModel.criteria.value.find { it.id == id }
        when (id) {
            R.id.FILTER_CATEGORY_COMMAND -> getCategory.launch(null to edit as? CategoryCriterion)

            R.id.FILTER_TAG_COMMAND -> getTags.launch(null to edit as? TagCriterion)

            R.id.FILTER_PAYEE_COMMAND -> getPayee.launch(null to edit as? PayeeCriterion)

            R.id.FILTER_METHOD_COMMAND -> {
                SelectMethodsAllDialogFragment.newInstance(
                    filterRequestKey,
                    edit as? MethodCriterion
                )
                    .show(supportFragmentManager, "METHOD_FILTER")
            }

            R.id.FILTER_STATUS_COMMAND -> {
                SelectCrStatusDialogFragment.newInstance(
                    filterRequestKey,
                    edit as? CrStatusCriterion,
                    false
                )
                    .show(supportFragmentManager, "STATUS_FILTER")
            }

            R.id.FILTER_ACCOUNT_COMMAND -> {
                SelectMultipleAccountDialogFragment.newInstance(
                    filterRequestKey,
                    selectedAccount()
                        .takeIf { !DataBaseAccount.isHomeAggregate(it.id) }
                        ?.currency,
                    edit as? AccountCriterion
                ).show(supportFragmentManager, "ACCOUNT_FILTER")
            }
        }
    }

    private val budgetId
        get() = intent.extras?.getLong(KEY_ROWID) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        floatingActionButton = binding.fab.CREATECOMMAND
        setupToolbarWithClose()
        injector.inject(viewModel)
        pendingBudgetLoad = if (savedInstanceState == null) budgetId else 0L
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountsMinimal().take(1).collect { list ->
                    if (list.isEmpty()) {
                        Toast.makeText(
                            this@BudgetEdit,
                            getString(R.string.no_accounts),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        accountSpinnerHelper.adapter = IdAdapter(this@BudgetEdit, list)
                        (accountId.takeIf { it != 0L } ?: list.getOrNull(0)?.id)?.let {
                            populateAccount(it)
                        }
                        if (pendingBudgetLoad != 0L) {
                            viewModel.budget(pendingBudgetLoad)
                                .observe(this@BudgetEdit) { populateData(it) }
                        }
                    }
                }
            }
        }
        newInstance = budgetId == 0L
        viewModel.databaseResult.observe(this) {
            if (it > -1) {
                finish()
            } else {
                Toast.makeText(this, "Error while saving budget", Toast.LENGTH_LONG).show()
            }
        }
        typeSpinnerHelper = SpinnerHelper(binding.Type).apply {
            adapter = GroupingAdapter(this@BudgetEdit)
            setSelection(Grouping.MONTH.ordinal)
        }
        accountSpinnerHelper = SpinnerHelper(binding.Accounts)
        if (!newInstance) {
            viewModel.initWith(budgetId)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.criteria.collect { criteria ->
                    listOf(
                        R.id.FILTER_CATEGORY_COMMAND to R.string.budget_filter_all_categories,
                        R.id.FILTER_PAYEE_COMMAND to R.string.budget_filter_all_parties,
                        R.id.FILTER_METHOD_COMMAND to R.string.budget_filter_all_methods,
                        R.id.FILTER_STATUS_COMMAND to R.string.budget_filter_all_states,
                        R.id.FILTER_TAG_COMMAND to R.string.budget_filter_all_tags,
                        R.id.FILTER_ACCOUNT_COMMAND to R.string.budget_filter_all_accounts
                    ).forEach { (id, label) ->
                        findViewById<ScrollingChip>(id)?.apply {
                            val criterion = criteria.find { it.id == id }
                            text = criterion?.prettyPrint(this@BudgetEdit) ?: getText(label)
                            isCloseIconVisible = criterion != null
                        }
                    }
                    configureFilterDependents(criteria.isEmpty())
                }
            }
        }
        setTitle(if (newInstance) R.string.menu_create_budget else R.string.menu_edit_budget)
        linkInputsWithLabels()
        supportFragmentManager.setFragmentResultListener(filterRequestKey, this) { _, result ->
            BundleCompat.getParcelable(result, KEY_RESULT_FILTER, SimpleCriterion::class.java)
                ?.let {
                    addFilterCriterion(it)
                }
        }
    }

    private inline fun buildLauncher(createContract: () -> PickObjectContract) =
        registerForActivityResult(createContract()) { criterion ->
            criterion?.let { addFilterCriterion(it) }
        }

    private val getCategory = buildLauncher(::PickCategoryContract)
    private val getPayee = buildLauncher(::PickPayeeContract)
    private val getTags = buildLauncher(::PickTagContract)

    fun addFilterCriterion(c: SimpleCriterion<*>) {
        setDirty()
        viewModel.addFilterCriterion(c)
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
        check(!newInstance)
        this.budget = budget
        binding.Title.setText(budget.title)
        binding.Description.setText(budget.description)
        populateAccount(budget.accountId)
        binding.AmountRow.isVisible = false
        typeSpinnerHelper.setSelection(budget.grouping.ordinal)
        typeSpinnerHelper.isEnabled = false
        configureTypeDependents(budget.grouping)
        binding.DefaultBudget.isChecked = budget.default
        if (resumedP) setupListeners()
        pendingBudgetLoad = 0L
    }

    private fun populateAccount(accountId: Long) {
        this.accountId = accountId
        with(accountSpinnerHelper) {
            (adapter as IdAdapter<*>).getPosition(accountId).takeIf { it > -1 }?.let {
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

    private fun configureFilterDependents(isEmpty: Boolean) {
        with(binding.DefaultBudget) {
            if (!isEmpty) {
                isChecked = false
            }
            isEnabled = isEmpty
        }
    }

    override fun onDateChanged(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        setDirty()
    }

    override val fabActionName = "SAVE_BUDGET"

    override fun onFabClicked() {
        super.onFabClicked()
        val grouping = typeSpinnerHelper.selectedItem as Grouping
        val duration =
            if (grouping == Grouping.NONE) binding.DurationFrom.getDate() to binding.DurationTo.getDate() else null
        if (duration != null && duration.second < duration.first) {
            showDismissibleSnackBar(R.string.budget_date_end_after_start)
        } else {
            val allocation = binding.Amount.getAmount(budgetId == 0L)
            if (allocation != null || budgetId != 0L) {
                val account: AccountMinimal = selectedAccount()
                val currencyUnit = currencyContext[account.currency]
                val initialAmount = if (budgetId == 0L) {
                    Money(currencyUnit, allocation!!).amountMinor
                } else null

                val budget = Budget(
                    budgetId,
                    account.id,
                    binding.Title.text.toString(),
                    binding.Description.text.toString(),
                    account.currency,
                    grouping,
                    -1,
                    duration?.first,
                    duration?.second,
                    null,
                    binding.DefaultBudget.isChecked
                )
                viewModel.saveBudget(budget, initialAmount)
            }
        }
    }

    private fun selectedAccount() = binding.Accounts.selectedItem as AccountMinimal
}

class GroupingAdapter(context: Context) : ArrayAdapter<Grouping>(
    context, android.R.layout.simple_spinner_item, android.R.id.text1,
    Grouping.entries.toTypedArray()
) {

    init {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
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