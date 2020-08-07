package org.totschnig.myexpenses.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import icepick.Icepick
import icepick.State
import org.threeten.bp.format.DateTimeFormatter
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.PlanInstanceBinding
import org.totschnig.myexpenses.databinding.PlannerFragmentBinding
import org.totschnig.myexpenses.dialog.CommitSafeDialogFragment
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.CalendarProviderProxy.calculateId
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.PlannerViewModell
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceUpdate
import timber.log.Timber
import javax.inject.Inject

fun configureMenuInternalPlanInstances(menu: Menu, state: PlanInstanceState) {
    configureMenuInternalPlanInstances(menu, 1, state == PlanInstanceState.OPEN,
            state == PlanInstanceState.APPLIED,
            state == PlanInstanceState.CANCELLED)
}

fun configureMenuInternalPlanInstances(menu: Menu, count: Int, withOpen: Boolean,
                                       withApplied: Boolean, withCancelled: Boolean) {
    //state open
    menu.findItem(R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND).isVisible = withOpen
    menu.findItem(R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND).isVisible = count == 1 && withOpen
    //state open or applied
    menu.findItem(R.id.CANCEL_PLAN_INSTANCE_COMMAND).isVisible = withOpen || withApplied
    //state cancelled or applied
    menu.findItem(R.id.RESET_PLAN_INSTANCE_COMMAND).isVisible = withApplied || withCancelled
    //state applied
    menu.findItem(R.id.EDIT_PLAN_INSTANCE_COMMAND).isVisible = count == 1 && withApplied
}

class PlannerFragment : CommitSafeDialogFragment(), DialogInterface.OnClickListener {

    private var _binding: PlannerFragmentBinding? = null

    // This property is only valid between onCreateDialog and onDestroyView.
    private val binding get() = _binding!!

    val model: PlannerViewModell by viewModels()

    @State @JvmField
    var instanceUriToUpdate: Uri? = null

    lateinit var stateObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
        stateObserver = StateObserver()
        context?.contentResolver?.registerContentObserver(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
                true, stateObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            context?.contentResolver?.unregisterContentObserver(stateObserver)
        } catch (ise: IllegalStateException) {
            // Do Nothing.  Observer has already been unregistered.
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PlannerFragmentBinding.inflate(LayoutInflater.from(activity), null, false)
        val plannerAdapter = PlannerAdapter()
        binding.recyclerView.adapter = plannerAdapter
        model.getInstances().observe(this, Observer { list ->
            val previousCount = plannerAdapter.itemCount
            plannerAdapter.addData(list)
            val itemCount = plannerAdapter.itemCount
            if (previousCount > 0 && itemCount > 0) {
                binding.recyclerView.layoutManager?.scrollToPosition(if (list.first) itemCount - 1 else 0)
            }
        })
        model.getTitle().observe(this, Observer { title ->
            binding.Title.setText(title)
        })
        model.getUpdates().observe(this, Observer { update ->
            plannerAdapter.postUpdate(update)
        })
        if (savedInstanceState == null) {
            model.loadInstances()
        }
        val alertDialog = AlertDialog.Builder(requireContext())
                .setView(binding.root)
                .setPositiveButton("Later", this)
                .setNegativeButton("Earlier", this)
                .create()
        alertDialog.setOnShowListener { dialog ->
            disableDismiss(alertDialog, AlertDialog.BUTTON_POSITIVE)
            disableDismiss(alertDialog, AlertDialog.BUTTON_NEGATIVE)
        }
        binding.CloseDialog.setOnClickListener { view -> dismiss() }
        binding.HELPCOMMAND.setOnClickListener { view ->
            (activity as? ProtectedFragmentActivity)?.dispatchCommand(view.id,
                    ManageTemplates.HelpVariant.planner.name) }
        return alertDialog
    }

    private fun disableDismiss(alertDialog: AlertDialog, which: Int) {
        alertDialog.getButton(which).setOnClickListener { v: View? -> onClick(alertDialog, which) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        model.loadInstances(which == AlertDialog.BUTTON_POSITIVE)
    }

    fun onEditRequestOk() {
        instanceUriToUpdate?.let {
            model.getUpdateFor(it)
        }
    }

    inner class StateObserver : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            Timber.d("received state change for uri: %s", uri)
            model.getUpdateFor(uri)
        }
    }

    inner class PlannerAdapter : RecyclerView.Adapter<PlanInstanceViewHolder>() {
        val data = mutableListOf<PlanInstance>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanInstanceViewHolder {
            val itemBinding = PlanInstanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
            holder.bind(data[position])
        }

        fun postUpdate(update: PlanInstanceUpdate) {
            data.indexOfFirst { planInstance -> planInstance.templateId == update.templateId && calculateId(planInstance.date) == update.instanceId   }
                    .takeIf { it != -1 }?.let { index ->
                        val oldInstance = data[index]
                        val amount = update.amount?.let { Money(oldInstance.amount.currencyUnit, it) } ?: oldInstance.amount
                        data.set(index,
                                PlanInstance(oldInstance.templateId, update.transactionId, oldInstance.title, oldInstance.date, oldInstance.color,
                                        amount, update.newState))
                        notifyItemChanged(index)
                    }
        }
    }

    inner class PlanInstanceViewHolder(private val itemBinding: PlanInstanceBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        @Inject
        lateinit var currencyFormatter: CurrencyFormatter
        private val formatter : DateTimeFormatter = getDateTimeFormatter(itemBinding.root.context)

        init {
            (itemBinding.root.context.applicationContext as MyApplication).appComponent.inject(this)
        }

        fun bind(planInstance: PlanInstance) {
            with(itemBinding) {
                date.text = planInstance.localDate.format(formatter)
                label.text = planInstance.title
                state.setImageResource(when (planInstance.state) {
                    PlanInstanceState.OPEN -> R.drawable.ic_stat_open
                    PlanInstanceState.APPLIED -> R.drawable.ic_stat_applied
                    PlanInstanceState.CANCELLED -> R.drawable.ic_stat_cancelled
                })
                colorAccount.setBackgroundColor(planInstance.color)
                amount.setText(currencyFormatter.formatCurrency(planInstance.amount))
                amount.setTextColor(UiUtils.themeIntAttr(root.context,
                        if (planInstance.amount.amountMinor < 0) R.attr.colorExpense else R.attr.colorIncome))
                root.setOnClickListener {
                    val popup = PopupMenu(root.context, root)
                    popup.inflate(R.menu.planlist_context)
                    configureMenuInternalPlanInstances(popup.menu, planInstance.state)
                    popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                        override fun onMenuItemClick(item: MenuItem): Boolean {
                            val templatesList = parentFragment as? TemplatesList
                            val instanceId = calculateId(planInstance.date)
                            return when (item.getItemId()) {
                                R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND -> {
                                    templatesList?.dispatchCreateInstanceEdit(
                                            planInstance.templateId, instanceId,
                                            planInstance.date)
                                    true
                                }
                                R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND -> {
                                    templatesList?.dispatchCreateInstanceSaveDo(arrayOf(planInstance.templateId), arrayOf(arrayOf(instanceId, planInstance.date)))
                                    true

                                }
                                R.id.EDIT_PLAN_INSTANCE_COMMAND -> {
                                    instanceUriToUpdate = TransactionProvider.PLAN_INSTANCE_SINGLE_URI(planInstance.templateId, instanceId)
                                    templatesList?.dispatchEditInstance(planInstance.transactionId)
                                    true
                                }
                                R.id.CANCEL_PLAN_INSTANCE_COMMAND -> {
                                    templatesList?.dispatchTask(TaskExecutionFragment.TASK_CANCEL_PLAN_INSTANCE, arrayOf(instanceId), arrayOf(arrayOf(planInstance.templateId, planInstance.transactionId)))
                                    true
                                }
                                R.id.RESET_PLAN_INSTANCE_COMMAND -> {
                                    templatesList?.dispatchTask(TaskExecutionFragment.TASK_RESET_PLAN_INSTANCE, arrayOf(instanceId), arrayOf(arrayOf(planInstance.templateId, planInstance.transactionId)))
                                    true
                                }
                                else -> false
                            }
                        }
                    })
                    //displaying the popup
                    popup.show()
                }
            }

        }
    }

}