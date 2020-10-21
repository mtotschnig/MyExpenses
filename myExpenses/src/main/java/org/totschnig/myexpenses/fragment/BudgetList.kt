package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.form.SimpleFormDialog
import icepick.State
import kotlinx.android.synthetic.main.budget_list_row.view.*
import kotlinx.android.synthetic.main.budgets.*
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.fragment.BudgetFragment.EDIT_BUDGET_DIALOG
import org.totschnig.myexpenses.fragment.BudgetFragment.buildAmountField
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Budget.Companion.DIFF_CALLBACK
import java.math.BigDecimal
import javax.inject.Inject

class BudgetList : Fragment(), SimpleDialog.OnDialogResultListener {
    private lateinit var viewModel: BudgetViewModel
    private var position2Spent: Array<Long?>? = null

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter
    @Inject
    lateinit var prefHandler: PrefHandler

    @State
    @JvmField
    var lastClickedPosition: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.budgets, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApplication.getInstance().getAppComponent().inject(this)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = requireActivity().let {
            viewModel = ViewModelProviders.of(it)[BudgetViewModel::class.java]
            BudgetsAdapter(it)
        }
        with(recycler_view) {
            LinearLayoutManager(activity).also {
                layoutManager = it
                addItemDecoration(DividerItemDecoration(activity, it.orientation))
            }
        }
        viewModel.data.observe(viewLifecycleOwner, Observer {
            position2Spent = arrayOfNulls(it.size)
            adapter.submitList(it)
            empty.isVisible = it.size == 0
            recycler_view.isVisible = it.size > 0
        })
        viewModel.spent.observe(viewLifecycleOwner, Observer {
            position2Spent?.set(it.first, it.second)
            adapter.notifyItemChanged(it.first)
        })
        recycler_view.adapter = adapter
        viewModel.loadAllBudgets()
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE && dialogTag == EDIT_BUDGET_DIALOG) {
            val amount = Money(extras.getSerializable(KEY_CURRENCY) as CurrencyUnit, extras.getSerializable(KEY_AMOUNT) as BigDecimal)
            viewModel.updateBudget(extras.getLong(KEY_ROWID), 0L, amount)
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        lastClickedPosition?.let {
            if (resultCode != Activity.RESULT_FIRST_USER) { //budget was deleted
                recycler_view.adapter?.notifyItemChanged(it)
            }
            lastClickedPosition = null
        }
    }

    inner class BudgetsAdapter(val context: Context) :
            ListAdapter<Budget, BudgetViewHolder>(DIFF_CALLBACK) {

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            return BudgetViewHolder(LayoutInflater.from(context).inflate(R.layout.budget_list_row, parent, false))
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            getItem(position).let { budget ->
                with(holder.itemView) {
                    Title.setText(budget.titleComplete(context))
                    val spent = position2Spent?.get(position) ?: run {
                        viewModel.loadBudgetSpend(position, budget, prefHandler)
                        0L
                    }
                    budgetSummary.bind(budget, spent, currencyFormatter)
                    budgetSummary.setOnBudgetClickListener(object : View.OnClickListener {
                        override fun onClick(v: View?) {
                            val bundle = Bundle(2)
                            bundle.putSerializable(KEY_CURRENCY, budget.currency)
                            bundle.putLong(KEY_ROWID, budget.id)
                            SimpleFormDialog.build()
                                    .title(getString(R.string.dialog_title_edit_budget))
                                    .neg()
                                    .theme(R.style.SimpleDialog)
                                    .extra(bundle)
                                    .fields(buildAmountField(budget.amount, context))
                                    .show(this@BudgetList, EDIT_BUDGET_DIALOG);
                        }
                    })

                    val filterList = mutableListOf<String>()
                    filterList.add(budget.label(requireContext()))
                    val filterPersistence = FilterPersistence(prefHandler, BudgetViewModel.prefNameForCriteria(budget.id), null, false, true)
                    filterPersistence.whereFilter.criteria.forEach { criterion -> filterList.add(criterion.prettyPrint(context)) }
                    filter.addChipsBulk(filterList, null)
                    setOnClickListener {
                        val i = Intent(context, BudgetActivity::class.java)
                        i.putExtra(KEY_ROWID, budget.id)
                        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        lastClickedPosition = holder.adapterPosition
                        startActivityForResult(i, 0)
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = getItem(position).id
    }
}


class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

