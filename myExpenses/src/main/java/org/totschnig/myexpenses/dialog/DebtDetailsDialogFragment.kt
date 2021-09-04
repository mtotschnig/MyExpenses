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
import org.totschnig.myexpenses.util.epochMillis2LocalDate
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
                            epochMillis2LocalDate(debt.date * 1000),
                            null,
                            debt.amount
                        )
                    ) + it
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder()
        val recyclerView = RecyclerView(builder.context)
        val padding = resources.getDimensionPixelSize(R.dimen.general_padding)
        recyclerView.setPadding(padding, 0, padding, 0)
        recyclerView.layoutManager = LinearLayoutManager(builder.context)
        adapter = Adapter()
        recyclerView.adapter = adapter
        val alertDialog = builder.setTitle(R.string.progress_dialog_loading)
            .setIcon(R.drawable.balance_scale)
            .setView(recyclerView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.menu_edit, null)
            .create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                startActivity(Intent(context, DebtEdit::class.java).apply {
                    putExtra(KEY_PAYEEID, debt.payeeId)
                    putExtra(KEY_PAYEE_NAME, debt.payeeName)
                    putExtra(KEY_DEBT_ID, debt.id)
                })
            }
        }
        return alertDialog
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