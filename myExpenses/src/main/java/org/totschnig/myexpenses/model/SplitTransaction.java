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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;

public class SplitTransaction extends Transaction {
  public static final String CSV_INDICATOR = "*";
  public static final String CSV_PART_INDICATOR = "-";
  private String PART_OR_PEER_SELECT = "(" + KEY_PARENTID + "= ? OR " + KEY_TRANSFER_PEER
      + " IN (SELECT " + KEY_ROWID + " FROM " + TABLE_TRANSACTIONS + " where "
      + KEY_PARENTID + " = ?))";

  public SplitTransaction(long accountId, Long amount) {
    super(accountId, amount);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  public SplitTransaction(long accountId, Money amount) {
    super(accountId, amount);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  public SplitTransaction(Account account, long amount) {
    super(account, amount);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  public static SplitTransaction getNewInstance(long accountId) {
    return getNewInstance(accountId, true);
  }

  /**
   * @param accountId if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
   * @param forEdit   if true transaction is immediately persisted to DB in uncommited state
   * @return new SplitTransactionw with Account set to accountId
   */
  public static SplitTransaction getNewInstance(long accountId, boolean forEdit) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    SplitTransaction t = new SplitTransaction(account, 0L);
    if (forEdit) {
      t.status = STATUS_UNCOMMITTED;
      //TODO: Strict mode
      t.persistForEdit();
    }
    return t;
  }

  @Override
  public Uri save() {
    if (status == STATUS_UNCOMMITTED) {
      ContribFeature.SPLIT_TRANSACTION.recordUsage();
    }
    return super.save();
  }

  public void persistForEdit() {
    super.save();
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset) {
    ArrayList<ContentProviderOperation> ops = super.buildSaveOperations(offset);
    if (getId() != 0) {
      String idStr = String.valueOf(getId());
      ContentValues statusValues = new ContentValues();

        ops.add(ContentProviderOperation.newDelete(CONTENT_URI).withSelection(
            PART_OR_PEER_SELECT + "  AND " + KEY_STATUS + " != ?",
            new String[]{idStr, idStr, String.valueOf(STATUS_UNCOMMITTED)}).build());
        statusValues.put(KEY_STATUS, STATUS_NONE);
        //for a new split, both the parent and the parts are in state uncommitted
        //when we edit a split only the parts are in state uncommitted,
        //in any case we only update the state for rows that are uncommitted, to
        //prevent altering the state of a parent (e.g. from exported to non-exported)
        ops.add(ContentProviderOperation.newUpdate(CONTENT_URI).withValues(statusValues).withSelection(
            KEY_STATUS + " = ? AND " + KEY_ROWID + " = ?",
            new String[]{String.valueOf(STATUS_UNCOMMITTED), idStr}).build());
        ops.add(ContentProviderOperation.newUpdate(CONTENT_URI).withValues(statusValues).withSelection(
            KEY_STATUS + " = ? AND " + PART_OR_PEER_SELECT,
            new String[]{String.valueOf(STATUS_UNCOMMITTED), idStr, idStr}).build());

      //make sure that parts have the same date as their parent,
      //otherwise they might be incorrectly counted in groups
      ContentValues dateValues = new ContentValues();
      dateValues.put(KEY_DATE, date.getTime() / 1000);
      ops.add(ContentProviderOperation.newUpdate(CONTENT_URI).withValues(dateValues)
          .withSelection(PART_OR_PEER_SELECT, new String[]{idStr, idStr}).build());
    }
    return ops;
  }

  /**
   * all Split Parts are cloned and we work with the uncommitted clones
   *
   * @param clone if true an uncommited clone of the instance is prepared
   */
  public void prepareForEdit(boolean clone) {
    Long oldId = getId();
    if (clone) {
      status = STATUS_UNCOMMITTED;
      super.saveAsNew();
    }
    String idStr = String.valueOf(oldId);
    //we only create uncommited clones if none exist yet
    Cursor c = cr().query(CONTENT_URI, new String[]{KEY_ROWID},
        KEY_PARENTID + " = ? AND NOT EXISTS (SELECT 1 from " + VIEW_UNCOMMITTED
            + " WHERE " + KEY_PARENTID + " = ?)", new String[]{idStr, idStr}, null);
    c.moveToFirst();
    while (!c.isAfterLast()) {
      Transaction part = Transaction.getInstanceFromDb(c.getLong(c.getColumnIndex(KEY_ROWID)));
      part.status = STATUS_UNCOMMITTED;
      part.parentId = getId();
      part.saveAsNew();
      c.moveToNext();
    }
    c.close();
  }

  /**
   * delete uncommitted rows that are related to this split transaction
   */
  public void cleanupCanceledEdit() {
    String idStr = String.valueOf(getId());
    cr().delete(CONTENT_URI, KEY_STATUS + " = ? AND " + PART_OR_PEER_SELECT,
        new String[]{String.valueOf(STATUS_UNCOMMITTED), idStr, idStr});
    cr().delete(CONTENT_URI, KEY_STATUS + " = ? AND " + KEY_ROWID + " = ?",
        new String[]{String.valueOf(STATUS_UNCOMMITTED), idStr});
  }

  public Uri saveAsNew() {
    Long oldId = getId();
    //saveAsNew sets new id
    Uri result = super.saveAsNew();
    Cursor c = cr().query(CONTENT_URI, new String[]{KEY_ROWID},
        KEY_PARENTID + " = ?", new String[]{String.valueOf(oldId)}, null);
    c.moveToFirst();
    while (!c.isAfterLast()) {
      Transaction part = Transaction.getInstanceFromDb(c.getLong(c.getColumnIndex(KEY_ROWID)));
      part.parentId = getId();
      part.saveAsNew();
      c.moveToNext();
    }
    c.close();
    return result;
  }
}
