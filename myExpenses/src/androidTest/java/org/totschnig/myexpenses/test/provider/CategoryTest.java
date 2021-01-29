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
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseDbTest;
import org.totschnig.myexpenses.util.ColorUtils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

public class CategoryTest extends BaseDbTest {

  private CategoryInfo[] TEST_CATEGORIES = new CategoryInfo[4];
  private Long[] testIds = new Long[4];

  private void insertData() {

    TEST_CATEGORIES[0] = new CategoryInfo("Main 1", null);
    testIds[0] = mDb.insertOrThrow(DatabaseConstants.TABLE_CATEGORIES, null, TEST_CATEGORIES[0].getContentValues());
    TEST_CATEGORIES[1] = new CategoryInfo("Main 2", null);
    TEST_CATEGORIES[2] = new CategoryInfo("Sub 1", testIds[0]);
    TEST_CATEGORIES[3] = new CategoryInfo("Sub 2", testIds[0]);

    for (int index = 1; index < TEST_CATEGORIES.length; index++) {

      testIds[index] = mDb.insertOrThrow(
          DatabaseConstants.TABLE_CATEGORIES,
          null,
          TEST_CATEGORIES[index].getContentValues()
      );
    }
  }

  public void testQueriesOnCategoriesUri() {
    final String[] TEST_PROJECTION = {
        DatabaseConstants.KEY_LABEL, DatabaseConstants.KEY_PARENTID
    };

    final String LABEL_SELECTION = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String SELECTION_COLUMNS =
        LABEL_SELECTION + " OR " + LABEL_SELECTION + " OR " + LABEL_SELECTION;

    final String[] SELECTION_ARGS = {"Main 1", "Main 2", "Sub 1"};
    final String SORT_ORDER = DatabaseConstants.KEY_LABEL + " ASC";

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
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
        TransactionProvider.CATEGORIES_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(TEST_CATEGORIES.length, cursor.getCount());
    cursor.close();

    Cursor projectionCursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
        TEST_PROJECTION,
        null,
        null,
        null
    );
    assert projectionCursor != null;

    assertEquals(TEST_PROJECTION.length, projectionCursor.getColumnCount());

    assertEquals(TEST_PROJECTION[0], projectionCursor.getColumnName(0));
    assertEquals(TEST_PROJECTION[1], projectionCursor.getColumnName(1));
    projectionCursor.close();

    projectionCursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
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

  public void testQueriesOnCategoryIdUri() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String[] SELECTION_ARGS = {"Main 1"};

    final String[] CATEGORY_ID_PROJECTION = {
        DatabaseConstants.KEY_ROWID,
        DatabaseConstants.KEY_LABEL};

    Uri categoryIdUri = ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, 1);

    Cursor cursor = getMockContentResolver().query(
        categoryIdUri,
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
        TransactionProvider.CATEGORIES_URI,
        CATEGORY_ID_PROJECTION,
        SELECTION_COLUMNS,
        SELECTION_ARGS,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int inputCategoryId = cursor.getInt(0);

    categoryIdUri = ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, inputCategoryId);
    cursor.close();

    cursor = getMockContentResolver().query(categoryIdUri,
        CATEGORY_ID_PROJECTION,
        SELECTION_COLUMNS,
        SELECTION_ARGS,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    assertEquals(inputCategoryId, cursor.getInt(0));
    cursor.close();
  }

  public void testInserts() {
    CategoryInfo transaction = new CategoryInfo(
        "Main 3", null);

    Uri rowUri = getMockContentResolver().insert(
        TransactionProvider.CATEGORIES_URI,
        transaction.getContentValues()
    );

    long categoryId = ContentUris.parseId(rowUri);

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
        null,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    int labelIndex = cursor.getColumnIndex(DatabaseConstants.KEY_LABEL);
    int parentIdIndex = cursor.getColumnIndex(DatabaseConstants.KEY_PARENTID);

    assertEquals(transaction.parentId, DbUtils.getLongOrNull(cursor,parentIdIndex));
    assertEquals(transaction.label, cursor.getString(labelIndex));

    ContentValues values = transaction.getContentValues();

    values.put(DatabaseConstants.KEY_ROWID, categoryId);

    try {
      getMockContentResolver().insert(TransactionProvider.CATEGORIES_URI, values);
      fail("Expected insert failure for existing record but insert succeeded.");
    } catch (Exception e) {
    }
    values.remove(DatabaseConstants.KEY_ROWID);
    values.put(DatabaseConstants.KEY_PARENTID, 100);
    try {
      getMockContentResolver().insert(TransactionProvider.CATEGORIES_URI, values);
      fail("Expected insert failure for link to non-existing parent but insert succeeded.");
    } catch (Exception e) {
    }
    cursor.close();
  }

  public void testDeleteRespectsForeignKeys() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String[] SELECTION_ARGS_MAIN = {"Main 1"};

    int rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.CATEGORIES_URI,
        SELECTION_COLUMNS,
        SELECTION_ARGS_MAIN
    );

    assertEquals(0, rowsDeleted);

    insertData();

    try {
      getMockContentResolver().delete(
          TransactionProvider.CATEGORIES_URI,
          DatabaseConstants.KEY_LABEL + " = " + "?",
          new String[]{"Main 1"}
      );
      fail("Expected foreign key to prevent main category from being deleted.");
    } catch (SQLiteConstraintException e) {
      // succeeded, so do nothing
    }
  }

  public void testDeleteSucceeds() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " IN (?,?)";

    final String[] SELECTION_ARGS_MAIN = {"Main 1", "Main 2"};
    final String[] SELECTION_ARGS_SUB = {"Sub 1", "Sub 2"};

    insertData();

    int rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.CATEGORIES_URI,
        SELECTION_COLUMNS,
        SELECTION_ARGS_SUB
    );

    assertEquals(2, rowsDeleted);

    rowsDeleted = getMockContentResolver().delete(
        TransactionProvider.CATEGORIES_URI,
        SELECTION_COLUMNS,
        SELECTION_ARGS_MAIN
    );

    assertEquals(2, rowsDeleted);

    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
        null,
        SELECTION_COLUMNS,
        SELECTION_ARGS_MAIN,
        null
    );
    assert cursor != null;

    assertEquals(0, cursor.getCount());
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.CATEGORIES_URI,
        null,
        SELECTION_COLUMNS,
        SELECTION_ARGS_SUB,
        null
    );
    assert cursor != null;

    assertEquals(0, cursor.getCount());
    cursor.close();
  }

  public void testUpdates() {
    final String SELECTION_COLUMNS = DatabaseConstants.KEY_LABEL + " = " + "?";

    final String[] selectionArgs = {"Main 1"};

    ContentValues values = new ContentValues();

    values.put(DatabaseConstants.KEY_LABEL, "Testing an update with this string");

    try {
      getMockContentResolver().update(
          TransactionProvider.CATEGORIES_URI,
          values,
          SELECTION_COLUMNS,
          selectionArgs
      );
      fail("Bulk update should not succeed");
    } catch (UnsupportedOperationException ignored) {}
  }

  public void testUniqueConstraintsCreateMain() {
    insertData();
    CategoryInfo category = new CategoryInfo(
        "Main 1", null);

    try {
      getMockContentResolver().insert(
          TransactionProvider.CATEGORIES_URI,
          category.getContentValues()
      );
      fail("Expected unique constraint to prevent main category from being created.");
    } catch (SQLiteConstraintException e) {
    }
  }

  public void testUniqueConstraintsCreateSub() {
    insertData();
    CategoryInfo category = new CategoryInfo(
        "Sub 1", testIds[0]);

    try {
      getMockContentResolver().insert(
          TransactionProvider.CATEGORIES_URI,
          category.getContentValues()
      );
      fail("Expected unique constraint to prevent sub category from being created.");
    } catch (SQLiteConstraintException e) {
    }
  }

  public void testUniqueConstraintsUpdateMain() {
    insertData();

    //we try to set the name of Main 2 to Main 1
    try {
      ContentValues args = new ContentValues();
      args.put(KEY_LABEL, "Main 1");
      getMockContentResolver().update(
          TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(testIds[1])).build(),
          args, null, null
      );
      fail("Expected unique constraint to prevent main category from being updated.");
    } catch (SQLiteConstraintException e) {
      // succeeded, so do nothing
    }
  }

  public void testUniqueConstraintsUpdateSub() {
    insertData();
    try {
      ContentValues args = new ContentValues();
      args.put(KEY_LABEL, "Sub 1");
      getMockContentResolver().update(
          TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(testIds[3])).build(),
          args, null, null
      );
      fail("Expected unique constraint to prevent sub category from being created.");
    } catch (SQLiteConstraintException e) {
    }
  }

  public void testUpdateColor() {
    insertData();
    ContentValues args = new ContentValues();
    final int testcolor = ColorUtils.MAIN_COLORS[0];
    args.put(KEY_COLOR, testcolor);
    final Uri categoryIdUri = TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(testIds[3])).build();
    getMockContentResolver().update(
        categoryIdUri,
        args, null, null
    );
    final String[] CATEGORY_ID_PROJECTION = {
        DatabaseConstants.KEY_COLOR};
    Cursor cursor = getMockContentResolver().query(categoryIdUri,
        CATEGORY_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    assertEquals(testcolor, cursor.getInt(0));

    cursor.close();
  }

  public void testAutomaticInsertOfColorForMainCategories() {
    final String[] CATEGORY_ID_PROJECTION = {
        DatabaseConstants.KEY_PARENTID,
        DatabaseConstants.KEY_COLOR};
    Cursor cursor = getMockContentResolver().query(TransactionProvider.CATEGORIES_URI,
        CATEGORY_ID_PROJECTION,
        null,
        null,
        null
    );
    assert cursor != null;

    while (cursor.moveToNext()) {
      final int color = cursor.getInt(1);
      if (cursor.isNull(0)) {
        //parentId
        assertTrue(color != 0);
      } else {
        assertEquals(0, color);
      }
    }
    cursor.close();
  }
}
