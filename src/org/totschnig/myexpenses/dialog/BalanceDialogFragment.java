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


import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class BalanceDialogFragment extends DialogFragment implements OnClickListener {
  
  public static final BalanceDialogFragment newInstance(Bundle bundle) {
    BalanceDialogFragment dialogFragment = new BalanceDialogFragment();
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.balance, null);
    ((TextView) view.findViewById(R.id.TotalReconciled)).setText(getArguments().getString(KEY_RECONCILED_TOTAL));
    ((TextView) view.findViewById(R.id.TotalCleared)).setText(getArguments().getString(KEY_CLEARED_TOTAL));
    return new AlertDialog.Builder(getActivity())
      .setTitle(getString(R.string.dialog_title_balance_account,getArguments().getString(KEY_LABEL)))
      .setView(view)
      .setNegativeButton(android.R.string.cancel,null)
      .setPositiveButton(android.R.string.ok,this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (ctx==null) {
      return;
    }
    Bundle b = getArguments();
    b.putBoolean("deleteP",
        ((CheckBox) ((AlertDialog) dialog).findViewById(R.id.balance_delete)).isChecked());
    ctx.dispatchCommand(R.id.BALANCE_COMMAND_DO,b);
  }
}
