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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

public class Payee extends Model {
  public static final String[] PROJECTION = new String[] {KEY_ROWID, "name"};
  public static final Uri CONTENT_URI = TransactionProvider.PAYEES_URI;
  /**
   * inserts a new payee if it does not exist yet
   * @param name
   * @return id of new record, or -1, if it already exists
   */
  public static long create(String name) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("name", name);
    Uri uri;
    try {
      uri = cr().insert(CONTENT_URI, initialValues);
    } catch (SQLiteConstraintException e) {
      return -1;
    }
    return Integer.valueOf(uri.getLastPathSegment());
  }
  public static boolean delete(long id) {
    return cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null) > 0;
  }
  public static long update(String value, Long id) {
    ContentValues args = new ContentValues();
    args.put("name", value);
    try {
      return cr().update(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
          args, null, null);
    } catch (SQLiteConstraintException e) {
      // TODO Auto-generated catch block
      return -1;
    }
  }
}
