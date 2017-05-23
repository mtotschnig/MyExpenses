package org.totschnig.myexpenses.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.adapter.ColorAdapter;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.util.UiUtils;

import java.math.BigDecimal;
import java.util.Currency;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESTORE_REQUEST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;


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
  private int lastSelectedCurrencyPosition;

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
    outState.putSerializable(KEY_CURRENCY, ((CurrencyEnum) currencySpinner.getSelectedItem()));
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.onboarding_data, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.SetupFromLocal:
        getActivity().startActivityForResult(new Intent("myexpenses.intent.restore"), RESTORE_REQUEST);
        return true;
    }
    return false;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_data, container, false);
    ButterKnife.bind(this, view);
    //lead
    ((TextView) view.findViewById(R.id.onboarding_lead)).setText(R.string.onboarding_data_title);

    //label
    labelEditText.setText(R.string.default_account_name);

    //amount
    amountEditText.setFractionDigits(2);
    amountEditText.setAmount(BigDecimal.ZERO);
    typeButton.setChecked(true);
    view.findViewById(R.id.Calculator).setVisibility(View.GONE);

    //currency
    currencySpinner = DialogUtils.configureCurrencySpinner(view, this);

    //type
    accountTypeSpinner = DialogUtils.configureTypeSpinner(view);

    //color
    int selected = Account.DEFAULT_COLOR;
    colorSpinner = DialogUtils.configureColorSpinner(view, selected);
    colorSpinner.setSelection(((ColorAdapter) colorSpinner.getAdapter()).getPosition(selected));

    if (moreOptionsShown) {
      showMoreOptions();
    }
    return view;
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      currencySpinner.setSelection(((CurrencyAdapter) currencySpinner.getAdapter())
          .getPosition((CurrencyEnum) savedInstanceState.get(KEY_CURRENCY)));
    }
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
          lastSelectedCurrencyPosition = position;
        } catch (IllegalArgumentException e) {
          Snackbar snackbar = Snackbar.make(
              parent, getString(R.string.currency_not_supported, currency), Snackbar.LENGTH_LONG);
          UiUtils.configureSnackbarForDarkTheme(snackbar);
          snackbar.show();
          currencySpinner.setSelection(lastSelectedCurrencyPosition);
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
