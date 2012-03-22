/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

package org.totschnig.myexpenses;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.Account.AccountNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class AccountEdit extends Activity {
  private static final int CURRENCY_DIALOG_ID = 0;
  private ExpensesDbAdapter mDbHelper;
  private EditText mLabelText;
  private EditText mDescriptionText;
  private EditText mOpeningBalanceText;
  private AutoCompleteTextView mCurrencyText;
  private Button mCurrencyButton;
  Account mAccount;
  private NumberFormat nfDLocal;
  private String[] currencyCodes;
  private String[] currencyDescs;
  private TextWatcher currencyInformer;

  private int monkey_state = 0;

  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENVELOPE) {
      switch (monkey_state) {
      case 0:
        mOpeningBalanceText.setText("100");
        mCurrencyText.setText("EUR");
        monkey_state = 1;
        return true;
      case 1:
        setResult(RESULT_OK);
        saveState();
        finish();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = MyApplication.db();
    nfDLocal = NumberFormat.getNumberInstance();
    nfDLocal.setGroupingUsed(false);
    
    currencyCodes = Account.getCurrencyCodes();
    currencyDescs = Account.getCurrencyDescs();
    
    setContentView(R.layout.one_account);

    mLabelText = (EditText) findViewById(R.id.Label);
    mDescriptionText = (EditText) findViewById(R.id.Description);
    mOpeningBalanceText = (EditText) findViewById(R.id.Opening_balance);
    mCurrencyText = (AutoCompleteTextView) findViewById(R.id.Currency);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_dropdown_item_1line, currencyCodes);
    mCurrencyText.setAdapter(adapter);
    
    currencyInformer = new TextWatcher() {
      public void afterTextChanged(Editable s) {
        if (s.length() == 3) {
          int index = java.util.Arrays.asList(currencyCodes).indexOf(
              s.toString());
          if (index > -1) {
            Toast.makeText(AccountEdit.this,currencyDescs[index], Toast.LENGTH_LONG).show();
          }
        }
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
    };

    mCurrencyButton = (Button) findViewById(R.id.Select);
    mCurrencyButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View view) {
        mCurrencyText.removeTextChangedListener(currencyInformer);
        showDialog(CURRENCY_DIALOG_ID);
      }
    });
    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Revert);

    confirmButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (saveState()) {
          Intent intent=new Intent();
          intent.putExtra("account_id", mAccount.id);
          setResult(RESULT_OK,intent);
          finish();
        }
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });
    populateFields();
  }
  /* (non-Javadoc)
   * @see android.app.Activity#onPostCreate(android.os.Bundle)
   * we add the textwatcher only here, to prevent it being triggered
   * during orientation change
   */
  @Override
  protected void onPostCreate (Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    mCurrencyText.addTextChangedListener(currencyInformer);
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case CURRENCY_DIALOG_ID:
        int checked = java.util.Arrays.asList(currencyCodes).indexOf(
            mCurrencyText.getText().toString());
        return new AlertDialog.Builder(this)
          .setTitle(R.string.dialog_title_select_currency)
          .setSingleChoiceItems(currencyDescs, checked, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              mCurrencyText.setText(currencyCodes[item]);
              dismissDialog(CURRENCY_DIALOG_ID);
              mCurrencyText.addTextChangedListener(currencyInformer);
            }
          }).create();
    }
    return null;
  }
  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {
    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      try {
        mAccount = new Account(mDbHelper,rowId);
      } catch (AccountNotFoundException e) {
        e.printStackTrace();
        setResult(RESULT_CANCELED);
        finish();
      }
      setTitle(R.string.menu_edit_account);
      mLabelText.setText(mAccount.label);
      mDescriptionText.setText(mAccount.description);
      mOpeningBalanceText.setText(nfDLocal.format(mAccount.openingBalance));
      mCurrencyText.setText(mAccount.currency.getCurrencyCode());
    } else {
      mAccount = new Account(mDbHelper);
      setTitle(R.string.menu_insert_account);
      Locale l = Locale.getDefault();
      Currency c = Currency.getInstance(l);
      String s = c.getCurrencyCode();
      mCurrencyText.setText(s);
    }
  }

  /**
   * validates currency (must be code from ISO 4217) and opening balance
   * (a valid float according to the format from the locale)
   * @return true upon success, false if validation fails
   */
  private boolean saveState() {
    String currency = mCurrencyText.getText().toString();
    try {
      mAccount.currency = Currency.getInstance(currency);
    } catch (IllegalArgumentException e) {
      Toast.makeText(this,getString(R.string.currency_not_iso4217,currency), Toast.LENGTH_LONG).show();
      return false;
    }
    mAccount.label = mLabelText.getText().toString();
    mAccount.description = mDescriptionText.getText().toString();
    Float openingBalance = Utils.validateNumber(mOpeningBalanceText.getText().toString());
    if (openingBalance == null) {
      Toast.makeText(this,getString(R.string.invalid_number_format,nfDLocal.format(11.11)), Toast.LENGTH_LONG).show();
      return false;
    }
    mAccount.openingBalance = openingBalance;
    mAccount.save();
    return true;
  }
}
