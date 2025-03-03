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

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.TransactionProvider.UNCOMMITTED_URI;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;

public class SplitTransaction extends Transaction implements ISplit {
  public static final String CSV_INDICATOR = "*";
  public static final String CSV_PART_INDICATOR = "-";
  private static final String PART_OR_PEER_SELECT = "(" + KEY_PARENTID + "= ? OR " + KEY_TRANSFER_PEER
      + " IN (SELECT " + KEY_ROWID + " FROM " + TABLE_TRANSACTIONS + " where "
      + KEY_PARENTID + " = ?))";

  public SplitTransaction() {
    super();
    setCatId(DatabaseConstants.SPLIT_CATID);
  }



  public SplitTransaction(long accountId) {
    super(accountId, (Long) null);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  public SplitTransaction(long accountId, Money amount) {
    super(accountId, amount);
    setCatId(DatabaseConstants.SPLIT_CATID);
  }

  @Override
  protected Uri getUriForSave(boolean callerIsSyncAdapter) {
    return getStatus() == STATUS_UNCOMMITTED ? UNCOMMITTED_URI : super.getUriForSave(callerIsSyncAdapter);
  }

  public static SplitTransaction getNewInstance(ContentResolver contentResolver, long accountId, CurrencyUnit currencyUnit)  {
    return getNewInstance(contentResolver, accountId, currencyUnit, true);
  }

  public static SplitTransaction getNewInstance(ContentResolver contentResolver, long accountId, CurrencyUnit currencyUnit, boolean forEdit)  {
    SplitTransaction t = new SplitTransaction(accountId, new Money(currencyUnit, 0L));
    if (forEdit) {
      t.setStatus(STATUS_UNCOMMITTED);
      //TODO: Strict mode
      t.save(contentResolver);
    }
    return t;
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(ContentResolver contentResolver, int offset, int parentOffset, boolean callerIsSyncAdapter, boolean withCommit) {
    ArrayList<ContentProviderOperation> ops = super.buildSaveOperations(contentResolver, offset, parentOffset, callerIsSyncAdapter, withCommit);
    Uri uri = getUriForSave(callerIsSyncAdapter);
    if (getId() != 0) {
      String idStr = String.valueOf(getId());
      if (withCommit) {
        addCommitOperations(uri, ops);
      }
      //make sure that parts have the same date as their parent,
      //otherwise they might be incorrectly counted in groups
      ContentValues dateValues = new ContentValues();
      dateValues.put(KEY_DATE, getDate());
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(dateValues)
          .withSelection(PART_OR_PEER_SELECT, new String[]{idStr, idStr}).build());
      //verify parts are linked to same account
      ops.add(ContentProviderOperation.newAssertQuery(TransactionProvider.TRANSACTIONS_URI)
                      .withSelection(KEY_PARENTID + " = ? AND " + KEY_ACCOUNTID + " != ?",
                              new String[] {idStr, String.valueOf(getAccountId())})
              .withValue("count(*)", 0).build());
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

  public static void cleanupCanceledEdit(ContentResolver contentResolver, Long id) {
    cleanupCanceledEdit(contentResolver, id, UNCOMMITTED_URI, PART_OR_PEER_SELECT);
  }
}
