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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.HelpDialogFragment.Companion.newInstance
import org.totschnig.myexpenses.util.Utils

class Help : ProtectedFragmentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newInstance(
            context = intent.getStringExtra(HelpDialogFragment.KEY_CONTEXT)
                ?: throw java.lang.IllegalArgumentException("context extra missing"),
            variant = intent.getStringExtra(HelpDialogFragment.KEY_VARIANT),
            title = intent.getStringExtra(HelpDialogFragment.KEY_TITLE)
        ).show(supportFragmentManager, "HELP")
    }

    override val snackBarContainerId get() = R.id.content
}