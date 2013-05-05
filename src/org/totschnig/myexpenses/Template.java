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
    //for Transfers we store -1 as peer since it needs to be different from 0,
    //but we are not interested in which was the transfer_peer of the transfer
    //from which the template was derived;
    this.transfer_peer = t.transfer_peer == 0 ? 0 : -1;
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
    t.label =  c.getString(c.getColumnIndexOrThrow("label"));
    t.title = c.getString(c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TITLE));
    c.close();
    return t;
  }
  /**
   * Saves the new template, or updated an existing one
   * @return the id of the template. Upon creation it is returned from the database
   */
  public long save() {
    if (id == 0) {
      id = mDbHelper.createTemplate(amount.getAmountMinor(), comment,catId,accountId,payee,transfer_peer,methodId,title);
    } else {
      Utils.recordUsage(MyApplication.CONTRIB_FEATURE_EDIT_TEMPLATE);
      mDbHelper.updateTemplate(id, amount.getAmountMinor(), comment,catId,payee, methodId,title);
    }
    return id;
  }
}

