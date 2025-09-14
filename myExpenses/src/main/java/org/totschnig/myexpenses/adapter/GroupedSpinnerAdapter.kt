package org.totschnig.myexpenses.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.viewmodel.data.Account

sealed class SpinnerItem() {
    abstract val itemId: Long

    data class Header<H>(val header: H, override val itemId: Long) : SpinnerItem()
    data class Item<T : IdHolder>(val data: T) : SpinnerItem() {
        override val itemId: Long = data.id
        override fun toString() = data.toString()
    }

    data class Divider(override val itemId: Long) : SpinnerItem()
}

open class GroupedSpinnerAdapter<H, T : IdHolder>(
    val context: Context,
    private val itemToString: (T) -> String,
    private val headerToString: (H) -> String
) : BaseAdapter() {
    private val items: MutableList<SpinnerItem> = mutableListOf()
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): SpinnerItem = items[position]
    override fun getItemId(position: Int): Long = getItem(position).itemId

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun addAll(items: List<Pair<H, List<T>>>) {
        //if we have only one group, we skip display of headers
        if (items.size == 1) {
            items.first().second.forEach {
                this.items.add(SpinnerItem.Item(it))
            }
        } else {
            var itemId = 0L
            items.forEachIndexed { index, (header, data) ->
                if (showHeader(header)) {
                    this.items.add(SpinnerItem.Header(header, itemId--))
                }
                data.forEach {
                    this.items.add(SpinnerItem.Item(it))
                }
                if (index < items.lastIndex) {
                    this.items.add(SpinnerItem.Divider(itemId--))
                }
            }
        }
        notifyDataSetChanged()
    }

    fun getPosition(item: T) = getPosition(item.id)
    fun getPosition(id: Long) = items.indexOfFirst { it.itemId == id }
    fun getFirstSelectable(): IndexedValue<SpinnerItem>? = items.withIndex().firstOrNull { (_, item) -> item is SpinnerItem.Item<*> }

    open fun showHeader(header: H): Boolean = true

    override fun isEnabled(position: Int): Boolean =
        items[position] is SpinnerItem.Item<*>

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        return ((convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)) as TextView).apply {
            @Suppress("UNCHECKED_CAST")
            text = if (item is SpinnerItem.Item<*>) itemToString(item.data as T) else ""
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        return when (item) {
            is SpinnerItem.Header<*> -> {
                @Suppress("UNCHECKED_CAST")
                val header = item.header as H
                LayoutInflater.from(context)
                    .inflate(R.layout.spinner_header_item, parent, false).apply {
                        (this as TextView).apply {
                            text = headerToString(header)
                        }
                    }
            }

            is SpinnerItem.Item<*> -> {
                @Suppress("UNCHECKED_CAST")
                val data = item.data as T
                LayoutInflater.from(context).inflate(
                    androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                    parent,
                    false
                ).apply {
                    (this as TextView).text = itemToString(data)
                }
            }

            is SpinnerItem.Divider -> {
                View(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (2 * resources.displayMetrics.density).toInt()
                    )
                    val colorOutline = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline,
                        Color.GRAY)
                    setBackgroundColor(colorOutline)
                }
            }
        }
    }
}

class AccountAdapter(context: Context): GroupedSpinnerAdapter<AccountFlag, Account>(
    context,
    itemToString = { it.label },
    headerToString = { if (it.id == 0L) "" else it.localizedLabel(context) }
) {
    override fun showHeader(header: AccountFlag) = header.id != 0L
}