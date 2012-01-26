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

public class AccountEdit extends Activity {
  private ExpensesDbAdapter mDbHelper;
  private EditText mLabelText;
  private EditText mDescriptionText;
  private EditText mOpeningBalanceText;
  private AutoCompleteTextView mCurrencyText;
  Account mAccount;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();
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
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
  }

  private void populateFields() {
    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      mAccount = new Account(mDbHelper,rowId);
      setTitle(R.string.menu_edit_account);
      mLabelText.setText(mAccount.label);
      mDescriptionText.setText(mAccount.description);
      mOpeningBalanceText.setText(Float.toString(mAccount.openingBalance));
      mCurrencyText.setText(mAccount.currency.getCurrencyCode());
    } else {
      mAccount = new Account(mDbHelper);
      setTitle(R.string.menu_insert_account);
      mCurrencyText.setText(Currency.getInstance(Locale.getDefault()).getCurrencyCode());
    }
  }

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
    try {
      mAccount.openingBalance = Float.valueOf(mOpeningBalanceText.getText().toString());
    } catch (NumberFormatException e) {
      mAccount.openingBalance = 0;
    }

    mAccount.save();
    return true;
  }

}
