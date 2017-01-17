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

import android.Manifest;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.service.DailyAutoBackupScheduler;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getCountFromWeekStartZero;

public class DbUtils {

  private DbUtils() {
  }

  public static Result backup(File backupDir) {
    cacheEventData();
    ContentResolver resolver = MyApplication.getInstance().getContentResolver();
    ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
    TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
    Result result = provider.backup(backupDir);
    client.release();
    return result;
  }

  public static boolean restore(File backupFile) {
    boolean result = false;
    MyApplication app = MyApplication.getInstance();
    try {
      DailyAutoBackupScheduler.cancelAutoBackup(app);
      PlanExecutor.cancelPlans(app);
      Account.clear();
      PaymentMethod.clear();

      if (backupFile.exists()) {
        ContentResolver resolver = app.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
        TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
        result = provider.restore(backupFile);
        client.release();
      }
    } catch (Exception e) {
      AcraHelper.report(e);
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
    return getLongOrNull(c, c.getColumnIndexOrThrow(field));
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
    return String.format(Locale.US, getCountFromWeekStartZero() + " AS " + KEY_WEEK_START,year,week*7);
  }
  public static String weekEndFromGroupSqlExpression(int year, int week) {
    return String.format(Locale.US, getCountFromWeekStartZero() + " AS " + KEY_WEEK_END,year,week*7+6);
  }

  public static Map<String, String> getSchemaDetails() {
    Cursor c = MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.DEBUG_SCHEMA_URI, null,null,null,null);
    return getTableDetails(c);
  }

  /**
   * @param c
   * @return
   */
  public static Map<String, String> getTableDetails(Cursor c) {
    HashMap<String, String> data = new HashMap<>();
    if (c == null) {
      return data;
    }
    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
      data.put(c.getString(0), c.getString(1));
    }
    c.close();
    return data;
  }

  private static void cacheEventData() {
    if (ContextCompat.checkSelfPermission(MyApplication.getInstance(),
        Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    String plannerCalendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
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
