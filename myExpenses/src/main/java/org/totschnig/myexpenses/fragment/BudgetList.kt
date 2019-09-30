package org.totschnig.myexpenses.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.budgets.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Budget.Companion.DIFF_CALLBACK

class BudgetList : Fragment() {
    private lateinit var viewModel: BudgetViewModel
    private var actionMode: ActionMode? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.budgets, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = requireActivity().let {
            viewModel = ViewModelProviders.of(it)[BudgetViewModel::class.java]
            BudgetsAdapter(it)
        }
        viewModel.data.observe(this, Observer {
            adapter.submitList(it)
        })
        recycler_view.adapter = adapter
        viewModel.loadAllBudgets()
    }
}

class BudgetsAdapter(val context: Context) : ListAdapter<Budget, BudgetViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(LayoutInflater.from(context).inflate(R.layout.budget_list_row, parent, false))
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        getItem(position).let { budget ->
            with(holder) {
                title.setText(budget.title)
                itemView.setOnClickListener {
                    val i = Intent(context, BudgetActivity::class.java)
                    i.putExtra(KEY_ROWID, budget.id)
                    context.startActivity(i)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).id
}

class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView

    init {
        title = itemView.findViewById(R.id.Title)
    }
}

