/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;

import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_CHECK;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_UNLINK;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_TOGGLE_EXCLUDE_FROM_TOTALS;

/**
 * Activity for editing an account
 *
 * @author Michael Totschnig
 */
public class AccountEdit extends AmountActivity implements
    OnItemSelectedListener, ContribIFace, SimpleDialog.OnDialogResultListener {

  private EditText mLabelText;
  private EditText mDescriptionText;

  private SpinnerHelper mCurrencySpinner, mAccountTypeSpinner, mSyncSpinner;
  private AppCompatButton mColorIndicator;
  private Account mAccount;
  private ArrayAdapter<CurrencyEnum> currencyAdapter;
  private ExchangeRateEdit mExchangeRateEdit;

  private void requireAccount() {
    if (mAccount == null) {
      Bundle extras = getIntent().getExtras();
      long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID) : 0;
      if (rowId != 0) {
        mAccount = Account.getInstanceFromDb(rowId);
      } else {
        mAccount = new Account();
      }
    }
  }

  @Override
  int getDiscardNewMessage() {
    return R.string.dialog_confirm_discard_new_account;
  }

  @SuppressLint("InlinedApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.one_account);
    setupToolbar();

    mLabelText = findViewById(R.id.Label);
    mDescriptionText = findViewById(R.id.Description);

    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
        : 0;
    requireAccount();
    if (mAccount == null) {
      showSnackbar("Error instantiating account " + rowId, Snackbar.LENGTH_SHORT);
      finish();
      return;
    }
    if (rowId != 0) {
      mNewInstance = false;
      setTitle(R.string.menu_edit_account);
      mLabelText.setText(mAccount.getLabel());
      mDescriptionText.setText(mAccount.description);
    } else {
      setTitle(R.string.menu_create_account);
      mAccount = new Account();
      String currency = extras != null ? extras.getString(DatabaseConstants.KEY_CURRENCY) : null;
      if (currency != null)
        try {
          mAccount.setCurrency(currency);
        } catch (IllegalArgumentException e) {
          //if not supported ignore
        }
    }
    mAmountText.setFractionDigits(Money.getFractionDigits(mAccount.currency));

    mCurrencySpinner = new SpinnerHelper(findViewById(R.id.Currency));
    currencyAdapter = new CurrencyAdapter(this);
    mCurrencySpinner.setAdapter(currencyAdapter);

    mAccountTypeSpinner = new SpinnerHelper(DialogUtils.configureTypeSpinner(findViewById(R.id.AccountType)));

    mColorIndicator = findViewById(R.id.ColorIndicator);
    mExchangeRateEdit = findViewById(R.id.ExchangeRate);

    mSyncSpinner = new SpinnerHelper(findViewById(R.id.Sync));
    configureSyncBackendAdapter();
    linkInputsWithLabels();
    populateFields();
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PREFERENCES_REQUEST) {
      configureSyncBackendAdapter();
    }
  }

  private void configureSyncBackendAdapter() {
    ArrayAdapter syncBackendAdapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            Stream.concat(
                Stream.of(getString(R.string.synchronization_none)),
                GenericAccountService.getAccountsAsStream(this).map(account -> account.name))
                .collect(Collectors.toList()));
    syncBackendAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mSyncSpinner.setAdapter(syncBackendAdapter);
    if (mAccount.getSyncAccountName() != null) {
      int position = syncBackendAdapter.getPosition(mAccount.getSyncAccountName());
      if (position > -1) {
        mSyncSpinner.setSelection(position);
        mSyncSpinner.setEnabled(false);
        findViewById(R.id.SyncUnlink).setVisibility(View.VISIBLE);
      }
    } else {
      findViewById(R.id.SyncHelp).setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupListeners();
  }

  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {

    BigDecimal amount = mAccount.openingBalance.getAmountMajor();
    if (amount.signum() == -1) {
      amount = amount.abs();
    } else {
      mType = INCOME;
      configureType();
    }
    mAmountText.setAmount(amount);
    String currencyCode = mAccount.currency.getCurrencyCode();
    mCurrencySpinner.setSelection(currencyAdapter.getPosition(
        CurrencyEnum.valueOf(currencyCode)));
    mAccountTypeSpinner.setSelection(mAccount.getType().ordinal());
    UiUtils.setBackgroundOnButton(mColorIndicator, mAccount.color);
    String homeCurrencyPref = PrefKey.HOME_CURRENCY.getString(currencyCode);
    if (!homeCurrencyPref.equals(currencyCode)) {
      exchangeRateRow.setVisibility(View.VISIBLE);
      mExchangeRateEdit.setSymbols(Money.getSymbol(mAccount.currency),
          Money.getSymbol(Utils.getSaveInstance(homeCurrencyPref)));
      mExchangeRateEdit.setRate(mAccount.getExchangeRate());
    }
  }

  /**
   * validates currency (must be code from ISO 4217) and opening balance
   * (a valid float according to the format from the locale)
   *
   * @return true upon success, false if validation fails
   */
  protected void saveState() {
    BigDecimal openingBalance = validateAmountInput(true);
    if (openingBalance == null)
      return;
    String label;
    String currency = ((CurrencyEnum) mCurrencySpinner.getSelectedItem()).name();
    try {
      mAccount.setCurrency(currency);
    } catch (IllegalArgumentException e) {
      showSnackbar(getString(R.string.currency_not_supported, currency), Snackbar.LENGTH_LONG);
      return;
    }

    label = mLabelText.getText().toString();
    if (label.equals("")) {
      mLabelText.setError(getString(R.string.no_title_given));
      return;
    }
    mAccount.setLabel(label);
    mAccount.description = mDescriptionText.getText().toString();
    if (mType == EXPENSE) {
      openingBalance = openingBalance.negate();
    }
    mAccount.openingBalance.setAmountMajor(openingBalance);
    mAccount.setType((AccountType) mAccountTypeSpinner.getSelectedItem());
    if (mSyncSpinner.getSelectedItemPosition() > 0) {
      mAccount.setSyncAccountName((String) mSyncSpinner.getSelectedItem());
    }
    mAccount.setExchangeRate(mExchangeRateEdit.getRate(false));
    //EditActivity.saveState calls DbWriteFragment
    super.saveState();
  }

  @Override
  public Model getObject() {
    return mAccount;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
                             long id) {
    setDirty(true);
    switch (parent.getId()) {
      case R.id.Currency:
        try {
          String currency = ((CurrencyEnum) mCurrencySpinner.getSelectedItem()).name();
          mAmountText.setFractionDigits(Money.getFractionDigits(
              Currency.getInstance(currency)));
        } catch (IllegalArgumentException e) {
          //will be reported to user when he tries so safe
        }
        break;
      case R.id.Sync:
        contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null);
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // TODO Auto-generated method stub
  }

  /*
   * callback of DbWriteFragment
   */
  @Override
  public void onPostExecute(Object result) {
    if (result == null) {
      showSnackbar("Unknown error while saving account", Snackbar.LENGTH_SHORT);
    } else {
      Intent intent = new Intent();
      long id = ContentUris.parseId((Uri) result);
      mAccount.requestSync();
      intent.putExtra(DatabaseConstants.KEY_ROWID, id);
      setResult(RESULT_OK, intent);
    }
    finish();
    //no need to call super after finish
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result r = ((Result) o);
    switch (taskId) {
      case TASK_SYNC_UNLINK:
        if (r.success) {
          mSyncSpinner.setSelection(0);
          mSyncSpinner.setEnabled(true);
          findViewById(R.id.SyncUnlink).setVisibility(View.GONE);
        }
        break;
      case TASK_SYNC_CHECK:
        if (!r.success) {
          mSyncSpinner.setSelection(0);
          showHelp(r.print(this));
        }
        break;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(Menu.NONE, R.id.EXCLUDE_FROM_TOTALS_COMMAND, 0, R.string.menu_exclude_from_totals)
        .setCheckable(true)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    requireAccount();
    if (mAccount == null) {
      AcraHelper.report(new NullPointerException("mAccount is null"));
    } else {
      MenuItem item = menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND);
      if (item == null) {
        AcraHelper.report(new NullPointerException("EXCLUDE_FROM_TOTALS_COMMAND menu item not found"));
      } else {
        item.setChecked(
            mAccount.excludeFromTotals);
      }
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.EXCLUDE_FROM_TOTALS_COMMAND:
        mAccount.excludeFromTotals = !mAccount.excludeFromTotals;
        if (mAccount.getId() != 0) {
          startTaskExecution(
              TASK_TOGGLE_EXCLUDE_FROM_TOTALS,
              new Long[]{mAccount.getId()},
              mAccount.excludeFromTotals, 0);
          supportInvalidateOptionsMenu();
        }
        return true;
      case R.id.SYNC_UNLINK_COMMAND:
        startTaskExecution(
            TASK_SYNC_UNLINK,
            new String[]{mAccount.uuid}, null, 0);
        return true;
      case R.id.SETTINGS_COMMAND:
        Intent i = new Intent(this, ManageSyncBackends.class);
        startActivityForResult(i, PREFERENCES_REQUEST);
        return true;
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  protected void setupListeners() {
    super.setupListeners();
    mLabelText.addTextChangedListener(this);
    mDescriptionText.addTextChangedListener(this);
    mAccountTypeSpinner.setOnItemSelectedListener(this);
    mCurrencySpinner.setOnItemSelectedListener(this);
    mSyncSpinner.setOnItemSelectedListener(this);
  }

  @Override
  protected void linkInputsWithLabels() {
    super.linkInputsWithLabels();
    linkInputWithLabel(mLabelText, findViewById(R.id.LabelLabel));
    linkInputWithLabel(mDescriptionText, findViewById(R.id.DescriptionLabel));
    linkInputWithLabel(mColorIndicator, findViewById(R.id.ColorLabel));
    linkInputWithLabel(mAccountTypeSpinner.getSpinner(), findViewById(R.id.AccountTypeLabel));
    linkInputWithLabel(mCurrencySpinner.getSpinner(), findViewById(R.id.CurrencyLabel));
    linkInputWithLabel(mSyncSpinner.getSpinner(), findViewById(R.id.SyncLabel));
  }

  public void syncUnlink(View view) {
    DialogUtils.showSyncUnlinkConfirmationDialog(this, mAccount);
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (!mNewInstance) {
      startTaskExecution(
          TASK_SYNC_CHECK,
          new String[]{mAccount.uuid},
          (String) mSyncSpinner.getSelectedItem(),
          R.string.progress_dialog_checking_sync_backend);
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
    if (feature == ContribFeature.SYNCHRONIZATION) {
      mSyncSpinner.setSelection(0);
    }
  }

  public void syncHelp(View view) {
    showHelp(getString(R.string.form_synchronization_help_text_add));
  }

  private void showHelp(String message) {
    MessageDialogFragment.newInstance(
        0,
        message,
        new MessageDialogFragment.Button(R.string.pref_category_title_manage, R.id.SETTINGS_COMMAND, null),
        MessageDialogFragment.Button.okButton(),
        null)
        .show(getSupportFragmentManager(), "SYNC_HELP");
  }

  public void editAccountColor(View view) {
    SimpleColorDialog.build()
        .allowCustom(true)
        .cancelable(false)
        .neut()
        .colorPreset(mAccount.color)
        .show(this, ACCOUNT_COLOR_DIALOG);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (ACCOUNT_COLOR_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      mAccount.color = extras.getInt(SimpleColorDialog.COLOR);
      UiUtils.setBackgroundOnButton(mColorIndicator, mAccount.color);
      return true;
    }
    return false;
  }

  @Override
  @IdRes
  protected int getSnackbarContainerId() {
    return R.id.OneAccount;
  }
}
