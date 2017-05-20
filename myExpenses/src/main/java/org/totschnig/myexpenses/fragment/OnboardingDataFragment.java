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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.ui.AmountEditText;

import java.math.BigDecimal;

import icepick.Icepick;
import icepick.State;


public class OnboardingDataFragment extends Fragment implements AdapterView.OnItemSelectedListener {

  private View moreOptionsContainer, moreOptionsButton;
  private Spinner currencySpinner;
  private Spinner accountTypeSpinner;
  private Spinner colorSpinner;
  @State boolean moreOptionsShown = false;

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
    //lead
    ((TextView) view.findViewById(R.id.onboarding_lead)).setText(R.string.onboarding_data_title);

    //label
    ((EditText) view.findViewById(R.id.Label)).setText(R.string.default_account_name);

    //amount
    AmountEditText amountEditText = (AmountEditText) view.findViewById(R.id.Amount);
    amountEditText.setFractionDigits(2);
    amountEditText.setAmount(BigDecimal.ZERO);
    view.findViewById(R.id.Calculator).setVisibility(View.GONE);

    //currency
    currencySpinner = DialogUtils.configureCurrencySpinner(view, this);

    //type
    accountTypeSpinner = DialogUtils.configureTypeSpinner(view);

    //color
    colorSpinner = DialogUtils.configureColorSpinner(view, Account.DEFAULT_COLOR);

    moreOptionsContainer = view.findViewById(R.id.MoreOptionsContainer);
    moreOptionsButton = view.findViewById(R.id.MoreOptionsButton);
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

  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
