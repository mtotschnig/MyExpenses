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

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.Normalizer;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.util.Log;

public class Payee extends Model {
  public String name;
  public Payee(Long id, String name) {
    this.setId(id);
    this.name = name;
  }
  public static final String[] PROJECTION = new String[] {
    KEY_ROWID,
    KEY_PAYEE_NAME,
    "(select count(*) from " + TABLE_TRANSACTIONS + " WHERE " + KEY_PAYEEID + "=" + TABLE_PAYEES + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TRANSACTIONS,
    "(select count(*) from " + TABLE_TEMPLATES    + " WHERE " + KEY_PAYEEID + "=" + TABLE_PAYEES + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TEMPLATES
  };
  public static final Uri CONTENT_URI = TransactionProvider.PAYEES_URI;


  /**
   * check if a party exists, create it if not
   * @param name
   * @return id of the existing or the new party
   */
  public static Long require(String name) {
    long id = find(name);
    if (id == -1) {
      Uri uri = new Payee(0L,name).save();
      if (uri == null) {
        //TODO report to ACRA
        Log.w(MyApplication.TAG,"unable to save party "+name);
        return null;
      } else {
        return Long.valueOf(uri.getLastPathSegment());
      }
    } else {
      return id;
    }
  }
  /**
   * Looks for a party with name
   * @param name
   * @return id or -1 if not found
   */
  public static long find(String name) {
    String selection = KEY_PAYEE_NAME + " = ?";
    String[] selectionArgs =new String[]{name};
    Cursor mCursor = cr().query(CONTENT_URI,
        new String[] {KEY_ROWID}, selection, selectionArgs, null);
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
   * @param name
   * @return id of new record, or -1, if it already exists
   */
  public static long maybeWrite(String name) {
    Uri uri = new Payee(0L,name).save();
    return uri == null ? -1 : Long.valueOf(uri.getLastPathSegment());
  }
  public static void delete(long id) {
    cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null);
  }
  @SuppressLint("NewApi")
  @Override
  public Uri save() {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_PAYEE_NAME, name);
    initialValues.put(KEY_PAYEE_NAME_NORMALIZED,
        Utils.normalize(name));
    Uri uri;
    if (getId() == 0) {
      try {
        uri = cr().insert(CONTENT_URI, initialValues);
      } catch (SQLiteConstraintException e) {
        uri = null;
      }
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      try {
        cr().update(CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build(),
            initialValues, null, null);
      } catch (SQLiteConstraintException e) {
        // TODO Auto-generated catch block
        uri = null;
      }
    }
    return uri;
  }
}
