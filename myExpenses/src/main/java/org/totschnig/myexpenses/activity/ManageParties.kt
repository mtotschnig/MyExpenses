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
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.viewmodel.DebtViewModel

class ManageParties : DebtActivity() {
    override val debtViewModel: DebtViewModel by viewModels()
    private lateinit var listFragment: PartiesList
    fun configureFabMergeMode(mergeMode: Boolean) {
        configureFloatingActionButton(
            if (mergeMode) R.string.menu_merge else R.string.menu_create_party,
            if (mergeMode) R.drawable.ic_menu_split_transaction else R.drawable.ic_menu_add_fab
        )
    }

    fun setFabEnabled(enabled: Boolean) {
        floatingActionButton.isEnabled = enabled
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_parties)
        setupToolbar()
        var title = 0
        val action = intent.asAction
        when (action) {
            Action.MANAGE -> {
                setHelpVariant(HelpVariant.manage, true)
                title = R.string.pref_manage_parties_title
            }
            Action.SELECT_FILTER -> {
                setHelpVariant(HelpVariant.select_filter, true)
                configureFloatingActionButton(R.string.select, R.drawable.ic_menu_done)
                setFabEnabled(false)
                title = R.string.search_payee
            }
            Action.SELECT_MAPPING -> {
                setHelpVariant(HelpVariant.select_mapping, true)
                title = R.string.select_payee
            }
        }
        if (title != 0) supportActionBar!!.setTitle(title)
        if (action == Action.SELECT_MAPPING || action == Action.MANAGE) {
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
}