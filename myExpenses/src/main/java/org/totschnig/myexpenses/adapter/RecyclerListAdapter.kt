/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.myexpenses.adapter

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.helper.ItemTouchHelperAdapter
import org.totschnig.myexpenses.adapter.helper.ItemTouchHelperViewHolder
import org.totschnig.myexpenses.adapter.helper.OnStartDragListener
import java.util.Collections
import java.util.Locale

@Parcelize
data class SortableItem(
    val id: Long,
    val text: String,
    @DrawableRes val icon: Int = 0
) : Parcelable

/**
 * Simple RecyclerView.Adapter that implements [ItemTouchHelperAdapter] to respond to move and
 * dismiss events from a [ItemTouchHelper].
 *
 * @author Paul Burke (ipaulpro)
 */
class RecyclerListAdapter(
    private val mDragStartListener: OnStartDragListener,
    val items: ArrayList<SortableItem>
) : RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>(), ItemTouchHelperAdapter {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_with_drag_handle, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.textView.text =
            String.format(Locale.getDefault(), "%d. %s", position + 1, items[position].text)
        holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            items[position].icon,
            0,
            0,
            0
        )


        // Start a drag whenever the handle view it touched
        holder.handleView.setOnTouchListener { v: View?, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    override fun onDrop() {
        notifyDataSetChanged()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Simple example of a view holder that implements [ItemTouchHelperViewHolder] and has a
     * "handle" view that initiates a drag event when touched.
     */
    class ItemViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        ItemTouchHelperViewHolder {
        val textView: TextView
        val handleView: ImageView

        init {
            textView = itemView.findViewById(R.id.text)
            handleView = itemView.findViewById(R.id.handle)
        }

        override fun onItemSelected() {
            itemView.setBackgroundColor(itemView.context.resources.getColor(R.color.activatedBackground))
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }
}
