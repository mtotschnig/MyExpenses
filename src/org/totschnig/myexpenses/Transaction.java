package org.totschnig.myexpenses;

import java.sql.Timestamp;
import java.util.Date;

import android.database.Cursor;

public class Transaction {
  public long id = 0;
  public String comment;
  public Date date;
  public float amount;
  //for transfers cat_id stores the peer account
  public long cat_id;
  //stores a short label of the category or the account the transaction is linked to
  public String label;
  public long account_id;
  public String payee;
  public long transfer_peer = 0;
  protected String dateAsString;
  protected ExpensesDbAdapter mDbHelper;
  
  static Transaction getInstanceFromDb(ExpensesDbAdapter mDbHelper, long id) {
    Transaction t;
    Cursor c = mDbHelper.fetchExpense(id);
    long transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    if (transfer_peer == 0)
      t = new Transaction(mDbHelper,id,c);
    else
      t = new Transfer(mDbHelper,id,c);
    c.close();
    return t;
  }
  public static Transaction getTypedNewInstance(ExpensesDbAdapter mDbHelper,
      boolean mOperationType) {
    if(mOperationType == MyExpenses.TYPE_TRANSACTION)
      return new Transaction(mDbHelper);
    else 
      return new Transfer(mDbHelper);
  }
  
  public Transaction(ExpensesDbAdapter mDbHelper) {
    this.mDbHelper = mDbHelper;
    this.date = new Date();
  }
  public Transaction(ExpensesDbAdapter mDbHelper, long id, Cursor c) {
    this.mDbHelper = mDbHelper;
    this.id = id;
    dateAsString = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE));
    date = Timestamp.valueOf(dateAsString);
    try {
      amount = Float.valueOf(c.getString(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT)));
    } catch (NumberFormatException e) {
      amount = 0;
    }
    comment = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
    payee = c.getString(
            c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE));
    cat_id = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
    transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    label = c.getString(c.getColumnIndexOrThrow("label"));
    account_id = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ACCOUNTID));
  }
  public void setDate(String strDate) {
    //as a temporary shortcut we store the date as string,
    //since we have tested that this way thus UI->DB works
    //and have no time at the moment to test detour via Date class
    dateAsString = strDate;
    date = Timestamp.valueOf(strDate);
  }
  public void setPayee(String payee) {
    this.payee = payee;
    mDbHelper.createPayeeOrIgnore(payee);
  }
  public long save() {
    if (id == 0) {
      id = mDbHelper.createExpense(dateAsString, amount, comment,cat_id,account_id,payee);
    } else {
      mDbHelper.updateExpense(id, dateAsString, amount, comment,cat_id,payee);
    }
    return id;
  }


}
