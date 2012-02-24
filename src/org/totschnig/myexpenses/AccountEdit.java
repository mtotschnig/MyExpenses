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

import android.app.Activity;
import android.os.Bundle;
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
  private ExpensesDbAdapter mDbHelper;
  private EditText mLabelText;
  private EditText mDescriptionText;
  private EditText mOpeningBalanceText;
  private AutoCompleteTextView mCurrencyText;
  Account mAccount;
  private NumberFormat nfDLocal = NumberFormat.getNumberInstance();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = MyApplication.db();
    setContentView(R.layout.one_account);

    mLabelText = (EditText) findViewById(R.id.Label);
    mDescriptionText = (EditText) findViewById(R.id.Description);
    mOpeningBalanceText = (EditText) findViewById(R.id.Opening_balance);
    mCurrencyText = (AutoCompleteTextView) findViewById(R.id.Currency);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_dropdown_item_1line, Account.ISO_4217);
    mCurrencyText.setAdapter(adapter);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Cancel);

    confirmButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        if (saveState())
          finish();
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

  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {
    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      mAccount = new Account(mDbHelper,rowId);
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
