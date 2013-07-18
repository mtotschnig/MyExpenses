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
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * Domain class for transactions
 * @author Michael Totschnig
 *
 */
public class Transaction extends Model {
  public Long id = 0L;
  public String comment;
  public Date date;
  public Money amount;
  public Long catId;
  //stores a short label of the category or the account the transaction is linked to
  public String label;
  public Long accountId;
  public String payee;
  public Long transfer_peer;
  public Long transfer_account;
  public Long methodId;
  public static final String[] PROJECTION = new String[]{KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT,
    KEY_CATID,LABEL_MAIN,LABEL_SUB,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_METHODID};
  public static final Uri CONTENT_URI = TransactionProvider.TRANSACTIONS_URI;
  private static final Long SPLIT_CATID = -1L;
  /**
   * we store the date directly from UI to DB without creating a Date object
   */
  protected String dateAsString;

  /**
   * factory method for retrieving an instance from the db with the given id
   * @param mDbHelper
   * @param id
   * @return instance of {@link Transaction} or {@link Transfer}
   * @throws DataObjectNotFoundException 
   */
  public static Transaction getInstanceFromDb(long id) throws DataObjectNotFoundException  {
    Transaction t;
    String[] projection = new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
        SHORT_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_TRANSFER_ACCOUNT,KEY_ACCOUNTID,KEY_METHODID};

    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      throw new DataObjectNotFoundException(id);
    }
    c.moveToFirst();
    Long transfer_peer = DbUtils.getLongOrNull(c, KEY_TRANSFER_PEER);
    long account_id = c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID));
    long amount = c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT));
    if (transfer_peer != null) {
      t = new Transfer(account_id,amount);
      t.transfer_peer = transfer_peer;
      t.transfer_account = DbUtils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
      t.id = id;
    }
    else {
      Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
      if (catId == SPLIT_CATID) {
        SplitTransaction split = new SplitTransaction(account_id,amount);
        split.id = id;
        split.loadParts();
        t = split;
      } else {
        t = new Transaction(account_id,amount);
        t.id = id;
      }
      t.methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
      t.catId = catId;
      t.payee = c.getString(
          c.getColumnIndexOrThrow(KEY_PAYEE));
    }
    t.setDate(c.getString(
        c.getColumnIndexOrThrow(KEY_DATE)));
    t.comment = c.getString(
        c.getColumnIndexOrThrow(KEY_COMMENT));
    t.label = c.getString(c.getColumnIndexOrThrow("label"));
    c.close();
    return t;
  }
  public static Transaction getInstanceFromTemplate(long id) {
    Template te = Template.getInstanceFromDb(id);
    Transaction tr;
    if (te.isTransfer) {
      tr = new Transfer(te.accountId,te.amount);
      tr.transfer_account = te.transfer_account;
    }
    else {
      tr = new Transaction(te.accountId,te.amount);
      tr.methodId = te.methodId;
      tr.catId = te.catId;
    }
    tr.comment = te.comment;
    tr.payee = te.payee;
    tr.label = te.label;
    cr().update(
        TransactionProvider.TEMPLATES_URI.buildUpon().appendPath(String.valueOf(id)).appendPath("increaseUsage").build(),
        null, null, null);
    return tr;
  }
  /**
   * factory method for creating an object of the correct type and linked to a given account
   * @param mDbHelper
   * @param mOperationType either {@link MyExpenses#TYPE_TRANSACTION} or
   * {@link MyExpenses#TYPE_TRANSFER}
   * @return instance of {@link Transaction} or {@link Transfer} with date initialized to current date
   */
  public static Transaction getTypedNewInstance(int mOperationType, long accountId) {
    switch (mOperationType) {
    case MyExpenses.TYPE_TRANSACTION:
      return new Transaction(accountId,0);
    case MyExpenses.TYPE_TRANSFER:
      return new Transfer(accountId,0);
    case MyExpenses.TYPE_SPLIT:
      return new SplitTransaction(accountId,0);
    }
    return null;
  }
  
  public static void delete(long id) {
    Transaction t = Transaction.getInstanceFromDb(id);

    if (t instanceof Transfer)
      cr().delete(CONTENT_URI,
          KEY_ROWID + " in (" + id + "," + t.transfer_peer + ")",null);
    else
      cr().delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),null,null);
  }
  //needed for Template subclass
  public Transaction() {
    setDate(new Date());
  }
  /**
   * new empty transaction
   * @param mDbHelper
   */
  public Transaction(long accountId,long amount) {
    this();
    Account account = Account.getInstanceFromDb(accountId);

    this.accountId = accountId;
    this.amount = new Money(account.currency,amount);
  }
  public Transaction(long accountId,Money amount) {
    this();
    this.accountId = accountId;
    this.amount = amount;
  }
  /**
   * we store the date string and create a date object from it
   * this is only used with String stored in the database, where we are sure that they are correctly formated
   * @param strDate format accepted by {@link TransactionDatabase#dateFormat}
   */
  private void setDate(String strDate) {
    //as a temporary shortcut we store the date as string,
    //since we have tested that this way UI->DB works
    //and have no time at the moment to test detour via Date class
    dateAsString = strDate;
    date = Utils.fromSQL(strDate);
  }
  public void setDate(Date date){
    this.date = date;
    dateAsString = TransactionDatabase.dateFormat.format(date);
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
   * @return the URI of the transaction. Upon creation it is returned from the content provider
   */
  public Uri save() {
    Uri uri;
    if (payee != null && !payee.equals("")) {
      Payee.create(payee);
    }
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, dateAsString);
    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, catId);
    initialValues.put(KEY_PAYEE, payee);
    initialValues.put(KEY_METHODID, methodId);
    if (id == 0) {
      initialValues.put(KEY_ACCOUNTID, accountId);
      uri = cr().insert(CONTENT_URI, initialValues);
      id = ContentUris.parseId(uri);
      if (catId != null)
        cr().update(
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(catId)).appendPath("increaseUsage").build(),
            null, null, null);
    }
    else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      cr().update(uri,initialValues,null,null);
    }
    return uri;
  }
  public Uri saveAsNew() {
    id = 0L;
    setDate(new Date());
    return save();
  }
  /**
   * @param whichTransactionId
   * @param whereAccountId
   * 
   */
  public static void move(long whichTransactionId, long whereAccountId) {
    ContentValues args = new ContentValues();
    args.put(KEY_ACCOUNTID, whereAccountId);
    //we verify that a transfer can not be moved to the account it transfers to
    //IS NOT instead of != accepts cases where transfer_account is null
    cr().update(Uri.parse(CONTENT_URI + "/" + whichTransactionId), args,
        KEY_TRANSFER_ACCOUNT + " IS NOT ?", new String[]{String.valueOf(whereAccountId)});
  }
  public static int count(Uri uri,String selection,String[] selectionArgs) {
    Cursor cursor = cr().query(uri,new String[] {"count(*)"},
        selection, selectionArgs, null);
    if (cursor.getCount() == 0) {
      cursor.close();
      return 0;
    } else {
      cursor.moveToFirst();
      int result = cursor.getInt(0);
      cursor.close();
      return result;
    }
  }
  public static int countAll(Uri uri) {
    return count(uri,null,null);
  }
  public static int countPerCategory(Uri uri,long catId) {
    return count(uri, KEY_CATID + " = ?",new String[] {String.valueOf(catId)});
  }
  public static int countPerMethod(Uri uri,long methodId) {
    return count(uri, KEY_METHODID + " = ?",new String[] {String.valueOf(methodId)});
  }
  public static int countPerAccount(Uri uri,long accountId) {
    return count(uri, KEY_ACCOUNTID + " = ?",new String[] {String.valueOf(accountId)});
  }
  public static int countPerCategory(long catId) {
    return countPerCategory(CONTENT_URI,catId);
  }
  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI,methodId);
  }
  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI,accountId);
  }
  public static int countAll() {
    return countAll(CONTENT_URI);
  }
  /**
   * @return the number of transactions that have been created since creation of the db based on sqllite sequence
   */
  public static long getTransactionSequence() {
    Cursor mCursor = cr().query(TransactionProvider.SQLITE_SEQUENCE_TRANSACTIONS_URI,
        null, null, null, null);
    if (mCursor.getCount() == 0)
      return 0;
    mCursor.moveToFirst();
    int result = mCursor.getInt(0);
    mCursor.close();
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Transaction other = (Transaction) obj;
    if (accountId == null) {
      if (other.accountId != null)
        return false;
    } else if (!accountId.equals(other.accountId))
      return false;
    if (amount == null) {
      if (other.amount != null)
        return false;
    } else if (!amount.equals(other.amount))
      return false;
    if (catId == null) {
      if (other.catId != null)
        return false;
    } else if (!catId.equals(other.catId))
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    //we only compare dateAsString, since dates might have different millisecond values
/*    if (date == null) {
      if (other.date != null)
        return false;
    } else if (!date.equals(other.date))
      return false;*/
    if (dateAsString == null) {
      if (other.dateAsString != null)
        return false;
    } else if (!dateAsString.equals(other.dateAsString))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (methodId == null) {
      if (other.methodId != null)
        return false;
    } else if (!methodId.equals(other.methodId))
      return false;
    if (payee == null) {
      if (other.payee != null)
        return false;
    } else if (!payee.equals(other.payee))
      return false;
    if (transfer_account == null) {
      if (other.transfer_account != null)
        return false;
    } else if (!transfer_account.equals(other.transfer_account))
      return false;
    if (transfer_peer == null) {
      if (other.transfer_peer != null)
        return false;
    } else if (!transfer_peer.equals(other.transfer_peer))
      return false;
    return true;
  }
}
