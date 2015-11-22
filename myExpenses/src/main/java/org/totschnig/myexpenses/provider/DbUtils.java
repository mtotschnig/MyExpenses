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
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.service.AutoBackupService;
import org.totschnig.myexpenses.service.DailyAutoBackupScheduler;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.android.calendar.CalendarContractCompat;

public class DbUtils {

  public static Result backup(File backupDir) {
    SQLiteDatabase db = TransactionProvider.mOpenHelper.getReadableDatabase();
    db.beginTransaction();
    try {
      cacheEventData();
      File backupPrefFile, sharedPrefFile;
      Result result = DbUtils.backupDb(backupDir);
      if (result.success) {
        backupPrefFile = new File(backupDir, MyApplication.BACKUP_PREF_FILE_NAME);
        // Samsung has special path on some devices
        // http://stackoverflow.com/questions/5531289/copy-the-shared-preferences-xml-file-from-data-on-samsung-device-failed
        final MyApplication application = MyApplication.getInstance();
        String sharedPrefPath =  "/shared_prefs/" + application.getPackageName() + "_preferences.xml";
        sharedPrefFile = new File("/dbdata/databases/" + application.getPackageName() + sharedPrefPath);
        if (!sharedPrefFile.exists()) {
          File internalAppDir = application.getFilesDir().getParentFile();
          sharedPrefFile = new File(internalAppDir.getPath() + sharedPrefPath);
          Log.d("DbUtils",sharedPrefFile.getPath());
          if (!sharedPrefFile.exists()) {
            final String message = "Unable to find shared preference file at " +
                sharedPrefFile.getPath();
            Utils.reportToAcra(new Exception(message));
            return new Result(false,message);
          }
        }
        if (Utils.copy(sharedPrefFile, backupPrefFile)) {
          MyApplication.PrefKey.AUTO_BACKUP_DIRTY.putBoolean(false);
          TransactionProvider.mDirty = false;
          return result;
        }
      }
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static Result backupDb(File dir) {
    File backupDb = new File(dir,MyApplication.BACKUP_DB_FILE_NAME);
    File currentDb = new File(TransactionProvider.mOpenHelper.getReadableDatabase().getPath());
    if (currentDb.exists()) {
      if (Utils.copy(currentDb, backupDb)) {
        return new Result(true);
      }
      return new Result(false,String.format(
          "Error while copying %s to %s",currentDb.getPath(),backupDb.getPath()));
    }
    return new Result(false,"Could not find database at " + currentDb.getPath());
  }
  public static boolean restore(File backupFile) {
    boolean result = false;
    MyApplication app = MyApplication.getInstance();
    try {
      DailyAutoBackupScheduler.cancelAutoBackup(app);
      PlanExecutor.cancelPlans(app);
      Account.clear();
      PaymentMethod.clear();
      File dataDir = new File("/data/data/"+ app.getPackageName()+ "/databases/");
      dataDir.mkdir();

      //line below gives app_databases instead of databases ???
      //File currentDb = new File(mCtx.getDir("databases", 0),mDatabaseName);
      File currentDb = new File(dataDir,TransactionDatabase.DATABASE_NAME);

      if (backupFile.exists()) {
        result = Utils.copy(backupFile,currentDb);
        ContentResolver resolver = app.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
        TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
        provider.resetDatabase();
        client.release();
      }
    } catch (Exception e) {
      Utils.reportToAcra(e);
    }
    app.initPlanner();
    DailyAutoBackupScheduler.updateAutoBackupAlarms(app);
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
    return getLongOr0L(c, c.getColumnIndexOrThrow(field));
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
    return getString(c, c.getColumnIndexOrThrow(field));
  }
  public static String getString(Cursor c, int columnIndex) {
    if (c.isNull(columnIndex))
      return "";
    return c.getString(columnIndex);
  }
  public static boolean hasParent(Long id) {
    return Transaction.getInstanceFromDb(id).parentId != null;
  }
  public static String weekStartFromGroupSqlExpression(int year, int week) {
    return String.format(Locale.US, COUNT_FROM_WEEK_START_ZERO + " AS " + KEY_WEEK_START,year,week*7);
  }
  public static String weekEndFromGroupSqlExpression(int year, int week) {
    return String.format(Locale.US, COUNT_FROM_WEEK_START_ZERO + " AS " + KEY_WEEK_END,year,week*7+6);
  }

  public static String[][] getTableDetails() {
    Cursor c = MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.DEBUG_SCHEMA_URI, null,null,null,null);
    return getTableDetails(c);
  }
  /**
   * @param c
   * @return
   */
  public static String[][] getTableDetails(Cursor c) {
    if (c == null) {
      return null;
    }
    String[][] result = new String[c.getCount()][2];
    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
      int pos = c.getPosition();
      result[pos][0] = c.getString(0);
      result[pos][1] = c.getString(1);
    }
    c.close();
    return result;
  }

  private static void cacheEventData() {
    String plannerCalendarId = MyApplication.PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    if (plannerCalendarId.equals("-1")) {
      return;
    }
    ContentValues eventValues = new ContentValues();
    ContentResolver cr = MyApplication.getInstance().getContentResolver();
    //remove old cache
    cr.delete(
        TransactionProvider.EVENT_CACHE_URI, null, null);

    Cursor planCursor = cr.query(Template.CONTENT_URI, new String[]{
            DatabaseConstants.KEY_PLANID},
        DatabaseConstants.KEY_PLANID + " IS NOT null", null, null);
    if (planCursor != null) {
      if (planCursor.moveToFirst()) {
        String[] projection = MyApplication.buildEventProjection();
        do {
          long planId = planCursor.getLong(0);
          Uri eventUri = ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI,
              planId);

          Cursor eventCursor = cr.query(eventUri, projection,
              CalendarContractCompat.Events.CALENDAR_ID + " = ?", new String[]{plannerCalendarId}, null);
          if (eventCursor != null) {
            if (eventCursor.moveToFirst()) {
              MyApplication.copyEventData(eventCursor, eventValues);
              cr.insert(TransactionProvider.EVENT_CACHE_URI, eventValues);
            }
            eventCursor.close();
          }
        } while (planCursor.moveToNext());
      }
      planCursor.close();
    }
  }
}
