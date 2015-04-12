package org.totschnig.myexpenses.dialog;

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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AmountActivity;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.filter.AmountCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Date;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class DateFilterDialog extends CommitSafeDialogFragment implements OnClickListener {
  private DatePicker mDate1;
  private DatePicker mDate2;
  private Spinner mOperatorSpinner;
  public static final DateFilterDialog newInstance() {
    DateFilterDialog f = new DateFilterDialog();
    return f;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MyExpenses ctx  = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.filter_date, null);
    mOperatorSpinner = (Spinner) view.findViewById(R.id.Operator);
    final View date2Row = view.findViewById(R.id.Date2Row);
    mOperatorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
          int position, long id) {
        String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)[position];
        date2Row.setVisibility(selectedOp.equals("BTW") ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });

    mDate1 = (DatePicker) view.findViewById(R.id.amount1);
    mDate2 = (DatePicker) view.findViewById(R.id.amount2);


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
    Date date1 = new Date();//mDate1.getDate();
    Date date2 = new Date();//mDate1.getDate();
    String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)
        [mOperatorSpinner.getSelectedItemPosition()];
    if (date1.equals("")) {
      return;
    }

    if (selectedOp.equals("BTW")) {
      if (date2.equals("")) {
        return;
      }
    }
//    ctx.addFilterCriteria(R.id.FILTER_AMOUNT_COMMAND,new AmountCriteria(
//        WhereFilter.Operation.valueOf(selectedOp),
//        currency,
//        type,
//        bdAmount1,bdAmount2));
  }
}
