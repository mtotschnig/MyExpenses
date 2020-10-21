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


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.util.UiUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;

public class BalanceDialogFragment extends CommitSafeDialogFragment implements OnClickListener {
  
  public static BalanceDialogFragment newInstance(Bundle bundle) {
    BalanceDialogFragment dialogFragment = new BalanceDialogFragment();
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(R.layout.balance);
    TextView reconciledTextView = dialogView.findViewById(R.id.TotalReconciled);
    UiUtils.configureAmountTextViewForHebrew(reconciledTextView);
    reconciledTextView.setText(getArguments().getString(KEY_RECONCILED_TOTAL));
    TextView clearedTextView = dialogView.findViewById(R.id.TotalCleared);
    UiUtils.configureAmountTextViewForHebrew(clearedTextView);
    clearedTextView.setText(getArguments().getString(KEY_CLEARED_TOTAL));
    return builder
      .setTitle(getString(R.string.dialog_title_balance_account,getArguments().getString(KEY_LABEL)))
      .setView(dialogView)
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
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.BALANCE_COMMAND_DO);
    ctx.onPositive(b, ((CheckBox) ((AlertDialog) dialog).findViewById(R.id.balance_delete)).isChecked());
  }
}
