package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.filter.AmountCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.ui.AmountEditText;

import java.math.BigDecimal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class AmountFilterDialog extends BaseDialogFragment implements OnClickListener {
  private AmountEditText mAmount1Text;
  private AmountEditText mAmount2Text;
  private Spinner mOperatorSpinner;

  public static AmountFilterDialog newInstance(CurrencyUnit currency) {
    Bundle bundle = new Bundle();
    bundle.putSerializable(KEY_CURRENCY, currency);
    AmountFilterDialog f = new AmountFilterDialog();
    f.setArguments(bundle);
    return f;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(R.layout.filter_amount);
    mOperatorSpinner = dialogView.findViewById(R.id.Operator);
    mAmount1Text = dialogView.findViewById(R.id.amount1);
    mAmount2Text = dialogView.findViewById(R.id.amount2);
    final View amount2Row = dialogView.findViewById(R.id.Amount2Row);
    mOperatorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view,
                                 int position, long id) {
        mAmount1Text.setContentDescription(getResources().getStringArray(R.array.comparison_operator_entries)[position]);
        amount2Row.setVisibility(getResources().getStringArray(R.array.comparison_operator_values)[position].equals("BTW") ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
      }
    });
    ((ArrayAdapter) mOperatorSpinner.getAdapter())
        .setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    int fractionDigits = ((CurrencyUnit) getArguments().getSerializable(KEY_CURRENCY)).getFractionDigits();
    mAmount1Text.setFractionDigits(fractionDigits);
    mAmount2Text.setFractionDigits(fractionDigits);

    return builder
        .setTitle(R.string.search_amount)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, null)
        .create();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (ctx == null) {
      return;
    }
    BigDecimal bdAmount1 = mAmount1Text.validate(false);
    if (bdAmount1 == null) {
      return;
    }
    BigDecimal bdAmount2 = null;
    String selectedOp = getResources().getStringArray(R.array.comparison_operator_values)
        [mOperatorSpinner.getSelectedItemPosition()];
    AlertDialog dlg = (AlertDialog) dialog;
    boolean type = ((RadioGroup) dlg.findViewById(R.id.type)).getCheckedRadioButtonId() == R.id.income;
    if (selectedOp.equals("BTW")) {
      bdAmount2 = mAmount2Text.validate(false);
      if (bdAmount2 == null) {
        return;
      }
    }

    final CurrencyUnit currency = (CurrencyUnit) getArguments().getSerializable(KEY_CURRENCY);
    ctx.addFilterCriteria(new AmountCriteria(
        WhereFilter.Operation.valueOf(selectedOp),
        currency.getCode(),
        type,
        new Money(currency, bdAmount1).getAmountMinor(),
        bdAmount2 != null ? new Money(currency, bdAmount2).getAmountMinor() : null));
  }
}
