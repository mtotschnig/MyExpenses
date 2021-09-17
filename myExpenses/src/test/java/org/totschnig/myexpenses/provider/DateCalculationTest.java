package org.totschnig.myexpenses.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.totschnig.myexpenses.preference.PrefKey;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;

/**
 * This test can be run from the command line with different timezones
 * TZ=Etc/GMT+5 ./gradlew testAcraDebugUnitTest --tests org.totschnig.myexpenses.provider.DateCalculationTest
 */
@RunWith(RobolectricTestRunner.class)
public class DateCalculationTest {

  // Contains an SQLite database, used as test data
  private SQLiteDatabase mDb;

  private String TABLE = "test_dates";
  private String KEY_DATE = "date";
  private Random random;
  private Calendar calendar;

  @Before
  public void setUp() {
    final Context targetContext = RuntimeEnvironment.application;
    mDb = new MyDbHelper(targetContext).getWritableDatabase();
    ContentValues v = new ContentValues();
    for (int year = 2010; year < 2022; year++) {
      int month = 11, day = 26;
      while (true) {
        random = new Random();
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        v.put(KEY_DATE, new GregorianCalendar(year, month, day, hour, minute).getTime().getTime() / 1000);
        mDb.insertOrThrow(TABLE, null, v);
        day++;
        if (day == 32) {
          month = 0;
          day = 1;
        }
        if (day == 7) {
          break;
        }
      }
    }
  }

  @After
  public void cleanUp() {
    mDb.close();
    mDb = null;
  }

  @Test
  public void testDateCalculationsForWeekGroupsWithAllWeekDays() {
    final String timeZone = TimeZone.getDefault().getDisplayName();
    System.out.println(timeZone);
    for (int j = Calendar.SUNDAY; j <= Calendar.SATURDAY; j++) {
      doTheTest(timeZone, j);
    }
  }

  private void doTheTest(String timeZone, int configuredWeekStart) {
    calendar = Calendar.getInstance();
    PrefKey.GROUP_WEEK_STARTS.putString(String.valueOf(configuredWeekStart));
    DatabaseConstants.buildLocalized(Locale.getDefault());
    assertEquals(configuredWeekStart, DatabaseConstants.weekStartsOn);
    String[] projection = {
        DatabaseConstants.getYearOfWeekStart() + " AS year",
        DatabaseConstants.getWeek() + " AS week",
        DatabaseConstants.getWeekStart() + " AS week_start",
        DatabaseConstants.getWeekEnd() + " AS week_end",
        KEY_DATE
    };
    Cursor c = mDb.query(
        TABLE,
        projection,
        null, null, null, null, null);
    assertEquals(12 * 12, c.getCount());
    c.moveToFirst();
    while (!c.isAfterLast()) {
      int year = c.getInt(0);
      int week = c.getInt(1);
      long weekStartAsTimeStamp = c.getLong(2);
      int dayOfYearOfWeekStart = getDayOfYearFromTimestamp(weekStartAsTimeStamp);
      long weekEndAsTimeStamp = c.getLong(3);
      int dayOfYearOfWeekEnd = getDayOfYearFromTimestamp(weekEndAsTimeStamp);
      long unixTimeStamp = c.getLong(4);
      String date = SimpleDateFormat.getDateInstance().format(new Date(unixTimeStamp * 1000));
      String weekStartFromGroupSqlExpression = DbUtils.weekStartFromGroupSqlExpression(year, week);
      String weekEndFromGroupSqlExpression = DbUtils.weekEndFromGroupSqlExpression(year, week);
      Cursor check = mDb.query(
          TABLE,
          new String[]{
              weekStartFromGroupSqlExpression,
              weekEndFromGroupSqlExpression
          },
          null, null, null, null, null);
      check.moveToFirst();
      long weekStartFromGroupAsTimeStamp = check.getLong(0);
      int dayOfYearOfWeekStartFromGroup = getDayOfYearFromTimestamp(weekStartFromGroupAsTimeStamp);
      long weekEndFromGroupAsTimeStamp = check.getLong(1);
      int dayOfYearOfWeekEndFromGroup = getDayOfYearFromTimestamp(weekEndFromGroupAsTimeStamp);
      assertEquals(String.format(Locale.ROOT,
          "With timezone %s and week starts on %d, for date %s (%d) comparing weekStart %d did not match weekStart from group (%d,%d) %d",
          timeZone, configuredWeekStart, date, unixTimeStamp, weekStartAsTimeStamp, year, week, weekStartFromGroupAsTimeStamp),
          dayOfYearOfWeekStart, dayOfYearOfWeekStartFromGroup);
      assertEquals(String.format(Locale.ROOT,
          "With timezone %s and week starts on %d, for date %s (%d) comparing weekEnd %d did not match weekEnd from group %d",
          timeZone, configuredWeekStart, date, unixTimeStamp, weekEndAsTimeStamp, weekEndFromGroupAsTimeStamp),
          dayOfYearOfWeekEnd, dayOfYearOfWeekEndFromGroup);
      check.close();
      c.moveToNext();
    }
    c.close();
  }

  private int getDayOfYearFromTimestamp(long timestamp) {
    calendar.setTimeInMillis(timestamp * 1000);
    return calendar.get(Calendar.DAY_OF_YEAR);
  }

  private class MyDbHelper extends SQLiteOpenHelper {

    MyDbHelper(Context context) {
      super(context, "dateCalculationTest", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE + " (" + KEY_DATE + " DATETIME not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }
}
