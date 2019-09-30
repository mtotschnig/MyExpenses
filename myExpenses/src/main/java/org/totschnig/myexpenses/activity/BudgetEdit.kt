package org.totschnig.myexpenses.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.one_budget.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEdit : EditActivity(), AdapterView.OnItemSelectedListener {
    lateinit var viewModel: BudgetEditViewModel
    override fun getDiscardNewMessage() = R.string.dialog_confirm_discard_new_budget
    var pendingBudgetLoad = 0L
    var resumedP = false

    override fun setupListeners() {
        Title.addTextChangedListener(this)
        Description.addTextChangedListener(this)
        Amount.addTextChangedListener(this)
    }

    private val budgetId
        get() = intent.extras?.getLong(KEY_ROWID) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.one_budget)
        setupToolbar()
        viewModel = ViewModelProviders.of(this).get(BudgetEditViewModel::class.java)
        viewModel.accounts.observe(this, Observer {
            Accounts.adapter = AccountAdapter(this, it)
            linkInputWithLabel(Accounts, AccountsLabel)
        })
        viewModel.budget.observe(this, Observer { populateData(it) })
        mNewInstance = budgetId == 0L
        if (savedInstanceState == null) {
            pendingBudgetLoad = budgetId
            viewModel.loadData(pendingBudgetLoad)
        }
        viewModel.databaseResult.observe(this, Observer {
            if (it) finish() else {
                Toast.makeText(this, "Error while saving budget", Toast.LENGTH_LONG).show()
            }
        })
        Type.onItemSelectedListener = this
        Accounts.onItemSelectedListener = this
        Type.adapter = GroupingAdapter(this)
        Type.setSelection(Grouping.MONTH.ordinal)
        linkInputWithLabels()
    }

    private fun linkInputWithLabels() {
        linkInputWithLabel(Title, TitleLabel)
        linkInputWithLabel(Description, DescriptionLabel)
        linkInputWithLabel(Amount, AmountLabel)
        linkInputWithLabel(Type, TypeLabel)
        linkInputWithLabel(DurationFrom, DurationFromLabel)
        linkInputWithLabel(DurationTo, DurationToLabel)
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
        Title.setText(budget.title)
        Description.setText(budget.description)
        Amount.setAmount(budget.amount.amountMajor)
        (Accounts.adapter as AccountAdapter).getPosition(budget.accountId).takeIf { it > -1 }?.let {
            Accounts.setSelection(it)
        }
        Type.setSelection(budget.grouping.ordinal)
        if (resumedP) setupListeners()
        pendingBudgetLoad = 0L
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        when (parent.id) {
            R.id.Type -> showDateRange(false)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        when (parent.id) {
            R.id.Type -> showDateRange(position == Grouping.NONE.ordinal)
            R.id.Accounts -> Amount.setFractionDigits(currencyContext[selectedAccount().currency].fractionDigits())
        }
    }

    private fun showDateRange(visible: Boolean) {
        DurationFromRow.isVisible = visible
        DurationToRow.isVisible = visible
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.SAVE_COMMAND) {
            val account: Account = selectedAccount()
            val currencyUnit = currencyContext[account.currency]
            val budget = Budget(budgetId, account.id,
                    Title.text.toString(), Description.text.toString(), account.currency,
                    Money(currencyUnit, validateAmountInput(Amount, false)),
                    Type.selectedItem as Grouping,
                    -1)
            viewModel.saveBudget(budget)
            return true;
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
        (row.findViewById<View>(android.R.id.text1) as TextView).setText(getBudgetLabelForSpinner(getItem(position)!!))
    }

    private fun getBudgetLabelForSpinner(type: Grouping) = when (type) {
        Grouping.DAY -> R.string.daily_plain
        Grouping.WEEK -> R.string.weekly_plain
        Grouping.MONTH -> R.string.monthly
        Grouping.YEAR -> R.string.yearly_plain
        Grouping.NONE -> R.string.budget_onetime
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