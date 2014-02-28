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

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionDatabase;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class SplitTransaction extends Transaction {
  private boolean inEditState = false;
  
  public SplitTransaction(long accountId,Long amount) {
    super(accountId,amount);
    catId = DatabaseConstants.SPLIT_CATID;
  }
  public SplitTransaction(long accountId, Money amount) {
    super(accountId,amount);
    catId = DatabaseConstants.SPLIT_CATID;
  }
  public SplitTransaction(Account account, long amount) {
    super(account,amount);
    catId = DatabaseConstants.SPLIT_CATID;
  }
  @Override
  public Uri save() {
    Uri uri = super.save();
    commit();
    return uri;
  }
  public void persistForEdit() {
    super.save();
    inEditState = true;
  }
  /**
   * existing parts are deleted and the uncommitted ones are committed
   */
  public void commit() {
    if (!inEditState)
      return;
    String idStr = String.valueOf(id);
    cr().delete(CONTENT_URI,"(" + KEY_PARENTID + "= ? OR " + KEY_TRANSFER_PEER
        + " IN (SELECT " + KEY_ROWID + " FROM " + TABLE_TRANSACTIONS + " where " 
        + KEY_PARENTID + " = ?))  AND " + KEY_STATUS + " != ?",
        new String[] { idStr, idStr, String.valueOf(STATUS_UNCOMMITTED) });
    ContentValues initialValues = new ContentValues();
    if (status==STATUS_UNCOMMITTED)
      ContribFeature.Feature.SPLIT_TRANSACTION.recordUsage();
    initialValues.put(KEY_STATUS, 0);
    //for a new split, both the parent and the parts are in state uncommitted
    //when we edit a split only the parts are in state uncommitted,
    //in any case we only update the state for rows that are uncommitted, to
    //prevent altering the state of a parent (e.g. from exported to non-exported)
    cr().update(CONTENT_URI,initialValues,KEY_STATUS + " = ?",
        new String[] {String.valueOf(STATUS_UNCOMMITTED)});
    initialValues.clear();
    //make sure that parts have the same date as their parent,
    //otherwise they might be incorrectly counted in groups
    initialValues.put(KEY_DATE, date.getTime()/1000);
    cr().update(CONTENT_URI,initialValues,KEY_PARENTID + " = ?",
        new String[] {idStr});
    inEditState = false;
  }
  /**
   * all Split Parts are cloned and we work with the uncommitted clones
   */
  public void prepareForEdit() {
    String idStr = String.valueOf(id);
    //we only create uncommited clones if none exist yet
    Cursor c = cr().query(CONTENT_URI, new String[] {KEY_ROWID},
        KEY_PARENTID + " = ? AND NOT EXISTS (SELECT 1 from " + VIEW_UNCOMMITTED
            + " WHERE " + KEY_PARENTID + " = ?)", new String[] {idStr,idStr} , null);
    c.moveToFirst();
    while(!c.isAfterLast()) {
      Transaction t = Transaction.getInstanceFromDb(c.getLong(c.getColumnIndex(KEY_ROWID)));
      t.status = STATUS_UNCOMMITTED;
      t.saveAsNew();
      c.moveToNext();
    }
    c.close();
    inEditState = true;
  }
  /**
   * delete all uncommitted rows in the database,
   * this assumes that there are never more than one edits in parallel
   * TODO get rid of reliance on this assumption
   */
  public void cleanupCanceledEdit() {
    cr().delete(CONTENT_URI, KEY_STATUS + " = ?",
        new String[] { String.valueOf(STATUS_UNCOMMITTED) });
  }
  public Uri saveAsNew() {
    Long oldId = id;
    //saveAsNew sets new id
    Uri result = super.saveAsNew();
    Cursor c = cr().query(CONTENT_URI, new String[] {KEY_ROWID},
        KEY_PARENTID + " = ?", new String[] {String.valueOf(oldId)} , null);
    c.moveToFirst();
    while(!c.isAfterLast()) {
      Transaction t = Transaction.getInstanceFromDb(c.getLong(c.getColumnIndex(KEY_ROWID)));
      t.parentId = id;
      t.saveAsNew();
      c.moveToNext();
    }
    c.close();
    return result;
  }
}
