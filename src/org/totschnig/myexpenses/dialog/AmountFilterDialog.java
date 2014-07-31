package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

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
    Spinner operatorSpinner = (Spinner) view.findViewById(R.id.Operator);
    final View amount2Row = view.findViewById(R.id.Amount2Row);
    operatorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
          int position, long id) {
        String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)[position];
        amount2Row.setVisibility(selectedOp.equals("btw") ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });
    char decimalSeparator = Utils.getDefaultDecimalSeparator();
    EditText amount1 = (EditText) view.findViewById(R.id.amount1);
    EditText amount2 = (EditText) view.findViewById(R.id.amount2);
    Utils.configDecimalSeparator(amount1, decimalSeparator);
    Utils.configDecimalSeparator(amount2, decimalSeparator);

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
