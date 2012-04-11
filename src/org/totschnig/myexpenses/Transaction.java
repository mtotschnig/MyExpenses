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

package org.totschnig.myexpenses;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.totschnig.myexpenses.Account.AccountNotFoundException;

import android.database.Cursor;

/**
 * Domain class for transactions
 * @author Michael Totschnig
 *
 */
public class Transaction {
  public long id = 0;
  public String comment;
  public Date date;
  public Money amount;
  //for transfers cat_id stores the peer account
  public long catId;
  //stores a short label of the category or the account the transaction is linked to
  public String label;
  public long accountId;
  public String payee;
  public long transfer_peer = 0;
  /**
   * we store the date directly from UI to DB without creating a Date object
   */
  protected String dateAsString;
  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();
  
  /**
   * factory method for retrieving an instance from the db with the given id
   * @param mDbHelper
   * @param id
   * @return instance of {@link Transaction} or {@link Transfer}
   */
  public static Transaction getInstanceFromDb(long id) {
    Transaction t;
    Cursor c = mDbHelper.fetchTransaction(id);
    long transfer_peer = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
    long account_id = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_ACCOUNTID));
    long amount = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT));
    if (transfer_peer != 0) {
      t = new Transfer(account_id,amount);
      t.transfer_peer = transfer_peer;
    }
    else
      t = new Transaction(account_id,amount);
    
    t.id = id;
    t.setDate(c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE)));
    t.comment = c.getString(
        c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
    t.payee = c.getString(
            c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_PAYEE));
    t.catId = c.getLong(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_CATID));
    t.label = c.getString(c.getColumnIndexOrThrow("label"));
    return t;
  }
  /**
   * factory method for creating an object of the correct type and linked to a given account
   * @param mDbHelper
   * @param mOperationType either {@link MyExpenses#TYPE_TRANSACTION} or
   * {@link MyExpenses#TYPE_TRANSFER}
   * @return instance of {@link Transaction} or {@link Transfer} with date initialized to current date
   */
  public static Transaction getTypedNewInstance(boolean mOperationType, long accountId) {
    if(mOperationType == MyExpenses.TYPE_TRANSACTION)
      return new Transaction(accountId,0);
    else 
      return new Transfer(accountId,0);
  }
  
  public static boolean delete(long id) {
    return mDbHelper.deleteTransaction(id);
  }
  
  /**
   * new empty transaction
   * @param mDbHelper
   */
  public Transaction(long accountId,long amount) {
    setDate(new Date());
    Account account;
    try {
      account = Account.getInstanceFromDb(accountId);
    } catch (AccountNotFoundException e) {
      //we should not have to deal with a transaction not belonging to any account
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    this.accountId = accountId;
    this.amount = new Money(account.currency,amount);
  }
  /**
   * we store the date string and create a date object from it
   * @param strDate format accepted by {@link Timestamp#valueOf}
   */
  public void setDate(String strDate) {
    //as a temporary shortcut we store the date as string,
    //since we have tested that this way UI->DB works
    //and have no time at the moment to test detour via Date class
    dateAsString = strDate;
    date = Timestamp.valueOf(strDate);
  }
  public void setDate(Date date){
    this.date = date;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateAsString = dateFormat.format(date);
  }
  /**
   * 
   * @param payee
   */
  public void setPayee(String payee) {
    this.payee = payee;
  }
  /**
   * Saves the transaction, creating it new if necessary
   * as a side effect calls {@link ExpensesDbAdapter#createPayee(String)}
   * @return the id of the transaction. Upon creation it is returned from the database
   */
  public long save() {
    if (!payee.equals("")) {
      mDbHelper.createPayee(payee);
    }
    if (id == 0) {
      id = mDbHelper.createTransaction(dateAsString, amount.getAmountMinor(), comment,catId,accountId,payee);
    } else {
      mDbHelper.updateTransaction(id, dateAsString, amount.getAmountMinor(), comment,catId,payee);
    }
    return id;
  }


}
