package org.totschnig.myexpenses.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.setupwizardlib.SetupWizardLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.SplashActivity;
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.util.UiUtils;

import java.math.BigDecimal;
import java.util.Currency;

import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.ACCOUNT_COLOR_DIALOG;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESTORE_REQUEST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;


public class OnboardingDataFragment extends OnboardingFragment implements AdapterView.OnItemSelectedListener,
    SimpleDialog.OnDialogResultListener {

  private static final String KEY_LABEL_UNCHANGED_OR_EMPTY = "label_unchanged_or_empty";
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
  @BindView(R.id.ColorIndicator)
  AppCompatButton colorIndicator;
  @BindView(R.id.setup_wizard_layout)
  SetupWizardLayout setupWizardLayout;

  private Spinner currencySpinner;
  private Spinner accountTypeSpinner;
  @State
  boolean moreOptionsShown = false;
  @State
  int accountColor = Account.DEFAULT_COLOR;
  private int lastSelectedCurrencyPosition;

  public static OnboardingDataFragment newInstance() {
    return new OnboardingDataFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Icepick.restoreInstanceState(this, savedInstanceState);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_CURRENCY, ((CurrencyEnum) currencySpinner.getSelectedItem()));
    String label = labelEditText.getText().toString();
    outState.putBoolean(KEY_LABEL_UNCHANGED_OR_EMPTY, TextUtils.isEmpty(label) ||
        label.equals(getString(R.string.default_account_name)));
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  protected void onNextButtonClicked() {
    ((SplashActivity) getActivity()).finishOnboarding();
  }

  @Override
  protected void createMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.onboarding_data);
    Menu menu = toolbar.getMenu();
    SubMenu subMenu = menu.findItem(R.id.SetupFromRemote).getSubMenu();
    subMenu.clear();
    ((SyncBackendSetupActivity) getActivity()).addSyncProviderMenuEntries(subMenu);
    GenericAccountService.getAccountsAsStream(getActivity()).forEach(
        account -> subMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, account.name));
    toolbar.setOnMenuItemClickListener(this::onRestoreMenuItemSelected);
  }

  private boolean onRestoreMenuItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.SetupFromLocal) {
      getActivity().startActivityForResult(new Intent("myexpenses.intent.restore"), RESTORE_REQUEST);
    } else {
      SyncBackendSetupActivity hostActivity = (SyncBackendSetupActivity) getActivity();
      if (item.getItemId() == Menu.NONE) {
        hostActivity.fetchAccountData(item.getTitle().toString());
      } else {
        hostActivity.startSetup(item.getItemId());
      }
    }
    return true;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.onboarding_wizzard_2, container, false);
    ButterKnife.bind(this, view);

    //label
    setDefaultLabel();

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
    UiUtils.setBackgroundOnButton(colorIndicator, accountColor);

    if (moreOptionsShown) {
      showMoreOptions();
    }

    //lead
    setupWizardLayout.setHeaderText(R.string.onboarding_data_title);
    setupWizardLayout.setIllustration(R.drawable.bg_setup_header, R.drawable.bg_header_horizontal_tile);

    configureNavigation(view, inflater, R.id.suw_navbar_done);

    return view;
  }

  public void setDefaultLabel() {
    labelEditText.setText(R.string.default_account_name);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      currencySpinner.setSelection(((CurrencyAdapter) currencySpinner.getAdapter())
          .getPosition((CurrencyEnum) savedInstanceState.get(KEY_CURRENCY)));
      if(savedInstanceState.getBoolean(KEY_LABEL_UNCHANGED_OR_EMPTY)) {
        setDefaultLabel();
      }
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
        (AccountType) accountTypeSpinner.getSelectedItem(), accountColor);
  }

  public void editAccountColor() {
    SimpleColorDialog.build()
        .allowCustom(true)
        .cancelable(false)
        .neut()
        .colorPreset(accountColor)
        .show(this, ACCOUNT_COLOR_DIALOG);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (ACCOUNT_COLOR_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      accountColor = extras.getInt(SimpleColorDialog.COLOR);
      UiUtils.setBackgroundOnButton(colorIndicator, accountColor);
      return true;
    }
    return false;
  }
}
