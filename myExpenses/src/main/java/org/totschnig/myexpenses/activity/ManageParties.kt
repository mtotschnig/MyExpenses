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

import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.DebtDetailsDialogFragment
import org.totschnig.myexpenses.fragment.PartiesList

class ManageParties : DebtActivity() {
    private lateinit var listFragment: PartiesList
    fun configureFabMergeMode(mergeMode: Boolean) {
        configureFloatingActionButton(
            if (mergeMode) R.string.menu_merge else R.string.menu_create_party,
            if (mergeMode) R.drawable.ic_menu_split_transaction else R.drawable.ic_menu_add_fab
        )
    }

    fun setFabEnabled(enabled: Boolean) {
        floatingActionButton?.isEnabled = enabled
    }

    enum class HelpVariant {
        manage, select_mapping, select_filter
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_parties)
        setupToolbar(true)
        var title = 0
        when (action) {
            Intent.ACTION_MAIN, ACTION_MANAGE -> {
                setHelpVariant(HelpVariant.manage, true)
                title = R.string.pref_manage_parties_title
            }
            ACTION_SELECT_FILTER -> {
                setHelpVariant(HelpVariant.select_filter, true)
                configureFloatingActionButton(R.string.select, R.drawable.ic_menu_done)
                setFabEnabled(false)
                title = R.string.search_payee
            }
            ACTION_SELECT_MAPPING -> {
                setHelpVariant(HelpVariant.select_mapping, true)
                title = R.string.select_payee
            }
        }
        if (title != 0) supportActionBar!!.setTitle(title)
        if (action == ACTION_SELECT_MAPPING || action == ACTION_MANAGE) {
            configureFabMergeMode(false)
        }
        listFragment = supportFragmentManager.findFragmentById(R.id.parties_list) as PartiesList
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        if (command == R.id.CREATE_COMMAND) {
            listFragment.dispatchFabClick()
            return true
        }
        return false
    }

    val action: String
        get() = intent.action ?: ACTION_MANAGE
}