package org.totschnig.myexpenses;

import java.util.Currency;
import java.util.Locale;

import android.database.Cursor;
import android.util.Log;

public class Account {
 
  public long id = 0;
   
  public String label;
   
  public float openingBalance;
   
  public String description;
   
  public Currency currency;
  
  private ExpensesDbAdapter mDbHelper;
  
  //from http://www.currency-iso.org/dl_iso_table_a1.xml
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

  //creates an empty account
  public Account(ExpensesDbAdapter mDbHelper) {
    this.mDbHelper = mDbHelper;
  }
  
  //creates a new account with given values
  public Account(ExpensesDbAdapter mDbHelper, String label, float opening_balance, String description, String currency) {
    this.mDbHelper = mDbHelper;
    this.label = label;
    this.openingBalance = opening_balance;
    this.description = description;
    setCurrency(currency);
  }
  //returns an account stored in DB with the given id
  public Account(ExpensesDbAdapter mDbHelper, long id) {
    this.mDbHelper = mDbHelper;
    this.id = id;
    Cursor account = mDbHelper.fetchAccount(id);
    this.label = account.getString(account.getColumnIndexOrThrow("label"));
    this.openingBalance = account.getFloat(account.getColumnIndexOrThrow("opening_balance"));
    this.description = account.getString(account.getColumnIndexOrThrow("description"));
    setCurrency(account.getString(account.getColumnIndexOrThrow("currency")));
    account.close();
  }
  public void setCurrency(String currency) {
    try {
      this.currency = Currency.getInstance(currency);
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",currency + " is not defined in ISO 4217");
      this.currency = Currency.getInstance(Locale.getDefault());
    }
  }
  
  public float getCurrentBalance() { 
    return openingBalance + mDbHelper.getExpenseSum(id);
  }
  
  public void reset() {
    openingBalance = getCurrentBalance();
    mDbHelper.updateAccountOpeningBalance(id,openingBalance);
    mDbHelper.deleteExpenseAll(id);
  }
  
  public long save() {
    if (id == 0) {
      id = mDbHelper.createAccount(label, openingBalance, description,currency.getCurrencyCode());
    } else {
      mDbHelper.updateAccount(id, label,openingBalance,description,currency.getCurrencyCode());
    }
    return id;
  }
}

