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

    fun showPopupMenu(vararg items: String, listener: PopupMenu.OnMenuItemClickListener) {
        showPopupMenu(
            populateMenu = {
                items.forEachIndexed { index, s ->
                    it.add(Menu.NONE, index, Menu.NONE, s)
                }
            },
            listener
        )
    }

    fun showPopupMenu(populateMenu: (Menu) -> Unit, listener: PopupMenu.OnMenuItemClickListener) {
        PopupMenu(context, anchorView).apply {
            setOnMenuItemClickListener(listener)
            populateMenu(menu)
            show()
        }
    }
}