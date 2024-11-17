package org.totschnig.myexpenses.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ContextAwareRecyclerView : RecyclerView {
    private var contextMenuInfo: RecyclerContextMenuInfo? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    override fun getContextMenuInfo(): RecyclerContextMenuInfo? {
        return contextMenuInfo
    }

    override fun showContextMenuForChild(originalView: View): Boolean {
        val position = getChildAdapterPosition(originalView)
        if (position >= 0) {
            contextMenuInfo =
                RecyclerContextMenuInfo(position, getChildViewHolder(originalView).itemId)
            return super.showContextMenuForChild(originalView)
        }
        return false
    }

    class RecyclerContextMenuInfo(val position: Int, val id: Long) : ContextMenuInfo
}
