package org.totschnig.myexpenses;

import java.util.Currency;
import java.util.Locale;

import android.database.Cursor;
import android.util.Log;

public class Account {
 
  public int id;
   
  public String label;
   
  public float openingBalance;
   
  public String description;
   
  public Currency currency;
  
  private ExpensesDbAdapter mDbHelper;

  
  //creates a new account
  public Account(ExpensesDbAdapter mDbHelper, String label, float opening_balance, String description, String currency) {
    this.mDbHelper = mDbHelper;
    this.label = label;
    this.openingBalance = opening_balance;
    this.description = description;
    setCurrency(currency);
  }
  public Account(ExpensesDbAdapter mDbHelper, int id) {
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
    mDbHelper.deleteExpenseAll(id);
  }
}

