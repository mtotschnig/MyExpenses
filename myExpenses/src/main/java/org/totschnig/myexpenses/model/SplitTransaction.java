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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.ArrayList;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

public class SplitTransaction extends Transaction {
  public static final String CSV_INDICATOR = "*";
  public static final String CSV_PART_INDICATOR = "-";
  private String PART_OR_PEER_SELECT = "(" + KEY_PARENTID + "= ? OR " + KEY_TRANSFER_PEER
      + " IN (SELECT " + KEY_ROWID + " FROM " + TABLE_TRANSACTIONS + " where "
      + KEY_PARENTID + " = ?))";


  public SplitTransaction(long accountId, Money amount) {
    super(accountId, amount);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  public static SplitTransaction getNewInstance(long accountId) {
    return getNewInstance(accountId, true);
  }

  /**
   * @param accountId if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
   * @param forEdit   if true transaction is immediately persisted to DB in uncommitted state
   * @return new SplitTransactionw with Account set to accountId
   */
  public static SplitTransaction getNewInstance(long accountId, boolean forEdit) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    return getNewInstance(account, forEdit);
  }

  static SplitTransaction getNewInstance(@NonNull Account account, boolean forEdit)  {
    SplitTransaction t = new SplitTransaction(account.getId(), new Money(account.currency, 0L));
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
    Uri uri = super.save();
    inEditState = false;
    return uri;
  }

  public void persistForEdit() {
    super.save();
    inEditState = true;
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset, boolean callerIsSyncAdapter) {
    ArrayList<ContentProviderOperation> ops = super.buildSaveOperations(offset, parentOffset, callerIsSyncAdapter);
    Uri uri = getUriForSave(callerIsSyncAdapter);
    if (getId() != 0) {
      String idStr = String.valueOf(getId());
      if (inEditState) {
        addCommitOperations(uri, ops);
      }
      //make sure that parts have the same date as their parent,
      //otherwise they might be incorrectly counted in groups
      ContentValues dateValues = new ContentValues();
      dateValues.put(KEY_DATE, getDate().getTime() / 1000);
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(dateValues)
          .withSelection(PART_OR_PEER_SELECT, new String[]{idStr, idStr}).build());
    }
    return ops;
  }

  @Override
  public String getPartOrPeerSelect() {
    return PART_OR_PEER_SELECT;
  }

  @Override
  public int operationType() {
    return TYPE_SPLIT;
  }
}
