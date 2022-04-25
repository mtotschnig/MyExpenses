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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getCountFromWeekStartZero;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeekMax;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.service.DailyScheduler;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DbUtils {

  private DbUtils() {
  }

  public static boolean restore(File backupFile) {
    boolean result = false;
    MyApplication app = MyApplication.getInstance();
    try {
      DailyScheduler.cancelAutoBackup(app);
      DailyScheduler.cancelPlans(app);
      PaymentMethod.clear();

      if (backupFile.exists()) {
        ContentResolver resolver = app.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
        TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
        result = provider.restore(backupFile);
        client.release();
      }
    } catch (Exception e) {
      CrashHandler.report(e);
    }
    DailyScheduler.updatePlannerAlarms(app,false, true);
    DailyScheduler.updateAutoBackupAlarms(app);
    return result;
  }

  //TODO: create generic function
  public static String[] getStringArrayFromCursor(Cursor c, String field) {
    String[] result = new String[c.getCount()];
    if (c.moveToFirst()) {
      for (int i = 0; i < c.getCount(); i++) {
        result[i] = c.getString(c.getColumnIndexOrThrow(field));
        c.moveToNext();
      }
    }
    return result;
  }

  public static Long[] getLongArrayFromCursor(Cursor c, String field) {
    Long[] result = new Long[c.getCount()];
    if (c.moveToFirst()) {
      for (int i = 0; i < c.getCount(); i++) {
        result[i] = c.getLong(c.getColumnIndexOrThrow(field));
        c.moveToNext();
      }
    }
    return result;
  }

  /**
   * @return Long that is null if field is null in db
   */
  @Nullable
  public static Long getLongOrNull(Cursor c, String field) {
    return getLongOrNull(c, c.getColumnIndexOrThrow(field));
  }

  @Nullable
  public static Long getLongOrNull(Cursor c, int columnIndex) {
    if (c.isNull(columnIndex))
      return null;
    return c.getLong(columnIndex);
  }

  /**
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

  public static String weekStartFromGroupSqlExpression(int year, int week) {
    return String.format(Locale.US, getCountFromWeekStartZero() + " AS " + KEY_WEEK_START, year, week * 7);
  }

  public static String weekEndFromGroupSqlExpression(int year, int week) {
    return String.format(Locale.US, getCountFromWeekStartZero() + " AS " + KEY_WEEK_END, year, week * 7 + 6);
  }

  public static String maximumWeekExpression(int year) {
    return String.format(Locale.US, getWeekMax(), year);
  }

  public static Map<String, String> getSchemaDetails() {
    Cursor c = MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.DEBUG_SCHEMA_URI, null, null, null, null);
    return getTableDetails(c);
  }

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

  @VisibleForTesting
  public static String fqcn(String table, String column) {
    return String.format(Locale.ROOT, "%s.%s", table, column);
  }

  public static Uri storeSetting(ContentResolver contentResolver, String key, String value) {
    ContentValues values = new ContentValues(2);
    values.put(KEY_KEY, key);
    values.put(KEY_VALUE, value);
    return contentResolver.insert(TransactionProvider.SETTINGS_URI, values);
  }

  @Nullable
  public static String loadSetting(ContentResolver contentResolver, String key) {
    String result = null;
    Cursor cursor = contentResolver.query(TransactionProvider.SETTINGS_URI, new String[]{KEY_VALUE},
        KEY_KEY + " = ?", new String[]{key}, null);
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        result = cursor.getString(0);
      }
      cursor.close();
    }
    return result;
  }

  static int suggestNewCategoryColor(SQLiteDatabase db) {
    String[] projection = new String[]{
        "color",
        "(select count(*) from categories where parent_id is null and color=t.color) as count"
    };
    Cursor cursor = db.query(ColorUtils.MAIN_COLORS_AS_TABLE, projection, null, null, null, null, "count ASC", "1");
    int result = 0;
    if (cursor != null) {
      cursor.moveToFirst();
      result = cursor.getInt(0);
      cursor.close();
    }
    return result;
  }

  @Nullable
  private static String[] getArraySave(Resources resources, int resId) {
    try {
      return resources.getStringArray(resId);
    } catch (Resources.NotFoundException e) {//if resource does exist in an alternate locale, but not in the default one
      return null;
    }
  }

  @Nullable
  private static String getStringSafe(Resources resources, int resId) {
    try {
      return resources.getString(resId);
    } catch (Resources.NotFoundException e) {//if resource does exist in an alternate locale, but not in the default one
      return null;
    }
  }
}
