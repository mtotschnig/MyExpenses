package org.totschnig.myexpenses.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import javax.inject.Inject

abstract class BasePreferenceFragment: PreferenceFragmentCompat() {

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        with(requireActivity().injector) {
            inject(this@BasePreferenceFragment)
        }
        super.onCreate(savedInstanceState)
    }

    fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun unsetIconSpaceReservedRecursive(preferenceGroup: PreferenceGroup) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                unsetIconSpaceReservedRecursive(preference)
            }
            preference.isIconSpaceReserved = false
        }
    }

}