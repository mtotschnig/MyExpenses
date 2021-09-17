package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.DebtTransactionBinding
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel.Transaction
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject

class DebtDetailsDialogFragment : BaseDialogFragment() {
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    val viewModel: DebtViewModel by viewModels()

    lateinit var adapter: Adapter
    lateinit var debt: Debt
    lateinit var currency: CurrencyUnit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().applicationContext as MyApplication).appComponent) {
            inject(this@DebtDetailsDialogFragment)
            inject(viewModel)
        }
        val debtId = requireArguments().getLong(KEY_DEBT_ID)
        viewModel.loadDebt(debtId).observe(this) { debt ->
            (dialog as? AlertDialog)?.setTitle(debt.label)
            this.debt = debt
            this.currency = currencyContext[debt.currency]
            viewModel.loadTransactions(debtId, debt.amount).observe(this) {
                adapter.submitList(
                    listOf(
                        Transaction(
                            0,
                            epoch2LocalDate(debt.date),
                            null,
                            debt.amount
                        )
                    ) + it
                )
            }
            configureSealed()
        }
    }

    private fun configureSealed() {
        (dialog as? AlertDialog)?.let {
            it.setIcon(if (debt.isSealed) R.drawable.ic_lock else R.drawable.balance_scale)
            it.getButton(AlertDialog.BUTTON_NEUTRAL).setText(if (debt.isSealed) R.string.menu_reopen else R.string.menu_edit)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder()
        val recyclerView = RecyclerView(builder.context)
        val paddingSide = resources.getDimensionPixelSize(R.dimen.padding_dialog_side)
        val paddingTop = resources.getDimensionPixelSize(R.dimen.padding_dialog_content_top)
        recyclerView.setPadding(paddingSide, paddingTop, paddingSide, 0)
        recyclerView.layoutManager = LinearLayoutManager(builder.context)
        adapter = Adapter()
        recyclerView.adapter = adapter
        val alertDialog = builder.setTitle(R.string.progress_dialog_loading)
            .setIcon(R.drawable.balance_scale)
            .setView(recyclerView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.menu_edit, null)
            .setNegativeButton(R.string.menu_delete, null)
            .create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (::debt.isInitialized) {
                    if (debt.isSealed) {
                        viewModel.reopenDebt(debt.id)
                    } else {
                        startActivity(Intent(context, DebtEdit::class.java).apply {
                            putExtra(KEY_PAYEEID, debt.payeeId)
                            putExtra(KEY_PAYEE_NAME, debt.payeeName)
                            putExtra(KEY_DEBT_ID, debt.id)
                        })
                    }
                }
            }
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                val count = adapter.itemCount - 1
                MessageDialogFragment.newInstance(
                    getString(R.string.dialog_title_delete_debt),
                    "${
                        resources.getQuantityString(
                            R.plurals.debt_mapped_transactions,
                            count,
                            debt.label,
                            count
                        )
                    } ${getString(R.string.continue_confirmation)}",
                    MessageDialogFragment.Button(
                        R.string.menu_delete,
                        R.id.DELETE_DEBT_COMMAND,
                        debt.id
                    ),
                    null,
                    MessageDialogFragment.noButton(), 0
                )
                    .show(parentFragmentManager, "DELETE_DEBT")
            }
        }
        return alertDialog
    }

    fun deleteDebtDo(debtId: Long) {
        viewModel.deleteDebt(debtId).observe(this) {
            if (it) {
                dismiss()
            } else {
                showSnackbar("ERROR", Snackbar.LENGTH_LONG, null)
            }
        }
    }

    companion object {
        fun newInstance(debtId: Long) = DebtDetailsDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(KEY_DEBT_ID, debtId)
            }
        }

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class ViewHolder(val binding: DebtTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val colorIncome =
            ResourcesCompat.getColor(itemView.context.resources, R.color.colorIncome, null)
        val colorExpense =
            ResourcesCompat.getColor(itemView.context.resources, R.color.colorExpense, null)

        fun bind(item: Transaction, boldBalance: Boolean) {
            binding.Date.text = item.date.toString()
            item.amount?.let { amount ->
                binding.Amount.text = currencyFormatter.convAmount(amount, currency)
                val direction = when {
                    amount > 0 -> Transfer.RIGHT_ARROW
                    amount < 0 -> Transfer.LEFT_ARROW
                    else -> ""
                }
                //noinspection SetTextI18n
                binding.Payee.text = "$direction ${debt.payeeName}"
            }
            with(binding.RunningBalance) {
                text = currencyFormatter.convAmount(item.runningTotal, currency)
                when {
                    item.runningTotal > 0 -> setTextColor(colorIncome)
                    item.runningTotal < 0 -> setTextColor(colorExpense)
                    else -> setTextColor((activity as ProtectedFragmentActivity).textColorSecondary)
                }
                setTypeface(typeface, if (boldBalance) Typeface.BOLD else Typeface.NORMAL)
            }
        }

    }

    inner class Adapter : ListAdapter<Transaction, ViewHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(DebtTransactionBinding.inflate(LayoutInflater.from(context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), position == itemCount - 1)
        }
    }
}