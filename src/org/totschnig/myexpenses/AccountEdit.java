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
import android.database.Cursor;
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
  private Long mRowId;
  
  //from http://www.currency-iso.org/dl_iso_table_a1.xml
  private static final String[] ISO_4217 = new String[] {
    "AED" , "AFN" , "ALL" , "AMD" , "ANG" , "AOA" , "ARS" , "AUD" , "AWG" , "AZN" , 
    "BAM" , "BBD" , "BDT" , "BGN" , "BHD" , "BIF" , "BMD" , "BND" , "BOB" , "BOV" , 
    "BRL" , "BSD" , "BTN" , "BWP" , "BYR" , "BZD" , "CAD" , "CDF" , "CHE" , "CHF" , 
    "CHW" , "CLF" , "CLP" , "CNY" , "COP" , "COU" , "CRC" , "CUC" , "CUP" , "CVE" , 
    "CZK" , "DJF" , "DKK" , "DOP" , "DZD" , "EGP" , "ERN" , "ETB" , "EUR" , "FJD" , 
    "FKP" , "GBP" , "GEL" , "GHS" , "GIP" , "GMD" , "GNF" , "GTQ" , "GYD" , "HKD" , 
    "HNL" , "HRK" , "HTG" , "HUF" , "IDR" , "ILS" , "INR" , "IQD" , "IRR" , "ISK" , 
    "JMD" , "JOD" , "JPY" , "KES" , "KGS" , "KHR" , "KMF" , "KPW" , "KRW" , "KWD" , 
    "KYD" , "KZT" , "LAK" , "LBP" , "LKR" , "LRD" , "LSL" , "LTL" , "LVL" , "LYD" , 
    "MAD" , "MDL" , "MGA" , "MKD" , "MMK" , "MNT" , "MOP" , "MRO" , "MUR" , "MVR" , 
    "MWK" , "MXN" , "MXV" , "MYR" , "MZN" , "NAD" , "NGN" , "NIO" , "NOK" , "NPR" , 
    "NZD" , "OMR" , "PAB" , "PEN" , "PGK" , "PHP" , "PKR" , "PLN" , "PYG" , "QAR" , 
    "RON" , "RSD" , "RUB" , "RWF" , "SAR" , "SBD" , "SCR" , "SDG" , "SEK" , "SGD" , 
    "SHP" , "SLL" , "SOS" , "SRD" , "SSP" , "STD" , "SVC" , "SYP" , "SZL" , "THB" , 
    "TJS" , "TMT" , "TND" , "TOP" , "TRY" , "TTD" , "TWD" , "TZS" , "UAH" , "UGX" , 
    "USD" , "USN" , "USS" , "UYI" , "UYU" , "UZS" , "VEF" , "VND" , "VUV" , "WST" , 
    "XAF" , "XAG" , "XAU" , "XBA" , "XBB" , "XBC" , "XBD" , "XCD" , "XDR" , "XFU" , 
    "XOF" , "XPD" , "XPF" , "XPT" , "XSU" , "XTS" , "XUA" , "XXX" , "YER" , "ZAR" , 
    "ZMK" , "ZWL"
};

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
        android.R.layout.simple_dropdown_item_1line, ISO_4217);
    mCurrencyText.setAdapter(adapter);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Cancel);

    mRowId = savedInstanceState != null ? savedInstanceState.getLong(ExpensesDbAdapter.KEY_ROWID) 
        : null;
    Bundle extras = getIntent().getExtras();
    if (mRowId == null) {
      mRowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID) 
          : 0;
    }

    confirmButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        if (saveState())
          finish();
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
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
    float opening_balance;
    if (mRowId != 0) {
      setTitle(R.string.menu_edit_account);
      Cursor note = mDbHelper.fetchAccount(mRowId);
      startManagingCursor(note);
      try {
        opening_balance = Float.valueOf(note.getString(
            note.getColumnIndexOrThrow("opening_balance")));
      } catch (NumberFormatException e) {
        opening_balance = 0;
      }
      mLabelText.setText(note.getString(
          note.getColumnIndexOrThrow("label")));
      mDescriptionText.setText(note.getString(
          note.getColumnIndexOrThrow("description")));
      mOpeningBalanceText.setText(Float.toString(opening_balance));
      mCurrencyText.setText(note.getString(
          note.getColumnIndexOrThrow("currency")));
    } else {
      setTitle(R.string.menu_insert_account);
      mCurrencyText.setText(Currency.getInstance(Locale.getDefault()).getCurrencyCode());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(ExpensesDbAdapter.KEY_ROWID, mRowId);
  }

  private boolean saveState() {
    String currency = mCurrencyText.getText().toString();
    try {
      Currency.getInstance(currency);
    } catch (IllegalArgumentException e) {
      Toast.makeText(this,getString(R.string.currency_not_iso4217,currency), Toast.LENGTH_LONG).show();
      return false;
    }
    String label = mLabelText.getText().toString();
    String description = mDescriptionText.getText().toString();
    String opening_balance = mOpeningBalanceText.getText().toString();

    if (mRowId == 0) {
      long id = mDbHelper.createAccount(label, opening_balance, description,currency);
      if (id > 0) {
        mRowId = id;
      }
    } else {
      mDbHelper.updateAccount(mRowId, label,opening_balance,description,currency);
    }
    return true;
  }

}
