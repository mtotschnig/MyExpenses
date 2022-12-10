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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import icepick.State
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.databinding.BudgetListRowBinding
import org.totschnig.myexpenses.databinding.BudgetsBinding
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Budget.Companion.DIFF_CALLBACK
import javax.inject.Inject

class BudgetList : Fragment() {
    private var _binding: BudgetsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BudgetViewModel
    private var budgetAmounts: MutableMap<Long, Pair<Long, Long>?> = mutableMapOf()

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var prefHandler: PrefHandler

    @State
    @JvmField
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
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = requireActivity().let {
            viewModel = ViewModelProvider(it)[BudgetViewModel::class.java]
            (requireActivity().application as MyApplication).appComponent.inject(viewModel)
            BudgetsAdapter(it)
        }
        with(binding.recyclerView) {
            LinearLayoutManager(activity).also {
                layoutManager = it
                addItemDecoration(DividerItemDecoration(activity, it.orientation))
            }
        }
        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.data.collect {
                        adapter.submitList(it)
                        binding.empty.isVisible = it.isEmpty()
                        binding.recyclerView.isVisible = it.isNotEmpty()
                    }
                }
            }
        }

        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    viewModel.amounts.collect { (position, id, spent, allocated) ->
                        budgetAmounts[id] = spent to allocated
                        if (binding.recyclerView.isComputingLayout) {
                            CrashHandler.report(Exception("Budget amount received while recyclerView is computing layout"))
                            binding.recyclerView.post {
                                adapter.notifyItemChanged(position)
                            }
                        } else {
                            adapter.notifyItemChanged(position)
                        }
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

    inner class BudgetsAdapter(val context: Context) :
        ListAdapter<Budget, BudgetViewHolder>(DIFF_CALLBACK) {

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            val binding = BudgetListRowBinding.inflate(LayoutInflater.from(context), parent, false)
            return BudgetViewHolder(binding)
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            getItem(position).let { budget ->
                with(holder.binding) {
                    Title.text = budget.titleComplete(context)
                    val (spent, allocated) = budgetAmounts[budget.id] ?: run {
                        viewModel.loadBudgetAmounts(position, budget)
                        0L to 0L
                    }
                    budgetSummary.bind(budget, -spent, allocated, currencyFormatter)

                    filter.addChipsBulk(buildList {
                        this.add(budget.label(requireContext()))
                        FilterPersistence(
                            prefHandler,
                            BudgetViewModel.prefNameForCriteria(budget.id),
                            null,
                            immediatePersist = false,
                            restoreFromPreferences = true
                        ).whereFilter.criteria.forEach { criterion ->
                            this.add(
                                criterion.prettyPrint(context)
                            )
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

        override fun getItemId(position: Int): Long = getItem(position).id
    }
}


class BudgetViewHolder(val binding: BudgetListRowBinding) : RecyclerView.ViewHolder(binding.root)

