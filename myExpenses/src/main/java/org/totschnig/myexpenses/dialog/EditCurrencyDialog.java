package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator;
import org.totschnig.myexpenses.util.form.FormValidator;
import org.totschnig.myexpenses.util.form.NumberRangeValidator;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class EditCurrencyDialog extends CommitSafeDialogFragment {

  @BindView(R.id.edt_currency_symbol)
  EditText editTextSymbol;

  @BindView(R.id.edt_currency_fraction_digits)
  EditText editTextFractionDigits;

  @BindView(R.id.edt_currency_code)
  EditText editTextCode;

  @BindView(R.id.edt_currency_label)
  EditText editTextLabel;

  @BindView(R.id.container_currency_label)
  ViewGroup containerLabel;

  @BindView(R.id.container_currency_code)
  ViewGroup containerCode;


  @Inject
  CurrencyContext currencyContext;

  public static EditCurrencyDialog newInstance(Currency currency) {
    Bundle arguments = new Bundle(1);
    arguments.putSerializable(KEY_CURRENCY, currency);
    EditCurrencyDialog editCurrencyDialog = new EditCurrencyDialog();
    editCurrencyDialog.setArguments(arguments);
    return editCurrencyDialog;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    //noinspection InflateParams
    View view = li.inflate(R.layout.edit_currency, null);
    ButterKnife.bind(this, view);
    Currency currency = getCurrency();
    CurrencyUnit currencyUnit = currencyContext.get(currency.code());
    editTextSymbol.setText(currencyUnit.symbol());
    editTextFractionDigits.setText(String.valueOf(currencyUnit.fractionDigits()));
    editTextCode.setText(currency.code());
    final String displayName = currency.toString();
    final boolean frameworkCurrency = isFrameworkCurrency(currency.code());
    if (frameworkCurrency) {
      editTextSymbol.requestFocus();
    } else {
      containerLabel.setVisibility(View.VISIBLE);
      containerCode.setVisibility(View.VISIBLE);
      editTextLabel.setText(displayName);
    }
    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null);
    if (frameworkCurrency) {
      builder.setTitle(String.format(Locale.ROOT, "%s (%s)", displayName, currency.code()));
    }
    AlertDialog alertDialog = builder.create();
    alertDialog.setOnShowListener(dialog -> {

      Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(this::onOkClick);
    });
    return alertDialog;
  }

  private boolean isFrameworkCurrency(String currencyCode) {
    try {
      final java.util.Currency instance = java.util.Currency.getInstance(currencyCode);
      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && instance.getNumericCode() != 0;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Currency getCurrency() {
    return (Currency) getArguments().getSerializable(KEY_CURRENCY);
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
