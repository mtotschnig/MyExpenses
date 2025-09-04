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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.viewmodel.DebtViewModel

const val HELP_VARIANT_MERGE_MODE = "mergeMode"

class ManageParties : DebtActivity() {
    override val debtViewModel: DebtViewModel by viewModels()
    private val listFragment: PartiesList
        get() =  supportFragmentManager.findFragmentById(R.id.fragment_container) as PartiesList

    fun setFabEnabled(enabled: Boolean) {
        floatingActionButton.isEnabled = enabled
    }

    val mergeMode: Boolean
        get() = listFragment.mergeMode

    override val fabDescription: Int
        get() = when {
            intent.asAction == Action.SELECT_FILTER -> R.string.select
            mergeMode -> R.string.menu_merge
            else -> R.string.menu_create_party
        }

    override val fabIcon: Int
        get() = when {
            intent.asAction == Action.SELECT_FILTER -> R.drawable.ic_menu_done
            mergeMode -> R.drawable.ic_menu_split_transaction
            else -> R.drawable.ic_menu_add_fab
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null) {
            PartiesList()
        }
        setupToolbar()
        val title = when (intent.asAction) {
            Action.MANAGE -> {
                setHelpVariant(HELP_VARIANT_MANGE, true)
                R.string.pref_manage_parties_title
            }

            Action.SELECT_FILTER -> {
                setHelpVariant(HELP_VARIANT_SELECT_FILTER, true)
                setFabEnabled(false)
                R.string.search_payee
            }

            Action.SELECT_MAPPING -> {
                setHelpVariant(HELP_VARIANT_SELECT_MAPPING, true)
                R.string.select_payee
            }
        }
        if (title != 0) supportActionBar!!.setTitle(title)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        return super.dispatchCommand(command, tag) || when (command) {
            R.id.DELETE_COMMAND -> {
                listFragment.doDelete((tag as Bundle).getLong(KEY_ROWID))
                true
            }
            R.id.CLEANUP_COMMAND_DO -> {
                listFragment.doCleanup()
                true
            }
            else -> false
        }
    }

    override val fabActionName = "CREATE_PARTY"

    override fun onFabClicked() {
        super.onFabClicked()
        listFragment.dispatchFabClick()
    }
}