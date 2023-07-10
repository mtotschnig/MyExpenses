package org.totschnig.myexpenses.fragment

import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey

class MainPreferenceFragment : BasePreferenceFragment() {

    lateinit var adapter: PreferenceGroupAdapter

    var isSlideable: Boolean = true
        set(value) {
            field = value
            val preferenceAdapterPosition = adapter.getPreferenceAdapterPosition(highlightedKey)
            if (!value) {
                view?.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.settings_two_pane_background_color,
                        null
                    )
                )

                adapter.notifyItemChanged(preferenceAdapterPosition)
            }
        }

    @State
    var highlightedKey: String = "category_manage"

    fun onLoadPreference(key: String) {
        val oldPosition = adapter.getPreferenceAdapterPosition(highlightedKey)
        val newPosition = adapter.getPreferenceAdapterPosition(key)
        highlightedKey = key
        adapter.notifyItemChanged(oldPosition)
        adapter.notifyItemChanged(newPosition)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            StateSaver.restoreInstanceState(this, it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_headers, rootKey)
        requirePreference<Preference>(PrefKey.CATEGORY_BACKUP_EXPORT).title =
            getString(R.string.pref_category_title_export) + " / " + getString(R.string.menu_backup)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen) =
        object : PreferenceGroupAdapter(preferenceScreen) {

            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                if (!isSlideable && getItem(position)?.key == highlightedKey) {
                    holder.itemView.setBackgroundColor(
                        ResourcesCompat.getColor(resources, R.color.activatedBackground, null)
                    )
                }
            }
        }.also {
            adapter = it
        }
}