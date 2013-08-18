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

package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

public class SelectGroupingDialogFragment extends DialogFragment implements OnClickListener {
  /**
   * @param account_id
   * @return
   */
  public static final SelectGroupingDialogFragment newInstance(
      Long accountId,int selectedIndex) {
    SelectGroupingDialogFragment dialogFragment = new SelectGroupingDialogFragment();
    Bundle args = new Bundle();
    args.putLong(KEY_ACCOUNTID, accountId);
    args.putInt("selected_index",selectedIndex);
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  //KEY_ROWID + " != " + getArguments().getLong("fromAccountId")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.dialog_title_select_grouping)
      .setSingleChoiceItems(R.array.grouping_entries,
          getArguments().getInt("selected_index"), this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    Account account = Account.getInstanceFromDb(getArguments().getLong(KEY_ACCOUNTID));
    account.grouping=Account.Grouping.values()[which];
    account.save();
    dismiss();
  }
}