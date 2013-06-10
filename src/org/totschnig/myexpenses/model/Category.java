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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

//TODO implement complete DAO
//for the moment we only wrap calls to the content provider
public class Category extends Model {
  public static final String[] PROJECTION = new String[] {KEY_ROWID, KEY_LABEL, KEY_PARENTID};
  public static final Uri CONTENT_URI = TransactionProvider.CATEGORIES_URI;
  /**
   * Creates a new category under a parent
   * @param label
   * @param parentId
   * @return the row ID of the newly inserted row, or -1 if category already exists
   */
  public static long create(String label, long parentId) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, label);
    initialValues.put(KEY_PARENTID, parentId);
    Uri uri;
    try {
      uri = cr().insert(CONTENT_URI, initialValues);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
    return Integer.valueOf(uri.getLastPathSegment());
  }
  /**
   * Updates the label for category
   * @param label
   * @param cat_id
   * @return number of rows affected, or -1 if unique constraint is violated
   */
  public static int update(String label, long id) {
    ContentValues args = new ContentValues();
    args.put(KEY_LABEL, label);
    try {
      return cr().update(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
          args, KEY_ROWID + " = ?", new String[] {String.valueOf(id)});
    } catch (SQLiteConstraintException e) {
      // TODO Auto-generated catch block
      return -1;
    }
  }
  /**
   * Looks for a cat with a label under a given parent
   * @param label
   * @param parentId
   * @return id or -1 if not found
   */
  public static long find(String label, long parentId) {
    Cursor mCursor = cr().query(CONTENT_URI,
        new String[] {KEY_ROWID}, KEY_PARENTID + " = ? and " + KEY_LABEL + " = ?", new String[] {String.valueOf(parentId), label}, null);
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
  public static boolean delete(long id) {
    return cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null) > 0;
  }
  /**
   * How many subcategories under a given parent?
   * @param parentId
   * @return number of subcategories
   */
  public static int countSub(long parentId){
    Cursor mCursor = cr().query(CONTENT_URI,
        new String[] {"count(*)"}, KEY_PARENTID + " = ?", new String[] {String.valueOf(parentId)}, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return 0;
    } else {
      mCursor.moveToFirst();
      int result = mCursor.getInt(0);
      mCursor.close();
      return result;
    }
  }
}