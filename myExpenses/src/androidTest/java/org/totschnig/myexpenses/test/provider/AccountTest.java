/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.test.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseDbTest;

public class AccountTest extends BaseDbTest {

  private final AccountInfo[] TEST_ACCOUNTS = {
      new AccountInfo("Account 0", AccountType.CASH, 0),
      new AccountInfo("Account 1", AccountType.BANK, 100),
      new AccountInfo("Account 2", AccountType.CCARD, -100),
  };

  private void insertData() {

    for (AccountInfo TEST_ACCOUNT : TEST_ACCOUNTS) {
      mDb.insertOrThrow(
          DatabaseConstants.TABLE_ACCOUNTS,
          null,
          TEST_ACCOUNT.getContentValues()
      );
    }
  }

  private long insertAccountWithTwoBudgets() {
    final Grouping grouping = Grouping.MONTH;
    long accountId = mDb.insertOrThrow(
        DatabaseConstants.TABLE_ACCOUNTS,
        null,
        new AccountInfo("Account with 2 budgets", AccountType.CCARD, -100, "EUR", grouping).getContentValues()
    );
    BudgetInfo[] budgets = {
        new BudgetInfo(accountId, "budget 1", "description", 400, grouping),
        new BudgetInfo(accountId, "budget 2", "description", 5000, grouping)
    };
    for (BudgetInfo budgetInfo : budgets) {
      mDb.insertOrThrow(
          DatabaseConstants.TABLE_BUDGETS,
          null,
          budgetInfo.getContentValues()
      );
    }
    return accountId;
  }

  public void testQueriesOnAccountUri() {
    final String[] TEST_PROJECTION = {
        DatabaseConstants.KEY_LABEL, DatabaseConstants.KEY_DESCRIPTION, DatabaseConstants.KEY_CURRENCY
    };

    final String COMMENT_SELECTION = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String SELECTION_COLUMNS =
        COMMENT_SELECTION + " OR " + COMMENT_SELECTION + " OR " + COMMENT_SELECTION;

    final String[] SELECTION_ARGS = {"Account 0", "Account 1", "Account 2"};

    final String SORT_ORDER = DatabaseConstants.KEY_LABEL + " ASC";

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(0, cursor.getCount());

    insertData();
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(TEST_ACCOUNTS.length, cursor.getCount());
    cursor.close();

    Cursor projectionCursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        TEST_PROJECTION,
        null,
        null,
        null
    );

    assert projectionCursor != null;
    assertEquals(TEST_PROJECTION.length, projectionCursor.getColumnCount());

    assertEquals(TEST_PROJECTION[0], projectionCursor.getColumnName(0));
    assertEquals(TEST_PROJECTION[1], projectionCursor.getColumnName(1));
    assertEquals(TEST_PROJECTION[2], projectionCursor.getColumnName(2));
    projectionCursor.close();

    projectionCursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        TEST_PROJECTION,
        SELECTION_COLUMNS,
        SELECTION_ARGS,
        SORT_ORDER
    );

    assert projectionCursor != null;
    assertEquals(SELECTION_ARGS.length, projectionCursor.getCount());

    int index = 0;

    while (projectionCursor.moveToNext()) {

      assertEquals(SELECTION_ARGS[index], projectionCursor.getString(0));

      index++;
    }

    assertEquals(SELECTION_ARGS.length, index);
    projectionCursor.close();
  }

  public void testAccountIdUriWithMultipleBudgets() {
    long accountId = insertAccountWithTwoBudgets();

    Uri AccountIdUri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId);

    Cursor cursor = getMockContentResolver().query(AccountIdUri,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    cursor.close();
  }

  public void testQueriesOnAccountIdUri() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String query = "Account 0";
    final String[] SELECTION_ARGS = {query};

    final String[] Account_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_LABEL};

    insertData();

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        Account_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(TEST_ACCOUNTS.length, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int inputAccountId = cursor.getInt(0);

    Uri AccountIdUri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, inputAccountId);

    cursor.close();

    cursor = getMockContentResolver().query(AccountIdUri,
        new String[] {DatabaseConstants.KEY_LABEL},
        SELECTION_COLUMNS,
        SELECTION_ARGS,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    assertEquals(query, cursor.getString(0));
    cursor.close();
  }

  public void testInserts() {
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(0, cursor.getCount());

    AccountInfo account = new AccountInfo(
        "Account 4",
        AccountType.ASSET, 1000);

    Uri rowUri = getMockContentResolver().insert(
        TransactionProvider.ACCOUNTS_URI,
        account.getContentValues()
    );

    long AccountId = ContentUris.parseId(rowUri);
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int descriptionIndex = cursor.getColumnIndex(DatabaseConstants.KEY_DESCRIPTION);
    int labelIndex = cursor.getColumnIndex(DatabaseConstants.KEY_LABEL);
    int balanceIndex = cursor.getColumnIndex(DatabaseConstants.KEY_OPENING_BALANCE);
    int currencyIndex = cursor.getColumnIndex(DatabaseConstants.KEY_CURRENCY);

    assertEquals(account.getLabel(), cursor.getString(labelIndex));
    assertEquals(account.getDescription(), cursor.getString(descriptionIndex));
    assertEquals(account.getOpeningBalance(), cursor.getLong(balanceIndex));
    assertEquals(account.getCurrency(), cursor.getString(currencyIndex));

    ContentValues values = account.getContentValues();

    values.put(DatabaseConstants.KEY_ROWID, AccountId);

    try {
      getMockContentResolver().insert(TransactionProvider.ACCOUNTS_URI, values);
      fail("Expected insert failure for existing record but insert succeeded.");
    } catch (Exception e) {

    }
    cursor.close();
  }

  public void testDeletes() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String[] SELECTION_ARGS = {"Account 0"};

    int rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.ACCOUNTS_URI,
        SELECTION_COLUMNS,
        SELECTION_ARGS
    );

    assertEquals(0, rowsDeleted);

    insertData();

    rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.ACCOUNTS_URI,
        SELECTION_COLUMNS,
        SELECTION_ARGS
    );

    assertEquals(1, rowsDeleted);

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        null,
        SELECTION_COLUMNS,
        SELECTION_ARGS,
        null
    );
    assert cursor != null;

    assertEquals(0, cursor.getCount());
    cursor.close();
  }

  public void testUpdates() {

    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String[] selectionArgs = {"Account 1"};

    ContentValues values = new ContentValues();

    values.put(DatabaseConstants.KEY_LABEL, "Testing an update with this string");

    int rowsUpdated = getMockContentResolver().update(
        TransactionProvider.ACCOUNTS_URI,
        values,
        SELECTION_COLUMNS,
        selectionArgs
    );

    assertEquals(0, rowsUpdated);

    insertData();

    rowsUpdated = getMockContentResolver().update(
        TransactionProvider.ACCOUNTS_URI,
        values,
        SELECTION_COLUMNS,
        selectionArgs
    );

    assertEquals(1, rowsUpdated);
  }

  public void testGrouping() {
    final String[] Account_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_GROUPING
    };
    insertData();

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        Account_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(TEST_ACCOUNTS.length, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int inputAccountId = cursor.getInt(0);

    assertEquals(Grouping.NONE.name(), cursor.getString(1));

    Uri accountIdUri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, inputAccountId);

    cursor.close();

    getMockContentResolver().update(ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, inputAccountId)
            .buildUpon().appendPath(Grouping.YEAR.name()).build(),
        null, null, null);


    cursor = getMockContentResolver().query(accountIdUri,
        new String[]{DbUtils.fqcn(DatabaseConstants.TABLE_ACCOUNTS, DatabaseConstants.KEY_GROUPING)},
        null,
        null,
        null
    );
    assert cursor != null;

    assertTrue(cursor.moveToFirst());

    assertEquals(Grouping.YEAR.name(), cursor.getString(0));
    cursor.close();
  }

  public void testSortDirection() {
    final String[] Account_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_SORT_DIRECTION
    };
    insertData();

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,
        Account_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(TEST_ACCOUNTS.length, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int inputAccountId = cursor.getInt(0);

    assertEquals(SortDirection.DESC.name(), cursor.getString(1));

    Uri accountIdUri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, inputAccountId);

    cursor.close();

    getMockContentResolver().update(accountIdUri.buildUpon().appendPath("sortDirection").appendPath(SortDirection.ASC.name()).build(),
        null, null, null);

    cursor = getMockContentResolver().query(accountIdUri,
        new String[] {DatabaseConstants.KEY_SORT_DIRECTION},
        null,
        null,
        null
    );
    assert cursor != null;

    assertTrue(cursor.moveToFirst());

    assertEquals(SortDirection.ASC.name(), cursor.getString(0));
    cursor.close();
  }
}
