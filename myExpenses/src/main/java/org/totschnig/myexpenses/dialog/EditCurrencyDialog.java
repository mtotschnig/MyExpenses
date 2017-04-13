package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator;
import org.totschnig.myexpenses.util.form.FormValidator;
import org.totschnig.myexpenses.util.form.NumberRangeValidator;

import java.util.Currency;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class EditCurrencyDialog extends CommitSafeDialogFragment {

  private EditText editTextSymbol, editTextFractionDigits;

  public static EditCurrencyDialog newInstance(String currency) {
    Bundle arguments = new Bundle(1);
    arguments.putString(KEY_CURRENCY, currency);
    EditCurrencyDialog editCurrencyDialog = new EditCurrencyDialog();
    editCurrencyDialog.setArguments(arguments);
    return editCurrencyDialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    //noinspection InflateParams
    View view = li.inflate(R.layout.edit_currency, null);
    String strCurrency = getCurrency();
    Currency currency = Currency.getInstance(strCurrency);
    editTextSymbol = ((EditText) view.findViewById(R.id.edt_currency_symbol));
    editTextSymbol.setText(Money.getSymbol(currency));
    editTextFractionDigits = ((EditText) view.findViewById(R.id.edt_number_fraction_digits));
    editTextFractionDigits.setText(String.valueOf(Money.getFractionDigits(currency)));
    AlertDialog alertDialog = new AlertDialog.Builder(ctx)
        .setTitle(CurrencyEnum.valueOf(strCurrency).toString())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null)
        .create();
    alertDialog.setOnShowListener(dialog -> {

      Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(this::onOkClick);
    });
    return alertDialog;
  }

  private String getCurrency() {
    return getArguments().getString(KEY_CURRENCY);
  }

  private void onOkClick(View view) {
    FormValidator validator = new FormValidator();
    validator.add(new FormFieldNotEmptyValidator(editTextSymbol));
    validator.add(new NumberRangeValidator(editTextFractionDigits, 0, 8));
    if (validator.validate()) {
      ((ManageCurrencies) getActivity()).onFinishCurrencyEdit(getCurrency(),
          editTextSymbol.getText().toString(),
          Integer.parseInt(editTextFractionDigits.getText().toString()));
      dismiss();
    }
  }
}
