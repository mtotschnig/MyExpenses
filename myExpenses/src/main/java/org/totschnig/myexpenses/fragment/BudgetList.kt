package org.totschnig.myexpenses.fragment

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.budgets.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Budget.Companion.DIFF_CALLBACK

class BudgetList : Fragment() {
    private lateinit var viewModel: BudgetViewModel
    private lateinit var selectionTracker: SelectionTracker<Long>
    private var actionMode: ActionMode? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.budgets, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(activity!!)[BudgetViewModel::class.java]
        val adapter = BudgetsAdapter(context)
        viewModel.data.observe(this, Observer {
            adapter.submitList(it)
        })
        recycler_view.adapter = adapter
        selectionTracker = SelectionTracker.Builder<Long>(
                "my_selection",
                recycler_view,
                StableIdKeyProvider(recycler_view),
                BudgetDetailsLookup(recycler_view),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
        ).build()
        adapter.tracker = selectionTracker
        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            val actionModeCallback = ActionModeCallback()
            override fun onSelectionChanged() {
                val selectedCount = selectionTracker.selection?.size() ?: 0
                if (selectedCount == 0) {
                    actionMode?.let {
                        it.finish()
                        actionMode = null
                    }
                } else {
                    (requireActivity() as? AppCompatActivity)?.let { activity ->
                        if (actionMode == null) {
                            actionMode = activity.startSupportActionMode(actionModeCallback)
                        }
                        actionMode?.let { actionModeCallback.configureMenu(it.menu) }
                    }
                    actionMode?.title = selectedCount.toString()
                }
            }
        })
        viewModel.loadAllBudgets()
    }

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val handled = when (item.itemId) {
                R.id.DELETE_COMMAND -> {
                    viewModel.deleteBudgets(selectionTracker.selection.toList())
                    true
                }
                R.id.EDIT_COMMAND -> {
                    true
                }
                else -> false
            }
            selectionTracker.clearSelection()
            return handled
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val menuInflater = requireActivity().menuInflater
            menuInflater.inflate(R.menu.common_context, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            configureMenu(menu)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selectionTracker.clearSelection()
        }

        fun configureMenu(menu: Menu) {
            menu.setGroupVisible(R.id.MenuSingle, selectionTracker.selection?.size() ?: 0 == 1)
        }
    }
}

class BudgetsAdapter(val context: Context?) : ListAdapter<Budget, BudgetViewHolder>(DIFF_CALLBACK) {
    var tracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(LayoutInflater.from(context).inflate(R.layout.budget_list_row, parent, false))
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        getItem(position).let { budget ->
            val isSelected = tracker?.isSelected(budget.id) ?: false
            with(holder) {
                itemView.isActivated = isSelected
                title.setText(budget.title)
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

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition() = adapterPosition
                override fun getSelectionKey() = itemId
            }

}

class BudgetDetailsLookup(
        private val recyclerView: RecyclerView
) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as BudgetViewHolder)
                    .getItemDetails()
        }
        return null
    }
}

