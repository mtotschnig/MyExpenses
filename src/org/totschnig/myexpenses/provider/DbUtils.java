package org.totschnig.myexpenses.provider;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class DbUtils {
  /**
   * fix for date values that were incorrectly entered to database in non-western locales
   * https://github.com/mtotschnig/MyExpenses/issues/53
   */
  public static void fixDateValues() {
    Cursor c = MyApplication.cr().query(TransactionProvider.TRANSACTIONS_URI, 
        new String[] {KEY_ROWID, KEY_DATE}, null, null, null);
    String dateString;
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
          dateString = TransactionDatabase.dateFormat.format(localeDependent.parse(dateString));
        } catch (ParseException e1) {
          dateString = TransactionDatabase.dateFormat.format(new Date());
          args.put(KEY_COMMENT,"corrupted Date has been reset");
        }
        args.put(KEY_DATE,dateString);
        MyApplication.cr().update(TransactionProvider.TRANSACTIONS_URI, args,KEY_ROWID + " = ?",
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
      }
    } catch (Exception e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
    }
    return result;
  }
}
