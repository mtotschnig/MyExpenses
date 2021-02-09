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

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.Locale;
import java.util.Map;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

public class Payee extends Model {
  public static final String SELECTION = String.format(Locale.ROOT, "(%1$s LIKE ? OR %1$s GLOB ?)", KEY_PAYEE_NAME_NORMALIZED);
  public static String[] SELECTION_ARGS(String search) {
    return new String[] {
        search + "%",
        "*[ (.;,]" + search + "*"
    };
  }
  private String name;

  public Payee(long id, String name) {
    this.setId(id);
    this.name = strip(name);
  }

  private static String strip(String name) {
    return StringUtils.strip(name);
  }

  public static final String[] PROJECTION = new String[]{
      KEY_ROWID,
      KEY_PAYEE_NAME,
      "exists (select 1 from " + TABLE_TRANSACTIONS + " WHERE " + KEY_PAYEEID + "=" + TABLE_PAYEES + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TRANSACTIONS,
      "exists (select 1 from " + TABLE_TEMPLATES + " WHERE " + KEY_PAYEEID + "=" + TABLE_PAYEES + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TEMPLATES
  };
  public static final Uri CONTENT_URI = TransactionProvider.PAYEES_URI;


  /**
   * check if a party exists, create it if not
   *
   * @param name
   * @return id of the existing or the new party
   */
  static Long require(String name) {
    if (TextUtils.isEmpty(name)) {
      return null;
    }
    long id = find(name);
    if (id == -1) {
      final Payee payee = new Payee(0L, name);
      Uri uri = payee.save();
      if (uri == null) {
        CrashHandler.report(String.format("unable to save party %s", name));
        return null;
      } else {
        return payee.getId();
      }
    } else {
      return id;
    }
  }

  /**
   * Looks for a party with name
   *
   * @param name
   * @return id or -1 if not found
   */
  public static long find(String name) {
    String selection = KEY_PAYEE_NAME + " = ?";
    String[] selectionArgs = new String[]{strip(name)};
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

  public static long findOrWrite(String name) {
    long id = find(name);
    if (id == -1) {
      id = maybeWrite(name);
    }
    return id;
  }

  /**
   * @param name
   * @return id of new record, or -1, if it already exists
   */
  public static long maybeWrite(String name) {
    final Payee payee = new Payee(0L, name);
    Uri uri = payee.save();
    return uri == null ? -1 : payee.getId();
  }

  public static void delete(long id) {
    cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null);
  }

  public static long extractPayeeId(String payeeName, Map<String, Long> payeeToId) {
    Long id = payeeToId.get(payeeName);
    if (id == null) {
      id = Payee.findOrWrite(payeeName);
      if (id != -1) { //should always be the case
        payeeToId.put(payeeName, id);
      }
    }
    return id;
  }

  @Override
  public Uri save() {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_PAYEE_NAME, name);
    initialValues.put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name));
    Uri uri;
    if (getId() == 0) {
      try {
        uri = cr().insert(CONTENT_URI, initialValues);
        setId(ContentUris.parseId(uri));
      } catch (SQLiteConstraintException e) {
        uri = null;
      }
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      try {
        cr().update(CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build(),
            initialValues, null, null);
      } catch (SQLiteConstraintException e) {
        uri = null;
      }
    }
    return uri;
  }

  public String getName() {
    return name;
  }
}
