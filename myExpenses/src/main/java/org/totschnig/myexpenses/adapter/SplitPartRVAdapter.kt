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
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.Tag

class SplitPartRVAdapter(
    context: Context,
    var currencyUnit: CurrencyUnit,
    val currencyFormatter: ICurrencyFormatter,
    private val onItemClicked: ((View, ITransaction) -> Unit)? = null
) :
    ListAdapter<SplitPartRVAdapter.ITransaction, SplitPartRVAdapter.ViewHolder>(DIFF_CALLBACK) {
    val colorExpense: Int = ContextCompat.getColor(context, R.color.colorExpense)
    val colorIncome: Int = ContextCompat.getColor(context, R.color.colorIncome)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id

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
        fun bind(transaction: ITransaction) {
            transaction.icon?.let { binding.icon.setImageDrawable(IIconInfo.resolveIcon(it)?.asDrawable(binding.root.context)) }
            binding.amount.apply {
                text = currencyFormatter.formatMoney(Money(currencyUnit, transaction.amountRaw))
                setTextColor(
                    if (transaction.amountRaw < 0L) {
                        colorExpense
                    } else {
                        colorIncome
                    }
                )
            }
            binding.category.text = buildSpannedString {
                append(
                    when {
                        transaction.isTransfer -> Transfer.getIndicatorPrefixForLabel(transaction.amountRaw) + transaction.transferAccount
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
                transaction.tagList.takeIf { it.isNotEmpty() }?.let {
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
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ITransaction>() {
            override fun areItemsTheSame(oldItem: ITransaction, newItem: ITransaction): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ITransaction, newItem: ITransaction): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface ITransaction {
        val comment: String?
        val categoryPath: String?
        val transferAccount: String?
        val id: Long
        val amountRaw: Long
        val debtLabel: String?
        val tagList: List<Tag>
        val icon: String?

        val isTransfer
            get() = transferAccount != null

        override fun equals(other: Any?): Boolean
    }
}