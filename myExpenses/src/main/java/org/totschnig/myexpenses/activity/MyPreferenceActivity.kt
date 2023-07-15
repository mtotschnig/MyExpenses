/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.fragment.BaseSettingsFragment

/**
 * Present references screen defined in Layout file
 *
 * @author Michael Totschnig
 */
class MyPreferenceActivity : ProtectedFragmentActivity() {
    lateinit var binding: SettingsBinding

    private var initialPrefToShow: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                binding.fragmentContainer.id,
                BaseSettingsFragment.newInstance(intent.getStringExtra(ARG_PREFERENCE_ROOT)),
                FRAGMENT_TAG
            ).commit()
        }
        initialPrefToShow =
            if (savedInstanceState == null) intent.getStringExtra(KEY_OPEN_PREF_KEY) else null

    }

    override fun inflateHelpMenu(menu: Menu?) {
        //currently no help menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val KEY_OPEN_PREF_KEY = "openPrefKey"
        const val FRAGMENT_TAG = "Settings"
    }
}