package org.totschnig.myexpenses.test.provider;

import android.Manifest;
import android.app.AlarmManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;
import android.util.Log;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;

public class DateCalculationTest  {

  // Contains an SQLite database, used as test data
  private SQLiteDatabase mDb;

  String TABLE = "test_dates";
  String KEY_DATE = "date";
  private Random random;
  private Calendar calendar;

  @Rule
  public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_CALENDAR, Manifest.permission.SET_TIME_ZONE);
  private AlarmManager alarmManager;
  private String originalTimeZone;


  /*
   * Sets up the test environment before each test method. Creates a mock content resolver,
   * gets the provider under test, and creates a new database for the provider.
   */
  @Before
  public void setUp() throws Exception {
    originalTimeZone = TimeZone.getDefault().getID();
    //On O SET_TIME_ZONE has become restricted
    Assume.assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.O);
    mDb = new MyDbHelper(InstrumentationRegistry.getTargetContext()).getWritableDatabase();
    alarmManager = (AlarmManager) InstrumentationRegistry.getTargetContext().getSystemService(Context.ALARM_SERVICE);
  }

  @After
  public void after() {
    alarmManager.setTimeZone(originalTimeZone);
  }

  @Test
  public void testDateCalculationsForWeekGroupsWithAllWeekDaysForAllTimeZoneOffsets() throws InterruptedException {
    int[] timezones = {-11, -7, -3, 0, 4, 8, 12};
    for (int i: timezones) {
      Log.d("DEBUG", "now setting timezone with offset " + i);
      int rawOffset = i * 60 * 60 * 1000;
      String timeZone = TimeZone.getAvailableIDs(rawOffset)[0];
      alarmManager.setTimeZone(timeZone);
      //TODO do not know how to wait for effect of time zone change
      Thread.sleep(200);
      assertEquals(TimeZone.getDefault().getRawOffset(), rawOffset);
      for (int j = Calendar.SUNDAY; j <= Calendar.SATURDAY; j++) {
        doTheTest(timeZone, j);
      }
    }
  }

  private void doTheTest(String timeZone, int configuredWeekStart) {
    calendar = Calendar.getInstance();
    PrefKey.GROUP_WEEK_STARTS.putString(String.valueOf(configuredWeekStart));
    DatabaseConstants.buildLocalized(Locale.getDefault());
    assertEquals(configuredWeekStart, DatabaseConstants.weekStartsOn);
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
          "With timezone %s and week starts on %d, for date %s (%d) comparing weekStart %d did not match weekStart from group %d",
          timeZone, configuredWeekStart, date, unixTimeStamp, weekStartAsTimeStamp, weekStartFromGroupAsTimeStamp),
          dayOfYearOfWeekStart, dayOfYearOfWeekStartFromGroup);
      assertEquals(String.format(Locale.ROOT,
          "With timezone %s and week starts on %d, for date %s (%d) comparing weekEnd %d did not match weekEnd from group %d",
          timeZone, configuredWeekStart, date, unixTimeStamp, weekEndAsTimeStamp, weekEndFromGroupAsTimeStamp),
          dayOfYearOfWeekEnd, dayOfYearOfWeekEndFromGroup);
      check.close();
      c.moveToNext();
    }
    c.close();
    mDb.execSQL("DELETE FROM " + TABLE);
  }

  private int getDayOfYearFromTimestamp(long timestamp) {
    calendar.setTimeInMillis(timestamp * 1000);
    return calendar.get(Calendar.DAY_OF_YEAR);
  }

  private class MyDbHelper extends SQLiteOpenHelper {

    public MyDbHelper(Context context) {
      super(context, "datecalculationtest", null, 1);
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
