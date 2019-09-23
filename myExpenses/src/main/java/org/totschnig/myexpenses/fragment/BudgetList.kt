package org.totschnig.myexpenses.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.budgets.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Budget.Companion.DIFF_CALLBACK

class BudgetList : Fragment() {
    private lateinit var viewModel: BudgetViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.budgets, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(activity!!)[BudgetViewModel::class.java]
        val adapter = BudgetsAdapter(context)
        viewModel.data.observe(this, Observer {
            adapter.submitList(it)
        })
        recycler_view.adapter = adapter
        viewModel.loadAllBudgets()
    }
}

class BudgetsAdapter(val context: Context?) : ListAdapter<Budget, BudgetViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(TextView(context))
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        (holder.itemView as TextView).setText(getItem(position).title)
    }
}

class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

}
