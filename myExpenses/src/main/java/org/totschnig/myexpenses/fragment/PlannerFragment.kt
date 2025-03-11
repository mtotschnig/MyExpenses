package org.totschnig.myexpenses.fragment

import android.app.Dialog
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.HELP_VARIANT_PLANNER
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.PlanInstanceBinding
import org.totschnig.myexpenses.databinding.PlannerFragmentBinding
import org.totschnig.myexpenses.dialog.DialogViewBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.PlannerViewModel
import org.totschnig.myexpenses.viewmodel.data.EventObserver
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceSet
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceUpdate
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject

fun configureMenuInternalPlanInstances(menu: Menu, state: PlanInstanceState) {
    //state open
    menu.findItem(R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND).isVisible =
        state == PlanInstanceState.OPEN
    menu.findItem(R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND).isVisible =
        state == PlanInstanceState.OPEN
    //state open or applied
    menu.findItem(R.id.CANCEL_PLAN_INSTANCE_COMMAND).isVisible =
        state == PlanInstanceState.OPEN || state == PlanInstanceState.APPLIED
    //state cancelled or applied
    menu.findItem(R.id.RESET_PLAN_INSTANCE_COMMAND).isVisible =
        state == PlanInstanceState.APPLIED || state == PlanInstanceState.CANCELLED
    //state applied
    menu.findItem(R.id.EDIT_PLAN_INSTANCE_COMMAND).isVisible = state == PlanInstanceState.APPLIED
}

class PlannerFragment : DialogViewBinding<PlannerFragmentBinding>() {

    private val viewModel: PlannerViewModel by viewModels()

    @State
    var instanceUriToUpdate: Uri? = null

    @State
    var selectedInstances: PlanInstanceSet = PlanInstanceSet()

    private lateinit var backgroundColor: ColorStateList

    private lateinit var stateObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        stateObserver = StateObserver()
        requireContext().contentResolver.registerContentObserver(
            TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            true, stateObserver
        )
        backgroundColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(
                ResourcesCompat.getColor(resources, R.color.activatedBackgroundPlanner, null),
                ResourcesCompat.getColor(resources, R.color.cardBackground, null)
            )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            context?.contentResolver?.unregisterContentObserver(stateObserver)
        } catch (ise: IllegalStateException) {
            // Do Nothing.  Observer has already been unregistered.
        }
    }

    private val adapter
        get() = binding.recyclerView.adapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            PlannerFragmentBinding.inflate(it)
        }
        val plannerAdapter = PlannerAdapter()
        binding.recyclerView.adapter = plannerAdapter
        binding.Title.movementMethod = LinkMovementMethod.getInstance()
        viewModel.getInstances().observe(this, EventObserver { list ->
            val previousCount = plannerAdapter.itemCount
            plannerAdapter.addData(list)
            val itemCount = plannerAdapter.itemCount
            if (previousCount > 0 && itemCount > 0) {
                binding.recyclerView.layoutManager?.scrollToPosition(if (list.first) itemCount - 1 else 0)
            }
        })
        viewModel.getTitle().observe(this) { title ->
            binding.Title.text = title
        }
        viewModel.getUpdates().observe(this) { update ->
            //Timber.d("Update posted")
            plannerAdapter.postUpdate(update)
        }
        viewModel.getBulkCompleted().observe(this, EventObserver { list ->
            list.forEach { planInstance ->
                viewModel.getUpdateFor(
                    TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                        planInstance.templateId,
                        planInstance.instanceId
                    )
                )
            }
        })
        viewModel.loadInstances()
        val alertDialog = builder
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.menu_create_instance_save, null)
            .create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { onBulkApply() }
            configureBulkApplyButton()

        }
        binding.HELPCOMMAND.setOnClickListener { view ->
            (activity as? ProtectedFragmentActivity)?.dispatchCommand(
                view.id,
                HELP_VARIANT_PLANNER
            )
        }
        return alertDialog
    }

    private fun onBulkApply() {
        viewModel.applyBulk(selectedInstances.toList())
        selectedInstances.clear()
        configureBulkApplyButton()
    }

    fun onEditRequestOk() {
        instanceUriToUpdate?.let {
            viewModel.getUpdateFor(it)
        }
    }

    inner class StateObserver : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Timber.d("received state change for uri: %s", uri)
            uri?.let { viewModel.getUpdateFor(it) }
        }
    }

    inner class PlannerAdapter : RecyclerView.Adapter<PlanInstanceViewHolder>() {
        val data = mutableListOf<PlanInstance>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanInstanceViewHolder {
            val itemBinding =
                PlanInstanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            itemBinding.root.setCardBackgroundColor(backgroundColor)
            return PlanInstanceViewHolder(itemBinding)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        fun addData(pair: Pair<Boolean, List<PlanInstance>>) {
            val (later, data) = pair
            val insertionPoint = if (later) this.data.size else 0
            this.data.addAll(insertionPoint, data)
            notifyItemRangeInserted(insertionPoint, data.size)
        }

        override fun onBindViewHolder(holder: PlanInstanceViewHolder, position: Int) {
            holder.bind(data[position], position)
        }

        fun postUpdate(update: PlanInstanceUpdate) {
            data.indexOfFirst { planInstance -> planInstance.templateId == update.templateId && planInstance.instanceId == update.instanceId }
                .takeIf { it != -1 }?.let { index ->
                    val oldInstance = data[index]
                    val amount = update.amount?.let { Money(oldInstance.amount.currencyUnit, it) }
                        ?: oldInstance.amount
                    data[index] = PlanInstance(
                        oldInstance.templateId,
                        update.transactionId,
                        oldInstance.title,
                        oldInstance.date,
                        oldInstance.color,
                        amount,
                        update.newState,
                        oldInstance.sealed
                    )
                    notifyItemChanged(index)
                }
        }
    }

    inner class PlanInstanceViewHolder(private val itemBinding: PlanInstanceBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        @Inject
        lateinit var currencyFormatter: ICurrencyFormatter
        private val formatter: DateTimeFormatter = getDateTimeFormatter(itemBinding.root.context)

        init {
            itemBinding.root.context.injector.inject(this)
        }

        fun bind(planInstance: PlanInstance, position: Int) {
            with(itemBinding) {
                root.isSelected = selectedInstances.contains(planInstance)
                date.text = planInstance.localDate.format(formatter)
                label.text = planInstance.title
                state.setImageResource(
                    when (planInstance.state) {
                        PlanInstanceState.OPEN -> R.drawable.ic_stat_open
                        PlanInstanceState.APPLIED -> R.drawable.ic_stat_applied
                        PlanInstanceState.CANCELLED -> R.drawable.ic_stat_cancelled
                    }
                )
                colorAccount.setBackgroundColor(planInstance.color)
                amount.text = currencyFormatter.formatMoney(planInstance.amount)
                amount.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        if (planInstance.amount.amountMinor < 0) R.color.colorExpenseOnCard else R.color.colorIncomeOnCard,
                        null
                    )
                )
                val templatesList = parentFragment as TemplatesList
                root.setOnLongClickListener {
                    if (planInstance.sealed) {
                        warnSealed(templatesList)
                        true
                    } else onSelection(planInstance, bindingAdapterPosition)
                }
                templatesList.configureOnClickPopup(root,
                    planInstance.let {
                        PlanInstanceInfo(
                            it.templateId,
                            it.instanceId,
                            it.date,
                            it.transactionId,
                            it.state
                        )
                    },
                    onClick = {
                        if (planInstance.sealed) {
                            warnSealed(templatesList)
                            return@configureOnClickPopup true
                        } else if (selectedInstances.size > 0) {
                            if (onSelection(planInstance, bindingAdapterPosition))
                                return@configureOnClickPopup true
                        }
                        false
                    },
                    handleMenuItemClick = { itemId ->
                        when (itemId) {
                            R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND -> {
                                viewModel.applyBulk(listOf(planInstance))
                                return@configureOnClickPopup true
                            }

                            R.id.EDIT_PLAN_INSTANCE_COMMAND ->
                                instanceUriToUpdate =
                                    TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                                        planInstance.templateId,
                                        planInstance.instanceId
                                    )
                        }
                        false
                    }
                )
            }
        }

        private fun warnSealed(templatesList: TemplatesList) {
            templatesList.showSnackbar(
                this@PlannerFragment,
                getString(R.string.object_sealed)
            )
        }

        private fun onSelection(planInstance: PlanInstance, position: Int) =
            if (planInstance.state == PlanInstanceState.OPEN) {
                if (selectedInstances.contains(planInstance)) {
                    selectedInstances.remove(planInstance)
                } else {
                    selectedInstances.add(planInstance)
                }
                adapter?.notifyItemChanged(position)
                configureBulkApplyButton()
                true
            } else {
                false
            }
    }

    private fun configureBulkApplyButton() {
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEUTRAL)?.let {
            val enabled = selectedInstances.size > 0
            it.isEnabled = enabled
            it.text =
                if (enabled) "${getString(R.string.menu_create_instance_save)} (${selectedInstances.size})" else ""
        }
    }

}