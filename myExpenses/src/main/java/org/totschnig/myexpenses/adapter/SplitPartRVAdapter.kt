package org.totschnig.myexpenses.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.underline
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SplitPartRowBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.getIndicatorPrefixForLabel

class SplitPartRVAdapter(
    context: Context,
    var currencyUnit: CurrencyUnit,
    val currencyFormatter: ICurrencyFormatter,
    private val onItemClicked: ((View, SplitPart) -> Unit)? = null
) :
    ListAdapter<SplitPartRVAdapter.SplitPart, SplitPartRVAdapter.ViewHolder>(DIFF_CALLBACK) {
    val colorExpense: Int = ContextCompat.getColor(context, R.color.colorExpense)
    val colorIncome: Int = ContextCompat.getColor(context, R.color.colorIncome)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SplitPartRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).apply {
            onItemClicked?.let { onClick ->
                itemView.setOnClickListener { view ->
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onClick.invoke(view, getItem(pos))
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: SplitPartRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: SplitPart) {
            transaction.icon?.let { binding.icon.setImageDrawable(IIconInfo.resolveIcon(it)?.asDrawable(binding.root.context)) }
            binding.amount.apply {
                text = currencyFormatter.formatMoney(transaction.amount)
                setTextColor(
                    if (transaction.amount.amountMinor < 0L) {
                        colorExpense
                    } else {
                        colorIncome
                    }
                )
            }
            binding.category.text = buildSpannedString {
                append(
                    when {
                        transaction.isTransfer -> getIndicatorPrefixForLabel(transaction.amount.amountMinor) + transaction.transferAccount
                        else -> transaction.categoryPath ?: ""
                    }
                )
                transaction.comment.takeIf { !it.isNullOrBlank() }?.let {
                    if (isNotEmpty()) {
                        append(" / ")
                    }
                    italic {
                        append(it)
                    }
                }
                transaction.debtLabel.takeIf { !it.isNullOrBlank() }?.let {
                    if (isNotEmpty()) {
                        append(" / ")
                    }
                    underline {
                        append(it)
                    }
                }
                transaction.tags.takeIf { it.isNotEmpty() }?.let {
                    if (isNotEmpty()) {
                        append(" / ")
                    }
                    bold {
                        it.forEachIndexed { index, tag ->
                            tag.color?.also { color ->
                                color(color) {
                                    append(tag.label)
                                }
                            } ?: run {
                                append(tag.label)
                            }
                            if (index < it.size - 1) {
                                append(", ")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SplitPart>() {
            override fun areItemsTheSame(oldItem: SplitPart, newItem: SplitPart): Boolean {
                return oldItem.uuid == newItem.uuid
            }

            override fun areContentsTheSame(oldItem: SplitPart, newItem: SplitPart): Boolean {
                return oldItem == newItem
            }
        }
    }

    data class SplitPart(
        val amount: Money,
        val comment: String?,
        val categoryPath: String?,
        val transferAccount: String?,
        val debtLabel: String?,
        val tags: List<Tag>,
        val icon: String?,
        val uuid: String
    )  {
        val isTransfer
            get() = transferAccount != null
    }
}