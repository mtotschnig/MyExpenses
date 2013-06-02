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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class Template extends Transaction {
  public String title;

  public static final Uri CONTENT_URI = TransactionProvider.TEMPLATES_URI;

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
    String[] projection = new String[] {KEY_ROWID,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
        SHORT_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_ACCOUNTID,KEY_METHODID,KEY_TITLE};
    Cursor c = MyApplication.cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      return null;
      //TODO throw DataObjectNotFoundException
    }
    c.moveToFirst();
    Template t = new Template(c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID)),
        c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT))  
        );
    t.transfer_peer = c.getLong(c.getColumnIndexOrThrow(KEY_TRANSFER_PEER));
    t.methodId = c.getLong(c.getColumnIndexOrThrow(KEY_METHODID));
    t.id = id;
    t.comment = c.getString(
        c.getColumnIndexOrThrow(KEY_COMMENT));
    t.payee = c.getString(
            c.getColumnIndexOrThrow(KEY_PAYEE));
    t.catId = c.getLong(c.getColumnIndexOrThrow(KEY_CATID));
    t.label =  c.getString(c.getColumnIndexOrThrow("label"));
    t.title = c.getString(c.getColumnIndexOrThrow(KEY_TITLE));
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
      uri = MyApplication.cr().insert(CONTENT_URI, initialValues);
      id = Integer.valueOf(uri.getLastPathSegment());
    } else {
      org.totschnig.myexpenses.model.ContribFeature.EDIT_TEMPLATE.recordUsage();
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      if (MyApplication.cr().update(uri, initialValues, null, null) == -1)
        return null;
    }
    return uri;
  }
  public static boolean delete(long id) {
    return MyApplication.cr().delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),null,null) > 0;
  }
  public static int countPerCategory(long catId) {
    return countPerCategory(CONTENT_URI,catId);
  }
  public static int countPerMethod(long catId) {
    return countPerMethod(CONTENT_URI,catId);
  }
  public static int countPerAccount(long catId) {
    return countPerAccount(CONTENT_URI,catId);
  }
  public static int countAll() {
    return countAll(CONTENT_URI);
  }
}