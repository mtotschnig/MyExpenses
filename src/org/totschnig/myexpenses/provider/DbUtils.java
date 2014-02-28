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

package org.totschnig.myexpenses.provider;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class DbUtils {
  /**
   * fix for date values that were incorrectly entered to database in non-western locales
   * https://github.com/mtotschnig/MyExpenses/issues/53
   * @param cr 
   */
  public static void fixDateValues(ContentResolver cr) {
    Cursor c = cr.query(TransactionProvider.TRANSACTIONS_URI, 
        new String[] {KEY_ROWID, KEY_DATE}, null, null, null);
    String dateString;
    Date date;
    c.moveToFirst();
    while(!c.isAfterLast()) {
      dateString = c.getString(c.getColumnIndex(KEY_DATE));
      SimpleDateFormat localeDependent = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      try {
        Timestamp.valueOf(dateString);
      } catch (IllegalArgumentException e) {
        ContentValues args = new ContentValues();
        //first we try to parse in the users locale
        try {
          date = localeDependent.parse(dateString);
        } catch (ParseException e1) {
          date = new Date();
          args.put(KEY_COMMENT,"corrupted Date has been reset");
        }
        args.put(KEY_DATE,date.getTime()/1000);
        cr.update(TransactionProvider.TRANSACTIONS_URI, args,KEY_ROWID + " = ?",
            new String[] {String.valueOf(c.getLong(c.getColumnIndex(KEY_ROWID)))});
      }
      c.moveToNext();
    }
    c.close();
  }
  public static boolean backup() {
    File backupDb = MyApplication.getBackupDbFile();
    if (backupDb == null)
      return false;
    File currentDb = new File(TransactionProvider.mOpenHelper.getReadableDatabase().getPath());
    if (currentDb.exists()) {
      return Utils.copy(currentDb, backupDb);
    }
    return false;
  }
  public static boolean restore() {
    boolean result = false;
    try {
      MyApplication app = MyApplication.getInstance();
      Account.clear();
      PaymentMethod.clear();
      File dataDir = new File("/data/data/"+ app.getPackageName()+ "/databases/");
      dataDir.mkdir();
      File backupDb = MyApplication.getBackupDbFile();
      if (backupDb == null)
        return false;
      //line below gives app_databases instead of databases ???
      //File currentDb = new File(mCtx.getDir("databases", 0),mDatabaseName);
      File currentDb = new File(dataDir,TransactionDatabase.DATABASE_NAME);

      if (backupDb.exists()) {
        result = Utils.copy(backupDb,currentDb);
        ContentResolver resolver = app.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
        TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
        provider.resetDatabase();
        client.release();
      }
    } catch (Exception e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
    }
    return result;
  }
  //TODO: create generic function
  public static String[] getStringArrayFromCursor(Cursor c, String field) {
    String[] result = new String[c.getCount()];
    if(c.moveToFirst()){
     for (int i = 0; i < c.getCount(); i++){
       result[i] = c.getString(c.getColumnIndex(field));
       c.moveToNext();
     }
    }
    return result;
  }
  public static Long[] getLongArrayFromCursor(Cursor c, String field) {
    Long[] result = new Long[c.getCount()];
    if(c.moveToFirst()){
     for (int i = 0; i < c.getCount(); i++){
       result[i] = c.getLong(c.getColumnIndex(field));
       c.moveToNext();
     }
    }
    return result;
  }
  /**
   * @param c
   * @param field
   * @return Long that is null if field is null in db
   */
  public static Long getLongOrNull(Cursor c, String field) {
    return getLongOrNull(c,c.getColumnIndexOrThrow(field));
  }
  public static Long getLongOrNull(Cursor c, int columnIndex) {
    if (c.isNull(columnIndex))
      return null;
    return c.getLong(columnIndex);
  }
  /**
   * @param c
   * @param field
   * @return Long that is OL if field is null in db
   */
  public static Long getLongOr0L(Cursor c, String field) {
    return getLongOr0L(c,c.getColumnIndexOrThrow(field));
  }
  public static Long getLongOr0L(Cursor c, int columnIndex) {
    if (c.isNull(columnIndex))
      return 0L;
    return c.getLong(columnIndex);
  }
  /**
   * @param c
   * @param field
   * @return String that is guaranteed to be not null
   */
  public static String getString(Cursor c, String field) {
    return getString(c,c.getColumnIndexOrThrow(field));
  }
  public static String getString(Cursor c, int columnIndex) {
    if (c.isNull(columnIndex))
      return "";
    return c.getString(columnIndex);
  }
  public static boolean hasParent(Long id) {
    return Transaction.getInstanceFromDb(id).parentId != null;
  }
}
