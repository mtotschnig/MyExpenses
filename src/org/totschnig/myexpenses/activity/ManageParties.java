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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Payee;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageParties extends ProtectedFragmentActivity implements
    EditTextDialogListener, DbWriteFragment.TaskCallbacks {
  Cursor mPartiesCursor;
  Button mDeleteButton;
  Payee mParty;

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_parties);
    setTitle(R.string.pref_manage_parties_title);
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.parties, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (command == R.id.CREATE_COMMAND) {
      Bundle args = new Bundle();
      args.putString("dialogTitle", getString(R.string.menu_create_party));
      EditTextDialog.newInstance(args).show(getSupportFragmentManager(), "CREATE_PARTY");
      return true;
    }
    return super.dispatchCommand(command, tag);
   }
  @Override
  public void onFinishEditDialog(Bundle args) {
    mParty = new Payee(args.getLong("partyId"),args.getString("result"));
    getSupportFragmentManager().beginTransaction()
    .add(DbWriteFragment.newInstance(), "SAVE_TASK")
    .commit();
  }
  @Override
  public void onPostExecute(Uri result) {
    if (result == null)
      Toast.makeText(ManageParties.this,getString(R.string.already_defined, mParty.name), Toast.LENGTH_LONG).show();
  }
  @Override
  public Model getObject() {
    return mParty;
  }
}
