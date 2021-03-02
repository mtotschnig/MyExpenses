package org.totschnig.myexpenses.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SplitPartRowBinding
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Transaction

class SplitPartRVAdapter(context: Context, val currencyUnit: CurrencyUnit, val currencyFormatter: CurrencyFormatter, private val splitList: List<Transaction>) : RecyclerView.Adapter<SplitPartRVAdapter.ViewHolder>() {
    val colorExpense: Int = context.resources.getColor(R.color.colorExpense)
    val colorIncome: Int = context.resources.getColor(R.color.colorIncome)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SplitPartRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return splitList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(splitList[position])
    }

    inner class ViewHolder(private val binding: SplitPartRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.amount.apply {
                text = currencyFormatter.formatCurrency(transaction.amount)
                setTextColor(if (transaction.amount.amountMinor < 0L) {
                    colorExpense
                } else {
                    colorIncome
                })
            }
            binding.category.apply {
                var catText =  when {
                    transaction.isTransfer -> Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor) + transaction.label
                    transaction.catId == null -> Category.NO_CATEGORY_ASSIGNED_LABEL
                    else -> transaction.label
                }
                if (!TextUtils.isEmpty(transaction.comment)) {
                    catText += (if (catText == "") "" else " / ") + "<i>" + transaction.comment + "</i>"
                }
                text = HtmlCompat.fromHtml(catText, FROM_HTML_MODE_LEGACY)
            }
        }
    }
}