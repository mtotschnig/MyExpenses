package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.doOnLayout
import androidx.preference.*
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceFragment
import org.totschnig.myexpenses.fragment.preferences.MainPreferenceFragment
import org.totschnig.myexpenses.preference.PrefKey

class TwoPanePreference : PreferenceHeaderFragmentCompat() {

    override fun onCreatePreferenceHeader() = MainPreferenceFragment()

    private val initialScreen: String?
        get() = arguments?.getString(KEY_INITIAL_SCREEN)

    /** Only the [initialScreen] without the list of all screens, back leaves the settings */
    private val isSingleScreen: Boolean
        get() = arguments?.getBoolean(KEY_SINGLE_SCREEN) == true

    override fun selectInitialDetailPreference(headerFragment: PreferenceFragmentCompat) =
        if ((requireActivity() as PreferenceActivity).calledFromSystemSettings) null else
            (initialScreen?.let {
                headerFragment.preferenceScreen.findPreference<Preference>(it)
            } ?: super.selectInitialDetailPreference(headerFragment))
                ?.also { (headerFragment as MainPreferenceFragment).highlightedKey = it.key }

    @SuppressLint("MissingSuperCall")
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val started = if (headerFragment.isSlideable) {
            startFragment(caller, pref).also {
                if (it) {
                    requireActivity().title = pref.title
                    setHeaderFocusable(false)
                }
            }
        } else if (caller !is MainPreferenceFragment || headerFragment.highlightedKey != pref.key) {
            startFragment(caller, pref)
        } else false
        if (started && caller is MainPreferenceFragment) {
            caller.onLoadPreference(pref.key)
        }
        return true
    }

    private fun setHeaderFocusable(focusable: Boolean) {
        (headerFragment.view as? ViewGroup)?.descendantFocusability =
            if (focusable) ViewGroup.FOCUS_AFTER_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }

    private fun startFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ) = if ((requireActivity() as PreferenceActivity).protectionCheck(pref)) {
        super.onPreferenceStartFragment(caller, pref)
    } else false

    fun startPerformProtection() {
        val pref = headerFragment.requirePreference<Preference>(PrefKey.CATEGORY_SECURITY)
        super.onPreferenceStartFragment(headerFragment, pref)
        if (headerFragment.isSlideable) {
            requireActivity().title = pref.title
            setHeaderFocusable(false)
        }
        headerFragment.onLoadPreference(pref.key)
    }

    val headerFragment: MainPreferenceFragment
        get() = childFragmentManager.findFragmentById(R.id.preferences_header) as MainPreferenceFragment

    inline fun <reified F : BasePreferenceFragment?> getDetailFragment(): F? = childFragmentManager
        .findFragmentById(R.id.preferences_detail) as? F

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isSingleScreen) {
            view.findViewById<View>(R.id.preferences_header).visibility = View.GONE
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                (requireActivity() as PreferenceActivity).doHome()
            }
        }

        slidingPaneLayout.addPanelSlideListener(object :
            SlidingPaneLayout.SimplePanelSlideListener() {
            override fun onPanelClosed(panel: View) {
                activity?.setTitle(org.totschnig.myexpenses.R.string.settings_label)
            }

        })

        slidingPaneLayout.doOnLayout {
            headerFragment.isSlideable = slidingPaneLayout.isSlideable
            if (isSingleScreen) {
                if (slidingPaneLayout.isSlideable && !slidingPaneLayout.isOpen) {
                    slidingPaneLayout.openPane()
                }
                ensureTitle()
            } else if (slidingPaneLayout.isSlideable) {
                if (slidingPaneLayout.isOpen) {
                    ensureTitle()
                } else {
                    if (initialScreen != null) {
                        slidingPaneLayout.openPane()
                    }
                }
            }
        }
    }

    private fun ensureTitle() {
        getDetailFragment<BasePreferenceFragment>()?.let {
            requireActivity().title = it.preferenceScreen?.title
            it.headerPreference?.isVisible = false
        }
    }

    fun doHome(): Boolean =
        if (isSingleScreen) {
            popDetail()
        } else if (slidingPaneLayout.isSlideable) {
            popDetail() || slidingPaneLayout.closePane().also {
                if (it) {
                    setHeaderFocusable(true)
                }
            }
        } else false

    private fun popDetail() = (childFragmentManager.backStackEntryCount > 0).also {
        if (it) {
            childFragmentManager.popBackStackImmediate()
            ensureTitle()
        }
    }

    companion object {
        const val KEY_INITIAL_SCREEN = "initialScreen"
        const val KEY_SINGLE_SCREEN = "singleScreen"
        fun newInstance(initialScreen: String?, singleScreen: Boolean = false) = TwoPanePreference().apply {
            initialScreen?.let {
                arguments = Bundle().apply {
                    putString(KEY_INITIAL_SCREEN, it)
                    putBoolean(KEY_SINGLE_SCREEN, singleScreen)
                }
            }
        }
    }
}