package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class PopupMenuPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private lateinit var anchorView: View

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        anchorView = holder.itemView
    }

    fun showPopupMenu(listener: PopupMenu.OnMenuItemClickListener?, vararg items: String?) {
        val popup = PopupMenu(context, anchorView)
        val popupMenu = popup.menu
        popup.setOnMenuItemClickListener(listener)
        for (i in items.indices) {
            popupMenu.add(Menu.NONE, i, Menu.NONE, items[i])
        }
        popup.show()
    }
}