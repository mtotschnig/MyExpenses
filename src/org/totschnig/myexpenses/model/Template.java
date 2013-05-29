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

package org.totschnig.myexpenses.model;

import java.util.Date;

import org.totschnig.myexpenses.ExpensesDbAdapter;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.MyExpenses;
import org.totschnig.myexpenses.Utils;
import org.totschnig.myexpenses.MyApplication.ContribFeature;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

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
    title = "";
  }
  public static Template getTypedNewInstance(boolean mOperationType, long accountId) {
    Template t = new Template(accountId,0);
    t.transfer_peer = mOperationType == MyExpenses.TYPE_TRANSACTION ? 0 : -1;
    return t;
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
   * @return the Uri of the template. Upon creation it is returned from the content provider
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, catId);
    initialValues.put(KEY_PAYEE, payee);
    initialValues.put(KEY_METHODID, methodId);
    initialValues.put(KEY_TITLE, title);
    if (id == 0) {
      initialValues.put(KEY_ACCOUNTID, accountId);
      initialValues.put(KEY_TRANSFER_PEER, transfer_peer);
      uri = MyApplication.cr().insert(TransactionProvider.TEMPLATES_URI, initialValues);
    } else {
      Utils.recordUsage(MyApplication.ContribFeature.EDIT_TEMPLATE);
      uri = TransactionProvider.TEMPLATES_URI.buildUpon().appendPath(String.valueOf(id)).build();
      if (MyApplication.cr().update(uri, initialValues, null, null) == -1)
        return null;
    }
    return uri;
  }
}

