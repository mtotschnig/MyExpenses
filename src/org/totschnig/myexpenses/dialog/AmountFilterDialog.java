package org.totschnig.myexpenses.dialog;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Money;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import org.totschnig.myexpenses.provider.filter.AmountCriteria;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
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
  private EditText mAmount1Text;
  private EditText mAmount2Text;
  private DecimalFormat nfDLocal;
  private Spinner mOperatorSpinner;
  public static final AmountFilterDialog newInstance(Currency currency) {
    Bundle bundle = new Bundle();
    bundle.putSerializable(KEY_CURRENCY, currency);
    AmountFilterDialog f = new AmountFilterDialog();
    f.setArguments(bundle);
    return f;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MyExpenses ctx  = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.filter_amount, null);
    mOperatorSpinner = (Spinner) view.findViewById(R.id.Operator);
    final View amount2Row = view.findViewById(R.id.Amount2Row);
    mOperatorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
          int position, long id) {
        String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)[position];
        amount2Row.setVisibility(selectedOp.equals("BTW") ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });
    char decimalSeparator = Utils.getDefaultDecimalSeparator();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    nfDLocal = new DecimalFormat("#0.###",symbols);
    nfDLocal.setGroupingUsed(false);
    mAmount1Text = (EditText) view.findViewById(R.id.amount1);
    mAmount2Text = (EditText) view.findViewById(R.id.amount2);
    Utils.configDecimalSeparator(mAmount1Text, decimalSeparator);
    Utils.configDecimalSeparator(mAmount2Text, decimalSeparator);

    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.search_amount)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (ctx==null) {
      return;
    }
    String strAmount1 = mAmount1Text.getText().toString();
    String strAmount2 = mAmount2Text.getText().toString();
    Long longAmount1,longAmount2;
    Criteria c;
    String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)
        [mOperatorSpinner.getSelectedItemPosition()];
    if (strAmount1.equals("")) {
      return;
    }
    longAmount1 = new Money(
        (Currency)getArguments().getSerializable(KEY_CURRENCY),
        Utils.validateNumber(nfDLocal, strAmount1))
    .getAmountMinor();
    if (selectedOp.equals("BTW")) {
      if (strAmount2.equals("")) {
        return;
      }
      longAmount2 = new Money(
          (Currency)getArguments().getSerializable(KEY_CURRENCY),
          Utils.validateNumber(nfDLocal, strAmount2))
      .getAmountMinor();
      c = new AmountCriteria(
          WhereFilter.Operation.BTW,
          String.valueOf(longAmount1),
          String.valueOf(longAmount2));
    } else {
      longAmount2 = null;
      c = new AmountCriteria(
          WhereFilter.Operation.valueOf(selectedOp),
          String.valueOf(longAmount1));
    }
    ctx.addFilterCriteria(c);
  }
}
