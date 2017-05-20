package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.ui.AmountEditText;

import java.math.BigDecimal;
import java.util.Currency;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;


public class OnboardingDataFragment extends Fragment implements AdapterView.OnItemSelectedListener {

  @BindView(R.id.MoreOptionsContainer)
  View moreOptionsContainer;
  @BindView(R.id.MoreOptionsButton)
  View moreOptionsButton;
  @BindView(R.id.Label)
  EditText labelEditText;
  @BindView(R.id.Description)
  EditText descriptionEditText;
  @BindView(R.id.TaType)
  CompoundButton typeButton;
  @BindView(R.id.Amount)
  AmountEditText amountEditText;

  private Spinner currencySpinner;
  private Spinner accountTypeSpinner;
  private Spinner colorSpinner;
  @State boolean moreOptionsShown = false;
  private int defaultCurrencyPosition;

  public static OnboardingDataFragment newInstance() {
    return new OnboardingDataFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    Icepick.restoreInstanceState(this, savedInstanceState);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.onboarding_data, menu);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_data, container, false);
    ButterKnife.bind(this, view);
    //lead
    ((TextView) view.findViewById(R.id.onboarding_lead)).setText(R.string.onboarding_data_title);

    //label
    labelEditText.setText(R.string.default_account_name);

    //amount
    amountEditText.setFractionDigits(2);
    amountEditText.setAmount(BigDecimal.ZERO);
    view.findViewById(R.id.Calculator).setVisibility(View.GONE);

    //currency
    currencySpinner = DialogUtils.configureCurrencySpinner(view, this);
    defaultCurrencyPosition = currencySpinner.getSelectedItemPosition();

    //type
    accountTypeSpinner = DialogUtils.configureTypeSpinner(view);

    //color
    colorSpinner = DialogUtils.configureColorSpinner(view, Account.DEFAULT_COLOR);

    if (moreOptionsShown) {
      showMoreOptions();
    }
    return view;
  }

  public void showMoreOptions(View view) {
    moreOptionsShown = true;
    showMoreOptions();
  }

  private void showMoreOptions() {
    moreOptionsButton.setVisibility(View.GONE);
    moreOptionsContainer.setVisibility(View.VISIBLE);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    switch (parent.getId()) {
      case R.id.Currency:
        String currency = ((CurrencyEnum) currencySpinner.getSelectedItem()).name();
        try {
          Currency instance = Currency.getInstance(currency);
          amountEditText.setFractionDigits(Money.getFractionDigits(instance));
        } catch (IllegalArgumentException e) {
          Toast.makeText(getActivity(), getString(R.string.currency_not_supported, currency), Toast.LENGTH_LONG).show();
          currencySpinner.setSelection(defaultCurrencyPosition);
        }
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  public Account buildAccount() {
    String label = labelEditText.getText().toString();
    if (android.text.TextUtils.isEmpty(label)) {
      label = getString(R.string.default_account_name);
    }
    BigDecimal openingBalance = AccountEdit.validateAmoutInput(getActivity(), amountEditText, false);
    if (openingBalance == null) {
      openingBalance = BigDecimal.ZERO;
    } else if (!typeButton.isChecked()) {
      openingBalance = openingBalance.negate();
    }
    Currency instance = Currency.getInstance(((CurrencyEnum) currencySpinner.getSelectedItem()).name());
    return new Account(label, instance, new Money(instance, openingBalance),
        descriptionEditText.getText().toString(),
        (AccountType) accountTypeSpinner.getSelectedItem(), (Integer) colorSpinner.getSelectedItem());
  }
}
