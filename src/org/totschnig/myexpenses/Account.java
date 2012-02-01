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

import android.database.Cursor;
import android.util.Log;

/**
 * Account represents an account stored in the database.
 * Accounts have label, opening balance, description and currency
 * 
 * @author Michael Totschnig
 *
 */
public class Account {
 
  public long id = 0;
   
  public String label;
   
  public float openingBalance;
   
  public String description;
   
  /**
   * java.util.Currency based on ISO 4217 currency symbols
   */
  public Currency currency;
  
  private ExpensesDbAdapter mDbHelper;
  
  /**
   * @see <a href="http://www.currency-iso.org/dl_iso_table_a1.xml">http://www.currency-iso.org/dl_iso_table_a1.xml</a>
   */
  public static final String[] ISO_4217 = new String[] {
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

  /**
   * returns an empty Account instance
   * @param mDbHelper the database helper used in the activity
   */
  public Account(ExpensesDbAdapter mDbHelper) {
    this.mDbHelper = mDbHelper;
  }
  
  /**
   * retrieves an Account instance from the database
   * @param mDbHelper
   * @param id
   */
  public Account(ExpensesDbAdapter mDbHelper, long id) {
    this.mDbHelper = mDbHelper;
    this.id = id;
    Cursor c = mDbHelper.fetchAccount(id);
    this.label = c.getString(c.getColumnIndexOrThrow("label"));
    this.openingBalance = c.getFloat(c.getColumnIndexOrThrow("opening_balance"));
    this.description = c.getString(c.getColumnIndexOrThrow("description"));
    setCurrency(c.getString(c.getColumnIndexOrThrow("currency")));
    c.close();
  }
  /**
   * @param currency if not a legal symbol, silently the currency from the Locale
   * is used instead
   */
  public void setCurrency(String currency) {
    try {
      this.currency = Currency.getInstance(currency);
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",currency + " is not defined in ISO 4217");
      this.currency = Currency.getInstance(Locale.getDefault());
    }
  }
  
  /**
   * @return the sum of opening balance and all transactions for the account
   */
  public float getCurrentBalance() { 
    return openingBalance + mDbHelper.getExpenseSum(id);
  }
  
  /**
   * deletes all expenses and set the new opening balance to the current balance
   */
  public void reset() {
    openingBalance = getCurrentBalance();
    mDbHelper.updateAccountOpeningBalance(id,openingBalance);
    mDbHelper.deleteExpenseAll(id);
  }
  
  /**
   * Saves the account, creating it new if necessary
   * @return the id of the account. Upon creation it is returned from the database
   */
  public long save() {
    if (id == 0) {
      id = mDbHelper.createAccount(label, openingBalance, description,currency.getCurrencyCode());
    } else {
      mDbHelper.updateAccount(id, label,openingBalance,description,currency.getCurrencyCode());
    }
    return id;
  }
}

