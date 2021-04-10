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
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.databinding.OneAccountBinding;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.io.Serializable;
import java.math.BigDecimal;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;

import static org.totschnig.myexpenses.activity.ConstantsKt.PREFERENCES_REQUEST;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SET_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_CHECK;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_UNLINK;

/**
 * Activity for editing an account
 *
 * @author Michael Totschnig
 */
public class AccountEdit extends AmountActivity implements ExchangeRateEdit.Host,
    OnItemSelectedListener, ContribIFace, SimpleDialog.OnDialogResultListener {

  private OneAccountBinding binding;

  private SpinnerHelper mCurrencySpinner, mAccountTypeSpinner, mSyncSpinner;
  private Account mAccount;
  private CurrencyAdapter currencyAdapter;
  private CurrencyViewModel currencyViewModel;

  private void requireAccount() {
    if (mAccount == null) {
      Bundle extras = getIntent().getExtras();
      long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID) : 0;
      if (rowId != 0) {
        mAccount = Account.getInstanceFromDb(rowId);
      } else {
        mAccount = new Account();
        String currency = extras != null ? extras.getString(DatabaseConstants.KEY_CURRENCY) : null;
        mAccount.setCurrency(currencyContext.get(currency != null ? currency : currencyViewModel.getDefault().getCode()));
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
    binding = OneAccountBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setupToolbar();
    currencyViewModel = new ViewModelProvider(this).get(CurrencyViewModel.class);

    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID) : 0;
    requireAccount();
    if (mAccount == null) {
      Toast.makeText(this, "Error instantiating account " + rowId, Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    if (rowId != 0) {
      mNewInstance = false;
      setTitle(R.string.menu_edit_account);
      binding.Label.setText(mAccount.getLabel());
      binding.Description.setText(mAccount.description);
    } else {
      setTitle(R.string.menu_create_account);
    }
    configureForCurrrency(mAccount.getCurrencyUnit());

    mCurrencySpinner = new SpinnerHelper(findViewById(R.id.Currency));
    currencyAdapter = new CurrencyAdapter(this, android.R.layout.simple_spinner_item);
    mCurrencySpinner.setAdapter(currencyAdapter);

    final Spinner spinner = findViewById(R.id.AccountType);
    DialogUtils.configureTypeSpinner(spinner);
    mAccountTypeSpinner = new SpinnerHelper(spinner);

    mSyncSpinner = new SpinnerHelper(findViewById(R.id.Sync));
    configureSyncBackendAdapter();
    populateFields();

    currencyViewModel.getCurrencies().observe(this, currencies -> {
      currencyAdapter.addAll(currencies);
      if (savedInstanceState == null) {
        mCurrencySpinner.setSelection(currencyAdapter.getPosition(Currency.Companion.create(mAccount.getCurrencyUnit().getCode(), this)));
      }
    });
    linkInputsWithLabels();
  }

  @Override
  public void afterTextChanged(Editable s) {
    super.afterTextChanged(s);
    updateCriterionLabel();
  }

  private void updateCriterionLabel() {
    int criterionLabel;
    switch (binding.Criterion.getTypedValue().compareTo(BigDecimal.ZERO)) {
      case 1:
        criterionLabel = R.string.saving_goal;
        break;
      case -1:
        criterionLabel = R.string.credit_limit;
        break;
      default:
        criterionLabel = R.string.goal_or_limit;
    }
    binding.CriterionLabel.setText(criterionLabel);
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PREFERENCES_REQUEST) {
      configureSyncBackendAdapter();
    }
  }

  private void configureSyncBackendAdapter() {
    ArrayAdapter<String> syncBackendAdapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            ArrayUtils.insert(0, GenericAccountService.getAccountNames(this), getString(R.string.synchronization_none)));
    syncBackendAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mSyncSpinner.setAdapter(syncBackendAdapter);
    if (mAccount.getSyncAccountName() != null) {
      int position = syncBackendAdapter.getPosition(mAccount.getSyncAccountName());
      if (position > -1) {
        mSyncSpinner.setSelection(position);
        mSyncSpinner.setEnabled(false);
        binding.SyncUnlink.setVisibility(View.VISIBLE);
      }
    } else {
      binding.SyncHelp.setVisibility(View.VISIBLE);
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
    binding.Amount.setAmount(mAccount.openingBalance.getAmountMajor());
    mAccountTypeSpinner.setSelection(mAccount.getType().ordinal());
    UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, mAccount.color);
    final Money criterion = mAccount.getCriterion();
    if (criterion != null) {
      binding.Criterion.setAmount(criterion.getAmountMajor());
      updateCriterionLabel();
    }
  }

  private void setExchangeRateVisibility(CurrencyUnit currencyUnit) {
    String homeCurrencyPref = prefHandler.getString(PrefKey.HOME_CURRENCY, currencyUnit.getCode());
    final boolean isHomeAccount = currencyUnit.getCode().equals(homeCurrencyPref);
    binding.ERR.getRoot().setVisibility(isHomeAccount ? View.GONE : View.VISIBLE);
    if (!isHomeAccount) {
      binding.ERR.ExchangeRate.setCurrencies(currencyUnit, currencyContext.get(homeCurrencyPref));
      binding.ERR.ExchangeRate.setRate(new BigDecimal(mAccount.getCurrencyUnit().equals(currencyUnit) ?
          mAccount.getExchangeRate() : 1), true);
    }
  }

  /**
   * validates currency (must be code from ISO 4217) and opening balance
   * (a valid float according to the format from the locale)
   */
  protected void saveState() {
    BigDecimal openingBalance = validateAmountInput(true);
    if (openingBalance == null)
      return;
    String label;
    String currency = ((Currency) mCurrencySpinner.getSelectedItem()).getCode();
    mAccount.setCurrency(currencyContext.get(currency));

    label = binding.Label.getText().toString();
    if (label.equals("")) {
      binding.Label.setError(getString(R.string.no_title_given));
      return;
    }
    mAccount.setLabel(label);
    mAccount.description = binding.Description.getText().toString();
    mAccount.openingBalance = new Money(mAccount.getCurrencyUnit(), openingBalance);
    mAccount.setType((AccountType) mAccountTypeSpinner.getSelectedItem());
    if (mSyncSpinner.getSelectedItemPosition() > 0) {
      mAccount.setSyncAccountName((String) mSyncSpinner.getSelectedItem());
    }
    if (!prefHandler.getString(PrefKey.HOME_CURRENCY, currency).equals(currency)) {
      final BigDecimal rate = binding.ERR.ExchangeRate.getRate(false);
      if (rate != null) {
        mAccount.setExchangeRate(rate.doubleValue());
      }
    }
    mAccount.setCriterion(binding.Criterion.getTypedValue());
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
    setDirty();
    int parentId = parent.getId();
    if (parentId == R.id.Currency) {
      try {
        String currency = ((Currency) mCurrencySpinner.getSelectedItem()).getCode();
        configureForCurrrency(currencyContext.get(currency));
      } catch (IllegalArgumentException e) {
        //will be reported to user when he tries so safe
      }
    } else if (parentId == R.id.Sync) {
      contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null);
    }
  }

  private void configureForCurrrency(CurrencyUnit currencyUnit) {
    binding.Amount.setFractionDigits(currencyUnit.getFractionDigits());
    binding.Criterion.setFractionDigits(currencyUnit.getFractionDigits());
    setExchangeRateVisibility(currencyUnit);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // TODO Auto-generated method stub
  }

  /*
   * callback of DbWriteFragment
   */
  @Override
  public void onPostExecute(Uri result) {
    if (result == null) {
      complain();
      super.onPostExecute(result);
    } else {
      Intent intent = new Intent();
      long id = ContentUris.parseId(result);
      mAccount.requestSync();
      intent.putExtra(DatabaseConstants.KEY_ROWID, id);
      setResult(RESULT_OK, intent);
      currencyContext.ensureFractionDigitsAreCached(mAccount.getCurrencyUnit());
      finish();
    }
    //no need to call super after finish
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result r = ((Result) o);
    switch (taskId) {
      case TASK_SYNC_UNLINK:
        if (r.isSuccess()) {
          mSyncSpinner.setSelection(0);
          mSyncSpinner.setEnabled(true);
          binding.SyncUnlink.setVisibility(View.GONE);
        }
        break;
      case TASK_SYNC_CHECK:
        if (!r.isSuccess()) {
          mSyncSpinner.setSelection(0);
          showHelp(r.print(this));
        }
        break;
      case TASK_SET_EXCLUDE_FROM_TOTALS:
        if (r.isSuccess()) {
          mAccount.excludeFromTotals = !mAccount.excludeFromTotals;
          supportInvalidateOptionsMenu();
        } else {
          complain();
        }
        break;
    }
  }

  private void complain() {
    showSnackbar("Unknown error while saving account", Snackbar.LENGTH_SHORT);
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
      CrashHandler.report(new NullPointerException("mAccount is null"));
    } else {
      MenuItem item = menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND);
      if (item == null) {
        CrashHandler.report(new NullPointerException("EXCLUDE_FROM_TOTALS_COMMAND menu item not found"));
      } else {
        item.setChecked(
            mAccount.excludeFromTotals);
      }
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.EXCLUDE_FROM_TOTALS_COMMAND) {
      if (mAccount.getId() != 0) {
        startTaskExecution(
            TASK_SET_EXCLUDE_FROM_TOTALS,
            new Long[]{mAccount.getId()},
            !mAccount.excludeFromTotals, 0);
      }
      return true;
    } else if (command == R.id.SYNC_UNLINK_COMMAND) {
      mAccount.setSyncAccountName(null);
      startTaskExecution(
          TASK_SYNC_UNLINK,
          new String[]{mAccount.getUuid()}, null, 0);
      return true;
    } else if (command == R.id.SYNC_SETTINGS_COMMAND) {
      Intent i = new Intent(this, ManageSyncBackends.class);
      startActivityForResult(i, PREFERENCES_REQUEST);
      return true;
    }
    return false;
  }

  @Override
  protected void setupListeners() {
    super.setupListeners();
    binding.Label.addTextChangedListener(this);
    binding.Description.addTextChangedListener(this);
    mAccountTypeSpinner.setOnItemSelectedListener(this);
    mCurrencySpinner.setOnItemSelectedListener(this);
    mSyncSpinner.setOnItemSelectedListener(this);
    binding.Criterion.setTypeChangedListener(type -> {
      setDirty();
      updateCriterionLabel();
    });
    binding.Criterion.addTextChangedListener(this);
  }

  public void syncUnlink(View view) {
    DialogUtils.showSyncUnlinkConfirmationDialog(this, mAccount);
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (!mNewInstance) {
      startTaskExecution(
          TASK_SYNC_CHECK,
          new String[]{mAccount.getUuid()},
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
        null,
        message,
        new MessageDialogFragment.Button(R.string.pref_category_title_manage, R.id.SYNC_SETTINGS_COMMAND, null),
        MessageDialogFragment.okButton(),
        null)
        .show(getSupportFragmentManager(), "SYNC_HELP");
  }

  public void editAccountColor(View view) {
    SimpleColorDialog.build()
        .allowCustom(true)
        .cancelable(false)
        .neut()
        .colorPreset(mAccount.color)
        .show(this, EDIT_COLOR_DIALOG);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (EDIT_COLOR_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      mAccount.color = extras.getInt(SimpleColorDialog.COLOR);
      UiUtils.setBackgroundOnButton(binding.colorInput.ColorIndicator, mAccount.color);
      return true;
    }
    return false;
  }

  @NonNull
  @Override
  public LocalDate getDate() {
    return LocalDate.now();
  }

  @NotNull
  @Override
  public TextView getAmountLabel() {
    return binding.AmountLabel;
  }

  @NotNull
  @Override
  public ViewGroup getAmountRow() {
    return binding.AmountRow;
  }

  @NotNull
  @Override
  public ViewGroup getExchangeRateRow() {
    return binding.ERR.getRoot();
  }

  @NotNull
  @Override
  public AmountInput getAmountInput() {
    return binding.Amount;
  }

  @NotNull
  @Override
  public ExchangeRateEdit getExchangeRateEdit() {
    return binding.ERR.ExchangeRate;
  }
}
