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
import org.totschnig.myexpenses.fragment.PartiesList;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.ConstantsKt.ACTION_MANAGE;
import static org.totschnig.myexpenses.ConstantsKt.ACTION_SELECT_FILTER;
import static org.totschnig.myexpenses.ConstantsKt.ACTION_SELECT_MAPPING;

public class ManageParties extends ProtectedFragmentActivity {
  protected PartiesList listFragment;

  public void configureFabMergeMode() {
    configureFloatingActionButton(R.string.menu_merge, R.drawable.ic_menu_split_transaction);
  }

  public enum HelpVariant {
    manage, select_mapping, select_filter
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    String action = getAction();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_parties);
    setupToolbar(true);
    int title = 0;
    switch (action) {
      case Intent.ACTION_MAIN:
      case ACTION_MANAGE:
        setHelpVariant(HelpVariant.manage, true);
        title = R.string.pref_manage_parties_title;
        break;
      case ACTION_SELECT_FILTER:
        setHelpVariant(HelpVariant.select_filter, true);
        configureFloatingActionButton(R.string.select, R.drawable.ic_menu_done);
        title = R.string.search_payee;
        break;
      case ACTION_SELECT_MAPPING:
        setHelpVariant(HelpVariant.select_mapping, true);
        title = R.string.select_payee;
    }
    if (title != 0) getSupportActionBar().setTitle(title);
    if (action.equals(ACTION_SELECT_MAPPING) || action.equals(ACTION_MANAGE)) {
      configureFloatingActionButton(R.string.menu_create_party);
    }
    listFragment = (PartiesList) getSupportFragmentManager().findFragmentById(R.id.parties_list);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.CREATE_COMMAND) {
      listFragment.dispatchFabClick();
      return true;
    }
    return false;
  }

  @NonNull
  public String getAction() {
    Intent intent = getIntent();
    String action = intent.getAction();
    return action == null ? ACTION_MANAGE : action;
  }
}
