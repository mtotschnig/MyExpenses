package org.totschnig.myexpenses;

import java.util.Date;

import android.database.Cursor;

public class Template extends Transaction {
  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();
  public String title;

  public Template(Transaction t, String title) {
    this.title = title;
    this.accountId = t.accountId;
    this.amount = t.amount;
    this.catId = t.catId;
    this.comment = t.comment;
    this.methodId = t.methodId;
    this.payee = t.payee;
    this.transfer_peer = t.transfer_peer;
  }
  public Template(long accountId,long amount) {
    super(accountId,amount);
  }
  public void setDate(Date date){
    //templates have no date
  }
  public static Template getInstanceFromDb(long id) {
    Cursor c = mDbHelper.fetchTemplate(id);
    Template t = new Template(c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ACCOUNTID)),
        c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT))  
        );
    t.transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    t.methodId = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_METHODID));
    t.id = id;
    t.comment = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
    t.payee = c.getString(
            c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE));
    t.catId = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
    c.close();
    return t;
  }
  /**
   * Saves the new template, updating not yet implemented 
   * @return the id of the template. Upon creation it is returned from the database
   */
  public long save() {
    if (id == 0) {
      id = mDbHelper.createTemplate(dateAsString, amount.getAmountMinor(), comment,catId,accountId,payee,transfer_peer,methodId,title);
    } else {
      //not implemented yet
      //mDbHelper.updateTemplate(id, dateAsString, amount.getAmountMinor(), comment,catId,payee, methodId,title);
    }
    return id;
  }
}

