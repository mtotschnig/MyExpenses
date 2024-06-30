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
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.MethodList
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.MethodViewModel

class ManageMethods : ProtectedFragmentActivity() {

    private val viewModel by viewModels<MethodViewModel>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setupWithFragment(savedInstanceState == null) {
            MethodList()
        }
        setupToolbar()
        setTitle(R.string.pref_manage_methods_title)
    }

    override val fabDescription = R.string.menu_create_method

    override val fabActionName = "CREATE_METHOD"

    override fun onFabClicked() {
        super.onFabClicked()
        startActivity(Intent(this, MethodEdit::class.java))
    }

    fun deleteMethods(methodsIs: List<Long>) {
        viewModel.deleteMethods(methodsIs)
    }
}