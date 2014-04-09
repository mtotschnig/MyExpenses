package org.totschnig.myexpenses.test.provider;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ProviderTestCase2;
import android.util.Log;

public class DateCalculationTest extends ProviderTestCase2<TransactionProvider> {

  // Contains an SQLite database, used as test data
  private SQLiteDatabase mDb;
  
  DateFormat dateformat = Utils.localizedYearlessDateFormat();
  String TABLE = "test_dates";
  String KEY_DATE = "date";
  public DateCalculationTest() {
    super(TransactionProvider.class,TransactionProvider.AUTHORITY);
  }
  
  /*
   * Sets up the test environment before each test method. Creates a mock content resolver,
   * gets the provider under test, and creates a new database for the provider.
   */
  @Override
  protected void setUp() throws Exception {
      // Calls the base class implementation of this method.
      super.setUp();

      /*
       * Gets a handle to the database underlying the provider. Gets the provider instance
       * created in super.setUp(), gets the DatabaseOpenHelper for the provider, and gets
       * a database object from the helper.
       */
      mDb = getProvider().getOpenHelperForTest().getWritableDatabase();
      mDb.execSQL("CREATE TABLE " + TABLE + " (" + KEY_DATE + " DATETIME not null)");
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    mDb.execSQL("DROP TABLE " + TABLE);
  }

  public void testDateCalculationsForWeekGroupsWeekStartsOnMonday() {
    DatabaseConstants.buildLocalized(new Locale("de","DE"));
    assertEquals(Calendar.MONDAY,DatabaseConstants.weekStartsOn);
    doTheTest();
  }
  
  public void testDateCalculationsForWeekGroupsWeekStartsOnSunday() {
    DatabaseConstants.buildLocalized(new Locale("en","US"));
    assertEquals(Calendar.SUNDAY,DatabaseConstants.weekStartsOn);
    doTheTest();
  }
  
  public void testDateCalculationsForWeekGroupsWeekStartsOnSaturday() {
    DatabaseConstants.buildLocalized(new Locale("ar","SA"));
    assertEquals(Calendar.SATURDAY,DatabaseConstants.weekStartsOn);
    doTheTest();
  }
  
  private void doTheTest() {
    Log.i("DEBUG",DatabaseConstants.WEEK_END);
    ContentValues v = new ContentValues();
    for (int year = 2010; year < 2022; year++) {
      int month = 12, day = 26;
      while (true) {
        v.put(KEY_DATE, new GregorianCalendar(year, month, day).getTime().getTime()/1000);
        mDb.insertOrThrow(TABLE,null,v);
        day++;
        if (day==32) {
          month=1;
          day=1;
        }
        if (day==7) {
          break;
        }
      }
    }
    Cursor c = mDb.query(
        TABLE,
        new String[] {
            DatabaseConstants.YEAR_OF_WEEK_START + " AS year",
            DatabaseConstants.WEEK + " AS week",
            DatabaseConstants.WEEK_START + " AS week_start",
            DatabaseConstants.WEEK_END + " AS week_end",},
        null, null, null, null, null);
    assertEquals(12*12,c.getCount());
    c.moveToFirst();
    while (c.isAfterLast() == false) {
      int year = c.getInt(0);
      int week = c.getInt(1);
      Cursor check = mDb.query(
          TABLE,
          new String[]{
              DbUtils.weekStartFromGroupSqlExpression(year,week),
              DbUtils.weekEndFromGroupSqlExpression(year,week)
          },
          null, null, null, null, null);
      check.moveToFirst();
      assertEquals(
          Utils.convDateTime(c.getString(2),dateformat),
          Utils.convDateTime(check.getString(0),dateformat));
      assertEquals(
          Utils.convDateTime(c.getString(3),dateformat),
          Utils.convDateTime(check.getString(1),dateformat));
      check.close();
      c.moveToNext();
    }
    c.close();
  }
}
