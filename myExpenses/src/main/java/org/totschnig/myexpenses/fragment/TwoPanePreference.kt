package org.totschnig.myexpenses.fragment

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat

class TwoPanePreference : PreferenceHeaderFragmentCompat() {

    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        return MainPreferenceFragment()
    }
}