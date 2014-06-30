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
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

/**
 * uses {@link MessageDialogFragment.MessageDialogListener} to dispatch result back to activity
 *
 */
public class SelectGroupingDialogFragment extends CommitSafeDialogFragment implements OnClickListener {
  /**
   * @param account_id
   * @return
   */
  public static final SelectGroupingDialogFragment newInstance(
      int commandId,int selectedIndex) {
    SelectGroupingDialogFragment dialogFragment = new SelectGroupingDialogFragment();
    Bundle args = new Bundle();
    args.putInt("command_id",commandId);
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
    if (getActivity()==null) {
      return;
    }
    Bundle bundle = getArguments();
    ((MessageDialogListener) getActivity())
    .dispatchCommand(bundle.getInt("command_id"), which);
    dismiss();
  }
}