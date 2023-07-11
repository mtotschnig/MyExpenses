package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat
import androidx.preference.R
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceFragment
import org.totschnig.myexpenses.fragment.preferences.MainPreferenceFragment

class TwoPanePreference : PreferenceHeaderFragmentCompat() {

    override fun onCreatePreferenceHeader() = MainPreferenceFragment()

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        if (headerFragment.isSlideable) {
            super.onPreferenceStartFragment(caller, pref)
            requireActivity().title = pref.title
        } else if (caller !is MainPreferenceFragment || headerFragment.highlightedKey != pref.key) {
            super.onPreferenceStartFragment(caller, pref)
        }
        if (caller is MainPreferenceFragment) {
            headerFragment.onLoadPreference(pref.key)
        }
        return true
    }

    private val headerFragment: MainPreferenceFragment
        get() = childFragmentManager.findFragmentById(R.id.preferences_header) as MainPreferenceFragment

    @Suppress("UNCHECKED_CAST")
    fun <F : Fragment?> getDetailFragment(): F? = childFragmentManager
        .findFragmentById(R.id.preferences_detail) as? F

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slidingPaneLayout.addPanelSlideListener(object :
            SlidingPaneLayout.SimplePanelSlideListener() {
            override fun onPanelClosed(panel: View) {
                requireActivity().setTitle(org.totschnig.myexpenses.R.string.menu_settings)
            }

        })

        slidingPaneLayout.doOnLayout {
            headerFragment.isSlideable = slidingPaneLayout.isSlideable
            if (slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen) {
                ensureTitle()
            }
        }
    }

    private fun ensureTitle() {
        requireActivity().title = getDetailFragment<BasePreferenceFragment>()?.preferenceScreen?.title
    }

    fun doHome(): Boolean =
        if (slidingPaneLayout.isSlideable) {
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
                ensureTitle()
                true
            } else slidingPaneLayout.closePane()
        } else false
}