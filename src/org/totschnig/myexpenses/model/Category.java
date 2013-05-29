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
public class Category {
  /**
   * Creates a new category under a parent
   * @param label
   * @param parent_id
   * @return the row ID of the newly inserted row, or -1 if category already exists
   */
  public static long create(String label, long parent_id) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("parent_id", parent_id);
    Uri uri = MyApplication.cr().insert(TransactionProvider.CATEGORIES_URI, initialValues);
    if (uri == null)
      return -1;
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
    args.put("label", label);
    return MyApplication.cr().update(TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        args, KEY_ROWID + " = ?", new String[] {String.valueOf(id)});
  }
  /**
   * Looks for a cat with a label under a given parent
   * @param label
   * @param parent_id
   * @return id or -1 if not found
   */
  public static long find(String label, long parent_id) {
    Cursor mCursor = MyApplication.cr().query(TransactionProvider.CATEGORIES_URI,
        new String[] {KEY_ROWID}, "parent_id = ? and label = ?", new String[] {String.valueOf(parent_id), label}, null);
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
    return MyApplication.cr().delete(TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(id)).build(), 
        null, null) > 0;
  }
}
