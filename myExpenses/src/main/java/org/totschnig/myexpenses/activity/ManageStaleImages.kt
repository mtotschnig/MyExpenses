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
import org.totschnig.myexpenses.fragment.StaleImagesList

class ManageStaleImages : ProtectedFragmentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null) {
            StaleImagesList()
        }
        setupToolbar()
        title = "Stale images"
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as StaleImagesList).saveImageDo()
    }
}
