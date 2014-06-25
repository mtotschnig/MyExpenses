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

import java.text.DateFormat;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BalanceDialogFragment extends DialogFragment implements OnClickListener {
  
  public static final BalanceDialogFragment newInstance(String label, String reconciled, String cleared) {
    BalanceDialogFragment dialogFragment = new BalanceDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(KEY_LABEL, label);
    bundle.putString(KEY_RECONCILED_TOTAL, reconciled);
    bundle.putString(KEY_CLEARED_TOTAL, cleared);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.balance, null);
    ((TextView) view.findViewById(R.id.TotalReconciled)).setText(getArguments().getString(KEY_RECONCILED_TOTAL));
    ((TextView) view.findViewById(R.id.TotalCleared)).setText(getArguments().getString(KEY_CLEARED_TOTAL));
    return new AlertDialog.Builder(getActivity())
      .setTitle(getString(R.string.dialog_title_balance_account,getArguments().getString(KEY_LABEL)))
      .setView(view)
      .setNegativeButton(android.R.string.no,this)
      .setPositiveButton(android.R.string.yes,this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    //TODO
  }
}
