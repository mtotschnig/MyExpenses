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
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.provider.filter.CrStatusCriteria;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

/**
 * uses {@link MessageDialogFragment.MessageDialogListener} to dispatch result back to activity
 *
 */
public class SelectCrStatusDialogFragment extends CommitSafeDialogFragment implements OnClickListener {
  /**
   * @param account_id
   * @return
   */
  public static final SelectCrStatusDialogFragment newInstance() {
    SelectCrStatusDialogFragment dialogFragment = new SelectCrStatusDialogFragment();
    //Bundle args = new Bundle();
    //dialogFragment.setArguments(args);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.search_status)
      .setSingleChoiceItems(R.array.crstatus_entries,-1,this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null) {
      return;
    }
    ((MyExpenses) getActivity()).addFilterCriteria(
        R.id.FILTER_STATUS_COMMAND,
        new CrStatusCriteria(which));
    dismiss();
  }
}