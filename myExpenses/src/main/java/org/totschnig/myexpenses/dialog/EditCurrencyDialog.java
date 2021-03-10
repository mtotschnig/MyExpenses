package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.databinding.EditCurrencyBinding;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator;
import org.totschnig.myexpenses.util.form.FormValidator;
import org.totschnig.myexpenses.util.form.NumberRangeValidator;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.util.Utils.isFrameworkCurrency;

public class EditCurrencyDialog extends BaseDialogFragment {
  private EditCurrencyBinding binding;

  public static final String KEY_RESULT = "result";

  @Inject
  CurrencyContext currencyContext;

  private EditCurrencyViewModel editCurrencyViewModel;

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
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
    editCurrencyViewModel = new ViewModelProvider(this).get(EditCurrencyViewModel.class);
    editCurrencyViewModel.getUpdateComplete().observe(this, this::dismiss);
    editCurrencyViewModel.getInsertComplete().observe(this, success -> {
      if (success != null && success) {
        dismiss();
      } else {
        showSnackbar(R.string.currency_code_already_definded);
        setButtonState(true);
      }
    });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithBinding(() -> {
        binding = EditCurrencyBinding.inflate(materialLayoutInflater);
        return binding;
    });
    Currency currency = getCurrency();
    boolean frameworkCurrency;
    String title = null;
    if (currency != null) {
      CurrencyUnit currencyUnit = currencyContext.get(currency.getCode());
      binding.edtCurrencySymbol.setText(currencyUnit.getSymbol());
      binding.edtCurrencyCode.setText(currency.getCode());

      final String displayName = currency.toString();
      frameworkCurrency = isFrameworkCurrency(currency.getCode());
      if (frameworkCurrency) {
        binding.edtCurrencySymbol.requestFocus();
        title = String.format(Locale.ROOT, "%s (%s)", displayName, currency.getCode());
        binding.containerCurrencyLabel.setVisibility(View.GONE);
        binding.containerCurrencyCode.setVisibility(View.GONE);
      } else {
        binding.edtCurrencyLabel.setText(displayName);
      }
      binding.edtCurrencyFractionDigits.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
          final int newValue = readFractionDigitsFromUI();
          final int oldValue = currentFractionDigits();
          final boolean valueUpdate = newValue != -1 && newValue != oldValue;
          binding.checkBox.setVisibility(valueUpdate ? View.VISIBLE : View.GONE);
          binding.warningChangeFractionDigits.setVisibility(valueUpdate ? View.VISIBLE : View.GONE);
          if (valueUpdate) {
            String message = getString(R.string.warning_change_fraction_digits_1);
            int delta = oldValue - newValue;
            message += " " + getString(
                delta > 0 ? R.string.warning_change_fraction_digits_2_multiplied :
                    R.string.warning_change_fraction_digits_2_divided,
                Utils.pow(10, Math.abs(delta)));
            if (delta > 0) {
              message += " " + getString(R.string.warning_change_fraction_digits_3);
            }
            binding.warningChangeFractionDigits.setText(message);
          }
        }
      });
    } else {
      title = getString(R.string.dialog_title_new_currency);
      binding.edtCurrencyCode.setFocusable(true);
      binding.edtCurrencyCode.setFocusableInTouchMode(true);
      binding.edtCurrencyCode.setEnabled(true);
      binding.edtCurrencyCode.setFilters(new InputFilter[] {new InputFilter.AllCaps(), new InputFilter.LengthFilter(3)});
    }
    binding.edtCurrencyFractionDigits.setText(String.valueOf(currentFractionDigits()));
    final AlertDialog alertDialog = builder
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null)
        .setTitle(title)
        .create();
    alertDialog.setOnShowListener(dialog -> {
      Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(this::onOkClick);
    });
    return alertDialog;
  }

  private String readSymbolFromUI() {
    return binding.edtCurrencySymbol.getText().toString();
  }

  private String readLabelFromUI() {
    return binding.edtCurrencyLabel.getText().toString();
  }

  private String readCodeFromUI() {
    return binding.edtCurrencyCode.getText().toString();
  }

  private int currentFractionDigits() {
    final Currency currency = getCurrency();
    if (currency != null) {
      return currencyContext.get(currency.getCode()).getFractionDigits();
    }
    return 2;
  }

  private int readFractionDigitsFromUI() {
    try {
      return Integer.parseInt(binding.edtCurrencyFractionDigits.getText().toString());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Nullable
  private Currency getCurrency() {
    return (Currency) getArguments().getSerializable(KEY_CURRENCY);
  }

  private void onOkClick(View view) {
    final Currency currency = getCurrency();
    FormValidator validator = new FormValidator();
    validator.add(new FormFieldNotEmptyValidator(binding.edtCurrencySymbol));
    validator.add(new NumberRangeValidator(binding.edtCurrencyFractionDigits, 0, 8));
    if (currency == null) {
      validator.add(new FormFieldNotEmptyValidator(binding.edtCurrencyCode));
      validator.add(new FormFieldNotEmptyValidator(binding.edtCurrencyLabel));
    }
    if (validator.validate()) {
      final boolean withUpdate = binding.checkBox.isChecked();
      String label = readLabelFromUI();
      final String symbol = readSymbolFromUI();
      final int fractionDigits = readFractionDigitsFromUI();
      if (currency == null) {
        editCurrencyViewModel.newCurrency(readCodeFromUI(), symbol, fractionDigits, label);
        setButtonState(false);
      } else {
        final boolean frameworkCurrency = isFrameworkCurrency(currency.getCode());
        editCurrencyViewModel.save(currency.getCode(), symbol, fractionDigits, frameworkCurrency ? null : label, withUpdate);
        if (!withUpdate && frameworkCurrency) {
          dismiss();
        } else {
          setButtonState(false);
        }
      }
    }
  }

  private void setButtonState(boolean enabled) {
    ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
  }

  public void dismiss(Integer result) {
    Intent data = null;
    final Fragment targetFragment = getTargetFragment();
    if (targetFragment != null) {
      if (result != null) {
        data = new Intent();
        data.putExtra(KEY_RESULT, result.intValue());
        data.putExtra(KEY_CURRENCY, getCurrency().getCode());
      }
      targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
    }
    super.dismiss();
  }
}
