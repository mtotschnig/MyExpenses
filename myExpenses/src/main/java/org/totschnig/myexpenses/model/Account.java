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

import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CrStatusCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.totschnig.myexpenses.provider.DatabaseConstants.HAS_CLEARED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.HAS_EXPORTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.HAS_FUTURE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_AGGREGATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SELECT_AMOUNT_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_EXPORTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_EXPENSE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_IN_PAST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_TRANSFER;

/**
 * Account represents an account stored in the database.
 * Accounts have label, opening balance, description and currency
 *
 * @author Michael Totschnig
 */
public class Account extends Model {

  public static final int EXPORT_HANDLE_DELETED_DO_NOTHING = -1;
  public static final int EXPORT_HANDLE_DELETED_UPDATE_BALANCE = 0;
  public static final int EXPORT_HANDLE_DELETED_CREATE_HELPER = 1;

  public String label;

  public Money openingBalance;

  public Currency currency;

  public String description;

  public int color;

  public boolean excludeFromTotals = false;

  private String syncAccountName;

  private SortDirection sortDirection = SortDirection.DESC;

  public String getSyncAccountName() {
    return syncAccountName;
  }

  public void setSyncAccountName(String syncAccountName) {
    this.syncAccountName = syncAccountName;
  }


  public static final String[] PROJECTION_BASE, PROJECTION_EXTENDED, PROJECTION_FULL;
  public static final String CURRENT_BALANCE_EXPR = KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART
      + " AND " + WHERE_IN_PAST + " )";

  static {
    PROJECTION_BASE = new String[]{
        KEY_ROWID,
        KEY_LABEL,
        KEY_DESCRIPTION,
        KEY_OPENING_BALANCE,
        KEY_CURRENCY,
        KEY_COLOR,
        KEY_GROUPING,
        KEY_TYPE,
        KEY_SORT_KEY,
        KEY_EXCLUDE_FROM_TOTALS,
        HAS_EXPORTED,
        KEY_SYNC_ACCOUNT_NAME,
        KEY_UUID,
        KEY_SORT_DIRECTION
    };
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength + 1];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = CURRENT_BALANCE_EXPR + " AS " + KEY_CURRENT_BALANCE;
    PROJECTION_FULL = new String[baseLength + 13];
    System.arraycopy(PROJECTION_EXTENDED, 0, PROJECTION_FULL, 0, baseLength + 1);
    PROJECTION_FULL[baseLength + 1] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_INCOME + ") AS " + KEY_SUM_INCOME;
    PROJECTION_FULL[baseLength + 2] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_EXPENSE + ") AS " + KEY_SUM_EXPENSES;
    PROJECTION_FULL[baseLength + 3] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_TRANSFER + ") AS " + KEY_SUM_TRANSFERS;
    PROJECTION_FULL[baseLength + 4] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " ) AS " + KEY_TOTAL;
    PROJECTION_FULL[baseLength + 5] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " AND " + KEY_CR_STATUS + " IN " +
            "('" + CrStatus.RECONCILED.name() + "','" + CrStatus.CLEARED.name() + "')" +
            " ) AS " + KEY_CLEARED_TOTAL;
    PROJECTION_FULL[baseLength + 6] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " AND " + KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'  ) AS " + KEY_RECONCILED_TOTAL;
    PROJECTION_FULL[baseLength + 7] = KEY_USAGES;
    PROJECTION_FULL[baseLength + 8] = "0 AS " + KEY_IS_AGGREGATE;//this is needed in the union with the aggregates to sort real accounts first
    PROJECTION_FULL[baseLength + 9] = HAS_FUTURE;
    PROJECTION_FULL[baseLength + 10] = HAS_CLEARED;
    PROJECTION_FULL[baseLength + 11] = AccountType.sqlOrderExpression();
    PROJECTION_FULL[baseLength + 12] = KEY_LAST_USED;

  }

  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;

  private AccountType type;

  private Grouping grouping = Grouping.NONE;

  public static final int DEFAULT_COLOR = 0xff009688;

  protected static final Map<Long, Account> accounts =  Collections.synchronizedMap(new HashMap<>());

  private static boolean isInstanceCached(long id) {
    return accounts.containsKey(id);
  }

  public static void reportNull(long id) {
    //This can happen if user deletes account, and changes
    //device orientation before the accounts cursor in MyExpenses is switched
    /*org.acra.ACRA.getErrorReporter().handleSilentException(
        new Exception("Error instantiating account "+id));*/
  }

  /**
   * @param id id of account to be retrieved, if id == 0, the first entry in the accounts cache will be returned or
   *           if it is empty the account with the lowest id will be fetched from db,
   *           if id < 0 we forward to AggregateAccount
   * @return Account object or null if no account with id exists in db
   */
  public static Account getInstanceFromDb(long id) {
    if (id < 0)
      return AggregateAccount.getInstanceFromDb(id);
    Account account;
    String selection = KEY_ROWID + " = ";
    if (id == 0) {
      Set<Long> ids = accounts.keySet();
      synchronized (accounts) {
        for (Long _id : ids) {
          if (_id > 0) {
            return accounts.get(_id);
          }
        }
      }
      selection += "(SELECT min(" + KEY_ROWID + ") FROM accounts)";
    } else {
      account = accounts.get(id);
      if (account != null) {
        return account;
      }
      selection += id;
    }
    Cursor c = cr().query(
        CONTENT_URI, null, selection, null, null);
    if (c == null) {
      //reportNull(id);
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      //reportNull(id);
      return null;
    }
    c.moveToFirst();
    account = new Account(c);
    c.close();
    return account;
  }

  static Account getInstanceFromDbWithFallback(long id) {
    Account account = getInstanceFromDb(id);
    if (account == null) {
      account = getInstanceFromDb(0);
    }
    return account;
  }

  /**
   * empty the cache
   */
  public static void clear() {
    accounts.clear();
  }

  public static void checkSyncAccounts(Context context) {
    String[] validAccounts = GenericAccountService.getAccountsAsStream(context)
        .map(account -> account.name)
        .toArray(size -> new String[size]);
    ContentValues values = new ContentValues(1);
    values.putNull(KEY_SYNC_ACCOUNT_NAME);
    String where = validAccounts.length > 0 ?
        KEY_SYNC_ACCOUNT_NAME + " NOT " + WhereFilter.Operation.IN.getOp(validAccounts.length) :
        null;
    context.getContentResolver().update(TransactionProvider.ACCOUNTS_URI, values,
        where, validAccounts);
    List<String> validAccountNames = Arrays.asList(validAccounts);
    for (Account account: accounts.values()) {
      if (account.syncAccountName != null && validAccountNames.indexOf(account.syncAccountName) == -1) {
        account.syncAccountName = null;
      }
    }
  }

  public static void delete(long id) throws RemoteException, OperationApplicationException {
    Account account = getInstanceFromDb(id);
    if (account == null) {
      return;
    }
    if (account.getSyncAccountName() != null) {
      AccountManager accountManager = AccountManager.get(MyApplication.getInstance());
      android.accounts.Account syncAccount = GenericAccountService.GetAccount(account.getSyncAccountName());
      accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_LOCAL(account.getId()), null);
      accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_REMOTE(account.getId()), null);
    }
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ops.add(account.updateTransferPeersForTransactionDelete(
        buildTransactionRowSelect(null),
        new String[]{String.valueOf(account.getId())}));
    ops.add(ContentProviderOperation.newDelete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build())
        .build());
    cr().applyBatch(TransactionProvider.AUTHORITY, ops);
    accounts.remove(id);
    updateNewAccountEnabled();
    updateTransferShortcut();
  }

  /**
   * returns an empty Account instance
   */
  public Account() {
    this("", (long) 0, "");
  }

  /**
   * Account with currency from locale, of type CASH and with DEFAULT_COLOR
   *
   * @param label          the label
   * @param openingBalance the opening balance
   * @param description    the description
   */
  public Account(String label, long openingBalance, String description) {
    this(label, Utils.getLocalCurrency(), openingBalance, description, AccountType.CASH, DEFAULT_COLOR);
  }

  public Account(String label, Currency currency, long openingBalance, String description,
                 AccountType type, int color) {
    this(label, currency, new Money(currency, openingBalance), description, type, color);
  }

  public Account(String label, Currency currency, Money openingBalance, String description,
                 AccountType type, int color) {
    this.label = label;
    this.currency = currency;
    this.openingBalance = openingBalance;
    this.description = description;
    this.setType(type);
    this.color = color;
  }

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public Account(Cursor c) {
    extract(c);
    accounts.put(getId(), this);
  }

  /**
   * extract information from Cursor and populate fields
   *
   * @param c a Cursor retrieved from {@link TransactionProvider#ACCOUNTS_URI}
   */

  protected void extract(Cursor c) {
    this.setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
    this.label = c.getString(c.getColumnIndexOrThrow(KEY_LABEL));
    this.description = c.getString(c.getColumnIndexOrThrow(KEY_DESCRIPTION));
    this.currency = Utils.getSaveInstance(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY)));
    this.openingBalance = new Money(this.currency,
        c.getLong(c.getColumnIndexOrThrow(KEY_OPENING_BALANCE)));
    try {
      this.setType(AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE))));
    } catch (IllegalArgumentException ex) {
      this.setType(AccountType.CASH);
    }
    try {
      this.setGrouping(Grouping.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_GROUPING))));
    } catch (IllegalArgumentException ignored) {}
    try {
      //TODO ???
      this.color = c.getInt(c.getColumnIndexOrThrow(KEY_COLOR));
    } catch (IllegalArgumentException ex) {
      this.color = DEFAULT_COLOR;
    }
    this.excludeFromTotals = c.getInt(c.getColumnIndex(KEY_EXCLUDE_FROM_TOTALS)) != 0;

    this.syncAccountName = c.getString(c.getColumnIndex(KEY_SYNC_ACCOUNT_NAME));

    this.uuid = c.getString(c.getColumnIndex(KEY_UUID));

    try {
      this.sortDirection = SortDirection.valueOf(c.getString(c.getColumnIndex(KEY_SORT_DIRECTION)));
    } catch (IllegalArgumentException e) {
      this.sortDirection = SortDirection.DESC;
    }
  }

  public void setCurrency(String currency) throws IllegalArgumentException {
    this.currency = Currency.getInstance(currency);
    openingBalance.setCurrency(this.currency);
  }

  /**
   * @return the sum of opening balance and all transactions for the account
   */
  @VisibleForTesting
  public Money getTotalBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() + getTransactionSum(null)
    );
  }

  /**
   * @return the sum of opening balance and all cleared and reconciled transactions for the account
   */
  @VisibleForTesting
  public Money getClearedBalance() {
    WhereFilter filter = WhereFilter.empty();
    filter.put(R.id.FILTER_STATUS_COMMAND,
        new CrStatusCriteria(CrStatus.RECONCILED.name(), CrStatus.CLEARED.name()));
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(filter));
  }

  /**
   * @return the sum of opening balance and all reconciled transactions for the account
   */
  @VisibleForTesting
  public Money getReconciledBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(reconciledFilter()));
  }

  /**
   * @param filter if not null only transactions matched by current filter will be taken into account
   *               if null all transactions are taken into account
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getFilteredBalance(WhereFilter filter) {
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(filter));
  }

  /**
   * @return sum of all transcations
   */
  public long getTransactionSum(WhereFilter filter) {
    String selection = KEY_ACCOUNTID + " = ? AND " + WHERE_NOT_SPLIT_PART;
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_COMMITTED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor c = cr().query(Transaction.CONTENT_URI,
        new String[]{"sum(" + KEY_AMOUNT + ")"},
        selection,
        selectionArgs,
        null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result;
  }

  /**
   * deletes all expenses and updates account according to value of handleDelete
   *
   * @param filter        if not null only expenses matched by filter will be deleted
   * @param handleDelete  if equals {@link #EXPORT_HANDLE_DELETED_UPDATE_BALANCE} opening balance will
   *                      be adjusted to account for the deleted expenses,
   *                      if equals {@link #EXPORT_HANDLE_DELETED_CREATE_HELPER} a helper transaction
   * @param helperComment
   */
  public void reset(WhereFilter filter, int handleDelete, String helperComment) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ContentProviderOperation handleDeleteOperation = null;
    if (handleDelete == EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
      long currentBalance = getFilteredBalance(filter).getAmountMinor();
      openingBalance.setAmountMinor(currentBalance);
      handleDeleteOperation = ContentProviderOperation.newUpdate(
          CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValue(KEY_OPENING_BALANCE, currentBalance)
          .build();
    } else if (handleDelete == EXPORT_HANDLE_DELETED_CREATE_HELPER) {
      Transaction helper = new Transaction(getId(), new Money(currency,getTransactionSum(filter)));
      helper.setComment(helperComment);
      helper.status = STATUS_HELPER;
      handleDeleteOperation = ContentProviderOperation.newInsert(Transaction.CONTENT_URI)
          .withValues(helper.buildInitialValues()).build();
    }
    String rowSelect = buildTransactionRowSelect(filter);
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    ops.add(updateTransferPeersForTransactionDelete(rowSelect, selectionArgs));
    ops.add(ContentProviderOperation.newDelete(
        Transaction.CONTENT_URI)
        .withSelection(
            KEY_ROWID + " IN (" + rowSelect + ")",
            selectionArgs)
        .build());
    //needs to be last, otherwise helper transaction would be deleted
    if (handleDeleteOperation != null) ops.add(handleDeleteOperation);
    try {
      cr().applyBatch(TransactionProvider.AUTHORITY, ops);
    } catch (Exception e) {
      AcraHelper.report(e);
      e.printStackTrace();
    }
  }

  public void markAsExported(WhereFilter filter) {
    String selection = KEY_ACCOUNTID + " = ? and " + KEY_PARENTID + " is null";
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.TABLE_TRANSACTIONS);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    ContentValues args = new ContentValues();
    args.put(KEY_STATUS, STATUS_EXPORTED);
    cr().update(Transaction.CONTENT_URI, args,
        selection,
        selectionArgs);
  }

  /**
   * @param accountId id of account or null
   * @return true if the account with id accountId has transactions marked as exported
   * if accountId is null returns true if any account has transactions marked as exported
   */
  public static boolean getHasExported(Long accountId) {
    String selection = null;
    String[] selectionArgs = null;
    if (accountId != null) {
      if (accountId < 0L) {
        //aggregate account
        AggregateAccount aa = AggregateAccount.getInstanceFromDb(accountId);
        selection = KEY_ACCOUNTID + " IN " +
            "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
        if (aa == null) {
          return false;
        }
        selectionArgs = new String[]{aa.currency.getCurrencyCode()};
      } else {
        selection = KEY_ACCOUNTID + " = ?";
        selectionArgs = new String[]{String.valueOf(accountId)};
      }
    }
    Cursor c = cr().query(Transaction.CONTENT_URI,
        new String[]{"max(" + KEY_STATUS + ")"}, selection, selectionArgs, null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result == 1;
  }

  public static boolean getTransferEnabledGlobal() {
    Cursor cursor = cr().query(
        TransactionProvider.AGGREGATES_COUNT_URI,
        null, null, null, null);
    boolean result = cursor.getCount() > 0;
    cursor.close();
    return result;
  }

  private static String buildTransactionRowSelect(WhereFilter filter) {
    String rowSelect = "SELECT " + KEY_ROWID + " from " + TABLE_TRANSACTIONS + " WHERE " + KEY_ACCOUNTID + " = ?";
    if (filter != null && !filter.isEmpty()) {
      rowSelect += " AND " + filter.getSelectionForParents(DatabaseConstants.TABLE_TRANSACTIONS);
    }
    return rowSelect;
  }

  private ContentProviderOperation updateTransferPeersForTransactionDelete(
      String rowSelect, String[] selectionArgs) {
    ContentValues args = new ContentValues();
    args.putNull(KEY_TRANSFER_ACCOUNT);
    args.putNull(KEY_TRANSFER_PEER);
    return ContentProviderOperation.newUpdate(Transaction.CONTENT_URI)
        .withValues(args)
        .withSelection(
            KEY_TRANSFER_PEER + " IN (" + rowSelect + ")",
            selectionArgs)
        .build();
  }

  /**
   * Saves the account, creating it new if necessary
   *
   * @return the id of the account. Upon creation it is returned from the database
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, label);
    initialValues.put(KEY_OPENING_BALANCE, openingBalance.getAmountMinor());
    initialValues.put(KEY_DESCRIPTION, description);
    initialValues.put(KEY_CURRENCY, currency.getCurrencyCode());
    initialValues.put(KEY_TYPE, getType().name());
    initialValues.put(KEY_GROUPING, getGrouping().name());
    initialValues.put(KEY_COLOR, color);
    initialValues.put(KEY_SYNC_ACCOUNT_NAME, syncAccountName);
    initialValues.put(KEY_UUID, requireUuid());

    if (getId() == 0) {
      //if account is added from sync backend uuid is already set

      uri = cr().insert(CONTENT_URI, initialValues);
      if (uri == null) {
        return null;
      }
      setId(ContentUris.parseId(uri));
    } else {
      uri = ContentUris.withAppendedId(CONTENT_URI, getId());
      cr().update(uri, initialValues, null, null);
    }
    if (!accounts.containsKey(getId())) {
      accounts.put(getId(), this);
    }
    Money.ensureFractionDigitsAreCached(currency);
    updateNewAccountEnabled();
    updateTransferShortcut();
    return uri;
  }

  public static int count(String selection, String[] selectionArgs) {
    Cursor cursor = cr().query(CONTENT_URI, new String[]{"count(*)"},
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Account other = (Account) obj;
    if (color != other.color)
      return false;
    if (currency == null) {
      if (other.currency != null)
        return false;
    } else if (!currency.equals(other.currency))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (!getId().equals(other.getId()))
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (openingBalance == null) {
      if (other.openingBalance != null)
        return false;
    } else if (!openingBalance.equals(other.openingBalance))
      return false;
    if (getType() != other.getType())
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.label != null ? this.label.hashCode() : 0;
    result = 31 * result + (this.openingBalance != null ? this.openingBalance.hashCode() : 0);
    result = 31 * result + (this.currency != null ? this.currency.hashCode() : 0);
    result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
    result = 31 * result + this.color;
    result = 31 * result + (this.excludeFromTotals ? 1 : 0);
    result = 31 * result + (this.getType() != null ? this.getType().hashCode() : 0);
    result = 31 * result + (this.getGrouping() != null ? this.getGrouping().hashCode() : 0);
    return result;
  }

  /**
   * mark cleared transactions as reconciled
   *
   * @param resetP if true immediately delete reconciled transactions
   *               and reset opening balance
   */
  public void balance(boolean resetP) {
    ContentValues args = new ContentValues();
    args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name());
    cr().update(Transaction.CONTENT_URI, args,
        KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " is null AND " +
            KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'",
        new String[]{String.valueOf(getId())});
    if (resetP) {
      reset(reconciledFilter(), EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    }
  }

  private WhereFilter reconciledFilter() {
    WhereFilter filter = WhereFilter.empty();
    filter.put(R.id.FILTER_STATUS_COMMAND,
        new CrStatusCriteria(CrStatus.RECONCILED.name()));
    return filter;
  }

  public void persistGrouping(Grouping value) {
    setGrouping(value);
    cr().update(ContentUris.withAppendedId(CONTENT_URI, getId()).buildUpon().appendPath("grouping")
            .appendPath(value.name()).build(),
        null, null, null);
  }

  public void persistSortDirection(SortDirection value) {
    sortDirection = value;
    cr().update(ContentUris.withAppendedId(CONTENT_URI, getId()).buildUpon().appendPath("sortDirection")
            .appendPath(value.name()).build(),
        null, null, null);
  }

  /**
   * Looks for an account with a label. WARNING: If several accounts have the same label, this
   * method fill return the first account retrieved in the cursor, order is undefined
   *
   * @param label label of the account we want to retrieve
   * @return id or -1 if not found
   */
  public static long findAny(String label) {
    String selection = KEY_LABEL + " = ?";
    String[] selectionArgs = new String[]{label};

    Cursor mCursor = cr().query(CONTENT_URI,
        new String[]{KEY_ROWID}, selection, selectionArgs, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }

  public static long findByUuid(String uuid) {
    String selection = KEY_UUID + " = ?";
    String[] selectionArgs = new String[]{uuid};

    Cursor mCursor = cr().query(CONTENT_URI,
        new String[]{KEY_ROWID}, selection, selectionArgs, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }


  /**
   * return an Account or AggregateAccount that matches the one found in the cursor at the row it is
   * positioned at. Either the one found in the cache is returned or it is extracted from the cursor
   *
   * @param cursor
   * @return
   */
  public static Account fromCacheOrFromCursor(Cursor cursor) {
    long accountId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID));
    if (!Account.isInstanceCached(accountId)) {
      //calling the constructors, puts the objects into the cache from where the fragment can
      //retrieve it, without needing to create a new cursor
      if (accountId < 0) {
        return new AggregateAccount(cursor);
      } else {
        return new Account(cursor);
      }
    }
    return accounts.get(accountId);
  }

  public void requestSync() {
    if (syncAccountName != null) {
      Bundle bundle = new Bundle();
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
      bundle.putString(DatabaseConstants.KEY_UUID, uuid);
      ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccountName),
          TransactionProvider.AUTHORITY, bundle);
    }
  }

  public static void updateNewAccountEnabled() {
    boolean newAccountEnabled = true;
    if (!ContribFeature.ACCOUNTS_UNLIMITED.hasAccess()) {
      if (count(null, null) >= ContribFeature.FREE_ACCOUNTS) {
        newAccountEnabled = false;
      }
    }
    PrefKey.NEW_ACCOUNT_ENABLED.putBoolean(newAccountEnabled);
  }

  public static void updateTransferShortcut() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      ShortcutHelper.configureTransferShortcut(MyApplication.getInstance(), count(null, null) > 1);
    }
  }

  public Uri getExtendedUriForTransactionList() {
    return Transaction.EXTENDED_URI;
  }

  public String[] getExtendedProjectionForTransactionList() {
    return Transaction.PROJECTION_EXTENDED;
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
    this.type = type;
  }

  public Grouping getGrouping() {
    return grouping;
  }

  public void setGrouping(Grouping grouping) {
    this.grouping = grouping;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  protected void setSortDirection(SortDirection sortDirection) {
    this.sortDirection = sortDirection;
  }
}
