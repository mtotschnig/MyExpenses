package org.totschnig.myexpenses.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.totschnig.myexpenses.R

sealed class SpinnerItem() {
    abstract val itemId: Long
    data class Header<H>(val header: H, override val itemId: Long) : SpinnerItem()
    data class Item<T: IdHolder>(val data: T) : SpinnerItem() {
        override val itemId: Long = data.id
    }
}

class GroupedSpinnerAdapter<H, T: IdHolder>(
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
        items.forEachIndexed { index, (header, data) ->
            this.items.add(SpinnerItem.Header(header, -(index + 1L)))
            data.forEach {
                this.items.add(SpinnerItem.Item(it))
            }
        }
        notifyDataSetChanged()
    }

    fun getPosition(item: T) = getPosition(item.id)
    fun getPosition(id: Long) = items.indexOfFirst { it.itemId == id }

    override fun isEnabled(position: Int): Boolean =
        items[position] !is SpinnerItem.Header<*>

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        return if (item is SpinnerItem.Item<*>) {
            @Suppress("UNCHECKED_CAST")
            val data = item.data as T
            val view = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
            (view as TextView).text = itemToString(data)
            view
        } else {
            val view = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
            (view as TextView).text = ""
            view
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        return when (item) {
            is SpinnerItem.Header<*> -> {
                @Suppress("UNCHECKED_CAST")
                val header = item.header as H
                LayoutInflater.from(context).inflate(R.layout.spinner_header_item, parent, false).apply {
                    (this as TextView).text = headerToString(header)
                }
            }

            is SpinnerItem.Item<*> -> {
                @Suppress("UNCHECKED_CAST")
                val data = item.data as T
                LayoutInflater.from(context).inflate(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, parent, false).apply {
                    (this as TextView).text = itemToString(data)
                }
            }

        }
    }
}