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

package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.R;

public class ManageMethods extends ProtectedFragmentActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_methods);
    setupToolbar(true);
    setTitle(R.string.pref_manage_methods_title);
    configureFloatingActionButton(R.string.menu_create_method);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.CREATE_COMMAND) {
      Intent i = new Intent(this, MethodEdit.class);
      startActivity(i);
      return true;
    }
    return false;
  }
}
