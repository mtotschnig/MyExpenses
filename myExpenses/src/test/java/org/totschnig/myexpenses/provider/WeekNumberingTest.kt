package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey

@RunWith(RobolectricTestRunner::class)
class WeekNumberingTest {

    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<MyApplication>()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        db = TestDbHelper(app).writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun configureLocaleAndWeekStart(locale: Locale, configuredWeekStart: Int) {
        val app = ApplicationProvider.getApplicationContext<MyApplication>()
        app.appComponent.prefHandler()
            .putString(PrefKey.GROUP_WEEK_STARTS, configuredWeekStart.toString())
        DatabaseConstants.buildLocalized(locale, app)
    }

    private fun queryWeekInfo(date: LocalDate): Triple<Int, Int, String> {
        val epochSecond = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val values = ContentValues().apply {
            put("date", epochSecond)
        }
        db.delete("test_dates", null, null)
        db.insertOrThrow("test_dates", null, values)

        val projection = arrayOf(
            "${DatabaseConstants.yearOfWeekStart} AS year",
            "${DatabaseConstants.week} AS week",
            "${DatabaseConstants.weekStart} AS week_start"
        )
        return db.query("test_dates", projection, null, null, null, null, null).use { cursor ->
            cursor.moveToFirst()
            Triple(cursor.getInt(0), cursor.getInt(1), cursor.getString(2))
        }
    }

    private fun queryMaxWeeks(year: Int): Int {
        val maxExpr = DbUtils.maximumWeekExpression(year)
        return db.rawQuery("SELECT $maxExpr", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun assertRoundTrip(year: Int, week: Int, expectedWeekStart: String) {
        val sqlExpr = DbUtils.weekStartFromGroupSqlExpression(year, week)
        db.query("test_dates", arrayOf(sqlExpr), null, null, null, null, null).use { cursor ->
            cursor.moveToFirst()
            val actualWeekStart = cursor.getString(0)
            assertEquals(
                "Round-trip week start for ($year, W$week) mismatch",
                expectedWeekStart,
                actualWeekStart
            )
        }
    }

    @Test
    fun testJan1_2025_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // 2025-01-01 is Wednesday.
        // ISO 8601 (Germany): Week Dec 30, 2024 - Jan 5, 2025 is Year 2025, Week 1.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2025, 1, 1))
        assertEquals(2025, year)
        assertEquals(1, week)
        assertEquals("2024-12-30", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testDec30_2024_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // 2024-12-30 is Monday.
        // ISO 8601 (Germany): Year 2025, Week 1.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2024, 12, 30))
        assertEquals(2025, year)
        assertEquals(1, week)
        assertEquals("2024-12-30", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testJan1_2021_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // 2021-01-01 is Friday.
        // ISO 8601 (Germany): Dec 28, 2020 - Jan 3, 2021 belongs to Year 2020, Week 53.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2021, 1, 1))
        assertEquals(2020, year)
        assertEquals(53, week)
        assertEquals("2020-12-28", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testJan1_2024_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // 2024-01-01 is Monday.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2024, 1, 1))
        assertEquals(2024, year)
        assertEquals(1, week)
        assertEquals("2024-01-01", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testJan1_2025_US_SundayStart() {
        configureLocaleAndWeekStart(Locale.US, Calendar.SUNDAY)
        // 2025-01-01 is Wednesday.
        // US convention (Minimal 1 day): Week containing Jan 1 is Week 1 of 2025.
        // Week starts Sun Dec 29, 2024.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2025, 1, 1))
        assertEquals(2025, year)
        assertEquals(1, week)
        assertEquals("2024-12-29", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testDec28_2024_US_SundayStart() {
        configureLocaleAndWeekStart(Locale.US, Calendar.SUNDAY)
        // 2024-12-28 is Saturday.
        // US convention: Belongs to Year 2024, Week 52.
        val (year, week, weekStart) = queryWeekInfo(LocalDate.of(2024, 12, 28))
        assertEquals(2024, year)
        assertEquals(52, week)
        assertEquals("2024-12-22", weekStart)
        assertRoundTrip(year, week, weekStart)
    }

    @Test
    fun testMaxWeeks_2024_US_SundayStart() {
        configureLocaleAndWeekStart(Locale.US, Calendar.SUNDAY)
        // Year 2024 under US Sunday start has 52 weeks (Dec 29, 2024 is already 2025 W1).
        // Max weeks for 2024 must be 52, not 53!
        val maxWeeks = queryMaxWeeks(2024)
        assertEquals("Max weeks for 2024 under US Sunday start should be 52", 52, maxWeeks)
    }

    @Test
    fun testMaxWeeks_2028_US_SundayStart() {
        configureLocaleAndWeekStart(Locale.US, Calendar.SUNDAY)
        // Year 2028 under US Sunday start has 53 weeks.
        val maxWeeks = queryMaxWeeks(2028)
        assertEquals("Max weeks for 2028 under US Sunday start should be 53", 53, maxWeeks)
    }

    @Test
    fun testMaxWeeks_2020_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // Year 2020 under ISO 8601 has 53 weeks.
        val maxWeeks = queryMaxWeeks(2020)
        assertEquals("Max weeks for 2020 under ISO 8601 should be 53", 53, maxWeeks)
    }

    @Test
    fun testMaxWeeks_2024_ISO8601() {
        configureLocaleAndWeekStart(Locale.GERMANY, Calendar.MONDAY)
        // Year 2024 under ISO 8601 has 52 weeks.
        val maxWeeks = queryMaxWeeks(2024)
        assertEquals("Max weeks for 2024 under ISO 8601 should be 52", 52, maxWeeks)
    }

    private class TestDbHelper(context: android.content.Context) :
        SQLiteOpenHelper(context, "weekNumberingTest", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE test_dates (date DATETIME not null)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // noop
        }
    }
}
