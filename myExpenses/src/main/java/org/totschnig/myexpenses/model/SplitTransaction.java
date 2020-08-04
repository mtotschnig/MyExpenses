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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.annimon.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Result;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.TransactionProvider.UNCOMMITTED_URI;

public class SplitTransaction extends Transaction implements ISplit {
  public static final String CSV_INDICATOR = "*";
  public static final String CSV_PART_INDICATOR = "-";
  private static String PART_OR_PEER_SELECT = "(" + KEY_PARENTID + "= ? OR " + KEY_TRANSFER_PEER
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

  @Override
  protected Uri getUriForSave(boolean callerIsSyncAdapter) {
    return getStatus() == STATUS_UNCOMMITTED ? UNCOMMITTED_URI : super.getUriForSave(callerIsSyncAdapter);
  }

  static SplitTransaction getNewInstance(@NonNull Account account, boolean forEdit)  {
    SplitTransaction t = new SplitTransaction(account.getId(), new Money(account.getCurrencyUnit(), 0L));
    if (forEdit) {
      t.setStatus(STATUS_UNCOMMITTED);
      //TODO: Strict mode
      t.save();
    }
    return t;
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset, boolean callerIsSyncAdapter, boolean withCommit) {
    ArrayList<ContentProviderOperation> ops = super.buildSaveOperations(offset, parentOffset, callerIsSyncAdapter, withCommit);
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

  /**
   * Create new split transaction with the passed in transactions as parts
   * @param ids
   */
  public static Result split(@NonNull long[] ids) {
    final int count = ids.length;
    if (count == 0) throw new IllegalArgumentException();
    Result FAILURE = Result.ofFailure(count == 1 ? R.string.split_transaction_one_error : R.string.split_transaction_group_error);
    MyApplication application = MyApplication.getInstance();
    ContentResolver cr = application.getContentResolver();
    final String where = KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(count);
    final String[] selectionArgs = Stream.of(ArrayUtils.toObject(ids))
        .map(String::valueOf)
        .toArray(String[]::new);
    String[] projection = {
        KEY_ACCOUNTID, KEY_CURRENCY, KEY_PAYEEID, KEY_CR_STATUS, "avg(" + KEY_DATE + ")", "sum(" + KEY_AMOUNT + ")"
    };
    //columIndexes need to match with above projection
    int cIAccountId = 0;
    int cICurrency = 1;
    int cIPayeeId = 2;
    int ciCrStatus = 3;
    int ciDate = 4;
    int ciAmount = 5;
    final String groupBy = String.format(Locale.ROOT, "%s, %s, %s, %s", KEY_ACCOUNTID, KEY_CURRENCY, KEY_PAYEEID, KEY_CR_STATUS);
    Cursor cursor = cr.query(Transaction.EXTENDED_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_GROUP_BY, groupBy)
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_DISTINCT, "1")
            .build(),
        projection, where, selectionArgs, null);
    if (cursor == null) return FAILURE;
    if (cursor.getCount() > 1) {
      cursor.close();
      return Result.ofFailure(R.string.split_transaction_not_possible);
    }
    if (!cursor.moveToFirst()) {
      cursor.close();
      return FAILURE;
    }
    final long accountId = cursor.getLong(cIAccountId);
    final Money amount = new Money(application.getAppComponent().currencyContext().get(cursor.getString(cICurrency)), cursor.getLong(ciAmount));
    final Long payeeId = DbUtils.getLongOrNull(cursor, cIPayeeId);
    final long date = cursor.getLong(ciDate);
    final String crStatusString = cursor.getString(ciCrStatus);
    cursor.close();
    CrStatus crStatus;
    try {
      crStatus = CrStatus.valueOf(crStatusString);
    } catch (IllegalArgumentException e) {
      Timber.e(e);
      return FAILURE;
    }

    SplitTransaction parent = SplitTransaction.getNewInstance(accountId, false);
    if (parent == null) return FAILURE;
    parent.setAmount(amount);
    parent.setDate(date);
    parent.setPayeeId(payeeId);
    parent.setCrStatus(crStatus);
    final ArrayList<ContentProviderOperation> operations = parent.buildSaveOperations(false);
    ContentValues values = new ContentValues();
    values.put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name());
    values.put(KEY_DATE, date);
    values.putNull(KEY_PAYEEID);
    operations.add(ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
        .withValues(values)
        .withValueBackReference(KEY_PARENTID, 0)
        .withSelection(where, selectionArgs)
        .withExpectedCount(count)
        .build());
    try {
      cr.applyBatch(TransactionProvider.AUTHORITY, operations);
      return count == 1 ? Result.ofSuccess(R.string.split_transaction_one_success) :
          Result.ofSuccess(R.string.split_transaction_group_success, null, count);
    }  catch (RemoteException | OperationApplicationException e) {
      Timber.e(e);
      return FAILURE;
    }
  }

  public boolean unsplit() {
    MyApplication application = MyApplication.getInstance();
    ContentResolver cr = application.getContentResolver();
    ContentValues values = new ContentValues(1);
    values.put(KEY_UUID, getUuid());
    return cr.update(
        TransactionProvider.TRANSACTIONS_URI.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_UNSPLIT)
            .build(),
        values, null, null) == 1;
  }

  public static void cleanupCanceledEdit(Long id) {
    cleanupCanceledEdit(id, UNCOMMITTED_URI, PART_OR_PEER_SELECT);
  }
}
