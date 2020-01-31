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

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.PartiesList;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import androidx.annotation.NonNull;
import eltos.simpledialogfragment.input.SimpleInputDialog;

public class ManageParties extends ProtectedFragmentActivity implements
    SimpleInputDialog.OnDialogResultListener {
  private static final String DIALOG_NEW_PARTY = "dialogNewParty";
  Payee mParty;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_parties);
    setupToolbar(true);
    setTitle(R.string.pref_manage_parties_title);
    configureFloatingActionButton(R.string.menu_create_party);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.CREATE_COMMAND) {
      SimpleInputDialog.build()
          .title(R.string.menu_create_party)
          .cancelable(false)
          .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
          .hint(R.string.label)
          .pos(R.string.dialog_button_add)
          .neut()
          .show(this, DIALOG_NEW_PARTY);
      return true;
    }
    return false;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if ((DIALOG_NEW_PARTY.equals(dialogTag) || PartiesList.DIALOG_EDIT_PARTY.equals(dialogTag))
        && which == BUTTON_POSITIVE) {
      mParty = new Payee(
          extras.getLong(DatabaseConstants.KEY_ROWID),
          extras.getString(SimpleInputDialog.TEXT));
      startDbWriteTask();
      finishActionMode();
      return true;
    }
    return false;
  }

  private void finishActionMode() {
    ContextualActionBarFragment listFragment = ((ContextualActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.parties_list));
    if (listFragment != null)
      listFragment.finishActionMode();
  }

  @Override
  public void onPostExecute(Uri result) {
    if (result == null) {
      showSnackbar(getString(R.string.already_defined,
              mParty != null ? mParty.getName() : ""),
          Snackbar.LENGTH_LONG);
    }
    super.onPostExecute(result);
  }

  @Override
  public Model getObject() {
    return mParty;
  }
}
