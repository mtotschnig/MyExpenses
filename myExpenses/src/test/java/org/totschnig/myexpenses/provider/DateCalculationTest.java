package org.totschnig.myexpenses.provider;

import static junit.framework.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * This test can be run from the command line with different timezones
 * TZ=Etc/GMT+5 ./gradlew testAcraDebugUnitTest --tests org.totschnig.myexpenses.provider.DateCalculationTest
 */
@RunWith(RobolectricTestRunner.class)
public class DateCalculationTest {

  // Contains an SQLite database, used as test data
  private SQLiteDatabase mDb;

  private final String TABLE = "test_dates";
  private final String KEY_DATE = "date";

  @Before
  public void setUp() {
    final Context targetContext = RuntimeEnvironment.getApplication();
    WorkManagerTestInitHelper.initializeTestWorkManager(targetContext);
    mDb = new MyDbHelper(targetContext).getWritableDatabase();
    ContentValues v = new ContentValues();
    for (int year = 2020; year < 2032; year++) {
      int month = 11, day = 26;
      do {
        Random random = new Random();
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        v.put(KEY_DATE, new GregorianCalendar(year, month, day, hour, minute).getTime().getTime() / 1000);
        mDb.insertOrThrow(TABLE, null, v);
        day++;
        if (day == 32) {
          month = 0;
          day = 1;
        }
      } while (day != 7);
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
    ((MyApplication) ApplicationProvider.getApplicationContext()).getAppComponent().prefHandler()
            .putString(PrefKey.GROUP_WEEK_STARTS, String.valueOf(configuredWeekStart));
    DatabaseConstants.buildLocalized(Locale.getDefault(), (MyApplication) RuntimeEnvironment.getApplication());
    assertEquals(configuredWeekStart, DatabaseConstants.weekStartsOn);
    String[] projection = {
        DatabaseConstants.getYearOfWeekStart() + " AS year",
        DatabaseConstants.getWeek() + " AS week",
        DatabaseConstants.getWeekStart() + " AS week_start",
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
      String weekStart = c.getString(2);
      int dayOfYearOfWeekStart = getDayOfYearFromDate(weekStart);
      long unixTimeStamp = c.getLong(3);
      String date = SimpleDateFormat.getDateInstance().format(new Date(unixTimeStamp * 1000));
      String weekStartFromGroupSqlExpression = DbUtils.weekStartFromGroupSqlExpression(year, week);
      Cursor check = mDb.query(
          TABLE,
          new String[]{
              weekStartFromGroupSqlExpression
          },
          null, null, null, null, null);
      check.moveToFirst();
      String weekStartFromGroup = check.getString(0);
      int dayOfYearOfWeekStartFromGroup = getDayOfYearFromDate(weekStartFromGroup);
      assertEquals(String.format(Locale.ROOT,
          "With timezone %s and week starts on %d, for date %s (%d) comparing weekStart %s did not match weekStart from group (%d,%d) %s",
          timeZone, configuredWeekStart, date, unixTimeStamp, weekStart, year, week, weekStartFromGroup),
          dayOfYearOfWeekStart, dayOfYearOfWeekStartFromGroup);
      check.close();
      c.moveToNext();
    }
    c.close();
  }

  private int getDayOfYearFromDate(String date) {
    return LocalDate.parse(date).getDayOfYear();
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
