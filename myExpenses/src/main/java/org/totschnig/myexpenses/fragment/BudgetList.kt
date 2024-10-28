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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.databinding.BudgetListRowBinding
import org.totschnig.myexpenses.databinding.BudgetsBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.ui.addChipsBulk
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel.BudgetViewItem
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import javax.inject.Inject

class BudgetList : Fragment() {
    private var _binding: BudgetsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BudgetListViewModel by activityViewModels()

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var prefHandler: PrefHandler

    @State
    var lastClickedPosition: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        injector.inject(viewModel)
        viewModel.init()
        val activity = requireActivity()
        val adapter = BudgetsAdapter(activity)
        with(binding.recyclerView) {
            LinearLayoutManager(activity).also {
                layoutManager = it
                addItemDecoration(DividerItemDecoration(activity, it.orientation))
            }
        }
        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.enrichedData.collect {
                        adapter.submitList(it)
                        binding.empty.isVisible = it.isEmpty()
                        binding.recyclerView.isVisible = it.isNotEmpty()
                    }
                }
            }
        }

        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        lastClickedPosition?.let {
            if (resultCode != Activity.RESULT_FIRST_USER) { //budget was deleted
                binding.recyclerView.adapter?.notifyItemChanged(it)
            }
            lastClickedPosition = null
        }
    }

    val diffCallback = object : DiffUtil.ItemCallback<BudgetViewItem>() {
        override fun areItemsTheSame(oldItem: BudgetViewItem, newItem: BudgetViewItem): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: BudgetViewItem, newItem: BudgetViewItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class BudgetsAdapter(val context: Context) :
        ListAdapter<BudgetViewItem, BudgetViewHolder>(diffCallback) {

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            val binding = BudgetListRowBinding.inflate(LayoutInflater.from(context), parent, false)
            return BudgetViewHolder(binding)
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            getItem(position).let { (budget, info) ->
                if (info == null) {
                    viewModel.loadBudgetAmounts(budget)
                }
                with(holder.binding) {
                    Title.text = budget.titleComplete(context)

                    budgetSummary.bind(
                        budget,
                        currencyContext[budget.currency],
                        info?.spent?.unaryMinus() ?: 0L,
                        info?.allocated ?: 0L,
                        currencyFormatter
                    )

                    filter.addChipsBulk(buildList {
                        add(budget.label(requireContext()))
                        FilterPersistence(
                            prefHandler,
                            BudgetViewModel.prefNameForCriteria(budget.id),
                            null,
                            immediatePersist = false,
                            restoreFromPreferences = true
                        ).whereFilter.criteria.forEach { criterion ->
                            add(criterion.prettyPrint(context))
                        }
                    })

                    root.setOnClickListener {
                        val i = Intent(context, BudgetActivity::class.java)
                        i.putExtra(KEY_ROWID, budget.id)
                        i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        lastClickedPosition = holder.bindingAdapterPosition
                        startActivityForResult(i, 0)
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = getItem(position).budget.id
    }
}


class BudgetViewHolder(val binding: BudgetListRowBinding) : RecyclerView.ViewHolder(binding.root)

