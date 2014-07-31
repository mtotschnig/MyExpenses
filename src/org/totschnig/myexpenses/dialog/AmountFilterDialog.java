package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class AmountFilterDialog extends CommitSafeDialogFragment implements OnClickListener {
  public static final AmountFilterDialog newInstance() {
    return new AmountFilterDialog();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MyExpenses ctx  = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.filter_amount, null);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.search_amount)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    // TODO Auto-generated method stub
    
  }
}
