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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.SyncBackendProviderArrayAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SpinnerHelper;
import org.totschnig.myexpenses.util.AcraHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class AccountEdit extends AmountActivity implements
    OnItemSelectedListener  {
  private static final String OPENINTENTS_COLOR_EXTRA = "org.openintents.extra.COLOR";
  private static final String OPENINTENTS_PICK_COLOR_ACTION = "org.openintents.action.PICK_COLOR";
  private EditText mLabelText;
  private EditText mDescriptionText;
  private SpinnerHelper mCurrencySpinner, mAccountTypeSpinner, mColorSpinner, mSyncSpinner;
  private Account mAccount;
  private ArrayList<Integer> mColors;
  private ArrayAdapter<Integer> mColAdapter;
  private ArrayAdapter<CurrencyEnum> currencyAdapter;

  private void requireAccount() {
    if (mAccount==null) {
      Bundle extras = getIntent().getExtras();
      long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
          : 0;
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

    mLabelText = (EditText) findViewById(R.id.Label);
    mDescriptionText = (EditText) findViewById(R.id.Description);

    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
          : 0;
    requireAccount();
    if (mAccount == null) {
      Toast.makeText(this,"Error instantiating account "+rowId,Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    if (rowId != 0) {
      mNewInstance = false;
      setTitle(R.string.menu_edit_account);
      mLabelText.setText(mAccount.label);
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
    configTypeButton();
    mAmountText.setFractionDigits(Money.getFractionDigits(mAccount.currency));

    mCurrencySpinner = new SpinnerHelper(findViewById(R.id.Currency));
    currencyAdapter = new ArrayAdapter<>(
        this, android.R.layout.simple_spinner_item,
        android.R.id.text1, CurrencyEnum.sortedValues());
    currencyAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mCurrencySpinner.setAdapter(currencyAdapter);

    mAccountTypeSpinner = new SpinnerHelper(findViewById(R.id.AccountType));
    ArrayAdapter<AccountType> typAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, android.R.id.text1, AccountType.values());
    typAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mAccountTypeSpinner.setAdapter(typAdapter);

    mColorSpinner = new SpinnerHelper(findViewById(R.id.Color));
    mColors = new ArrayList<>();
    Resources r = getResources();
    mColors.add(r.getColor(R.color.material_red));
    mColors.add(r.getColor(R.color.material_pink));
    mColors.add(r.getColor(R.color.material_purple));
    mColors.add(r.getColor(R.color.material_deep_purple));
    mColors.add(r.getColor(R.color.material_indigo));
    mColors.add(r.getColor(R.color.material_blue));
    mColors.add(r.getColor(R.color.material_light_blue));
    mColors.add(r.getColor(R.color.material_cyan));
    mColors.add(r.getColor(R.color.material_teal));
    mColors.add(r.getColor(R.color.material_green));
    mColors.add(r.getColor(R.color.material_light_green));
    mColors.add(r.getColor(R.color.material_lime));
    mColors.add(r.getColor(R.color.material_yellow));
    mColors.add(r.getColor(R.color.material_amber));
    mColors.add(r.getColor(R.color.material_orange));
    mColors.add(r.getColor(R.color.material_deep_orange));
    mColors.add(r.getColor(R.color.material_brown));
    mColors.add(r.getColor(R.color.material_grey));
    mColors.add(r.getColor(R.color.material_blue_grey));

    if (mColors.indexOf(mAccount.color) == -1)
      mColors.add(mAccount.color);

    mColAdapter = new ArrayAdapter<Integer>(this,
        android.R.layout.simple_spinner_item, mColors) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getView(position, convertView, parent);
        if (mColors.get(position) != 0)
          setColor(tv,mColors.get(position));
        else
          setColor(tv,mAccount.color);
        return tv;
      }
      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
        if (mColors.get(position) != 0)
          setColor(tv,mColors.get(position));
        return tv;
      }
      public void setColor(TextView tv, int color) {
        tv.setBackgroundColor(color);
        tv.setText("");
        tv.setContentDescription(getString(R.string.color));
      }
    };
    mColAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mColorSpinner.setAdapter(mColAdapter);

    mSyncSpinner = new SpinnerHelper(findViewById(R.id.Sync));
    SyncBackendProviderArrayAdapter syncBackendProviderArrayAdapter =
        new SyncBackendProviderArrayAdapter(this, android.R.layout.simple_spinner_item, true);
    syncBackendProviderArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSyncSpinner.setAdapter(syncBackendProviderArrayAdapter);

    linkInputsWithLabels();
    populateFields();
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_COLOR_REQUEST && resultCode == RESULT_OK) {
      mAccount.color = data.getExtras().getInt(OPENINTENTS_COLOR_EXTRA);
      if (mColors.indexOf(mAccount.color) == -1) {
        final int lastButOne = mColors.size()-1;
        mColors.add(lastButOne,mAccount.color);
        mColorSpinner.setSelection(lastButOne,true);
        mColAdapter.notifyDataSetChanged();
      }
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
    mCurrencySpinner.setSelection(currencyAdapter.getPosition(
        CurrencyEnum.valueOf(mAccount.currency.getCurrencyCode())));
    mAccountTypeSpinner.setSelection(mAccount.type.ordinal());
    int selected = mColors.indexOf(mAccount.color);
    mColorSpinner.setSelection(selected);
    if (mAccount.getSyncAccountName() != null) {
      int position = ((ArrayAdapter<String>) mSyncSpinner.getAdapter()).getPosition(mAccount.getSyncAccountName());
      if (position > -1) {
        mSyncSpinner.setSelection(position);
      }
    }
  }

  /**
   * validates currency (must be code from ISO 4217) and opening balance
   * (a valid float according to the format from the locale)
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
      Toast.makeText(this, currency + " not supported by your OS. Please select a different currency.",Toast.LENGTH_LONG).show();
      return;
    }

    label = mLabelText.getText().toString();
    if (label.equals("")) {
      mLabelText.setError(getString(R.string.no_title_given));
      return;
    }
    mAccount.label = label;
    mAccount.description = mDescriptionText.getText().toString();
    if (mType == EXPENSE) {
      openingBalance = openingBalance.negate();
    }
    mAccount.openingBalance.setAmountMajor(openingBalance);
    mAccount.type = (AccountType) mAccountTypeSpinner.getSelectedItem();
    if (mSyncSpinner.getSelectedItemPosition() > 0) {
      mAccount.setSyncAccountName((String) mSyncSpinner.getSelectedItem());
    }
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
    switch(parent.getId()) {
      case R.id.Color:
        if (mColors.get(position) != 0) mAccount.color = mColors.get(position);
        break;
      case R.id.Currency:
        try {
          mAmountText.setFractionDigits(Money.getFractionDigits(
              Currency.getInstance(CurrencyEnum.values()[position].name())));
        } catch (IllegalArgumentException e) {
          //will be reported to user when he tries so safe
        }
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
      Toast.makeText(this, "Unknown error while saving account", Toast.LENGTH_SHORT).show();
    } else {
      Intent intent = new Intent();
      long id = ContentUris.parseId((Uri) result);
      intent.putExtra(DatabaseConstants.KEY_ROWID, id);
      setResult(RESULT_OK, intent);
    }
    finish();
    //no need to call super after finish
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuItemCompat.setShowAsAction(
        menu.add(Menu.NONE, R.id.EXCLUDE_FROM_TOTALS_COMMAND, 0, R.string.menu_exclude_from_totals)
          .setCheckable(true),
        MenuItemCompat.SHOW_AS_ACTION_NEVER);
    MenuItemCompat.setShowAsAction(
        menu.add(Menu.NONE, R.id.SYNCED_COMMAND, 0, "START SYNC")
            .setCheckable(true),
        MenuItemCompat.SHOW_AS_ACTION_NEVER);
    return true;
  }
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    requireAccount();
    if (mAccount==null) {
      AcraHelper.report(new NullPointerException("mAccount is null"));
    } else {
      MenuItem item = menu.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND);
      if (item==null) {
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
      if (mAccount.getId()!=0) {
        startTaskExecution(
            TaskExecutionFragment.TASK_TOGGLE_EXCLUDE_FROM_TOTALS,
            new Long[] {mAccount.getId()},
            mAccount.excludeFromTotals, 0);
        supportInvalidateOptionsMenu();
      }
      return true;
      case R.id.SYNCED_COMMAND:
        if (mAccount.getId() != 0) {
          startTaskExecution(
              TaskExecutionFragment.TASK_START_SYNC,
              new Long[]{mAccount.getId()},
              null, 0);
          supportInvalidateOptionsMenu();
        }
        return true;

    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  protected void setupListeners() {
    super.setupListeners();
    mLabelText.addTextChangedListener(this);
    mDescriptionText.addTextChangedListener(this);
    mColorSpinner.setOnItemSelectedListener(this);
    mAccountTypeSpinner.setOnItemSelectedListener(this);
    mCurrencySpinner.setOnItemSelectedListener(this);
    mSyncSpinner.setOnItemSelectedListener(this);
  }

  @Override
  protected void linkInputsWithLabels() {
    super.linkInputsWithLabels();
    linkInputWithLabel(mLabelText,findViewById(R.id.LabelLabel));
    linkInputWithLabel(mDescriptionText,findViewById(R.id.DescriptionLabel));
    linkInputWithLabel(mColorSpinner.getSpinner(),findViewById(R.id.ColorLabel));
    linkInputWithLabel(mAccountTypeSpinner.getSpinner(),findViewById(R.id.AccountTypeLabel));
    linkInputWithLabel(mCurrencySpinner.getSpinner(),findViewById(R.id.CurrencyLabel));
    linkInputWithLabel(mSyncSpinner.getSpinner(), findViewById(R.id.SyncLabel));
  }
}
