package org.totschnig.myexpenses.test.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.test.mock.MockContentProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.calendar.CalendarContractCompat
import org.assertj.core.api.Assertions.assertThat
import org.threeten.bp.LocalDateTime
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseDbTest
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PlanInfoTest : BaseDbTest() {
    private val dailyPlan = Plan(1, "FREQ=DAILY")
    private val weeklyPlan = Plan(2, "FREQ=WEEKLY")
    private val monthlyPlan = Plan(3, "FREQ=MONTHLY")
    private val templateInfos: Array<TemplateInfo> by lazy {
        val testAccountId = mDb.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null,
                AccountInfo("Test account", AccountType.CASH, 0).contentValues)
        val payeeId = mDb.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, PayeeInfo("N.N").contentValues)
        arrayOf(TemplateInfo(testAccountId, "Template monthly", 100, payeeId, monthlyPlan.id),
                TemplateInfo(testAccountId, "Template weekly", 100, payeeId, weeklyPlan.id),
                TemplateInfo(testAccountId, "Template daily", 100, payeeId, dailyPlan.id))
    }

    override fun setUp() {
        super.setUp()
        //grantCalendarPermission()
        EventProvider().also {
            MatrixCursor(arrayOf(CalendarContractCompat.Events._ID, CalendarContractCompat.Events.RRULE, CalendarContractCompat.Events.DTSTART)).apply {
                addRow(dailyPlan.toMatrixRow())
                addRow(weeklyPlan.toMatrixRow())
                addRow(monthlyPlan.toMatrixRow())
                it.addEventResult(this)
            }
            mockContentResolver.addProvider(CalendarContractCompat.AUTHORITY, it)
        }
    }

    //unfortunately does not work
    fun grantCalendarPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            with(getInstrumentation()) {
                uiAutomation.apply {
                    grantRuntimePermission(targetContext.packageName, "android.permission.READ_CALENDAR")
                    grantRuntimePermission(targetContext.packageName, "android.permission.WRITE_CALENDAR")
                }
            }
        }
    }

    private fun insertData() {
        for (transactionInfo in templateInfos) {

            mDb.insertOrThrow(
                    DatabaseConstants.TABLE_TEMPLATES,
                    null,
                    transactionInfo.contentValues
            )
        }
    }

    fun testPlanInfo() {
        insertData()
        val cursor = mockContentResolver.query(
                TransactionProvider.TEMPLATES_URI.buildUpon()
                        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "2")
                        .build(),
                null, null, null, null)!!

        assertEquals(templateInfos.size, cursor.count)
        cursor.moveToFirst()
        do {
            val planId = cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_PLANID))
            val planInfo = cursor.getString(cursor.getColumnIndex(DatabaseConstants.KEY_PLAN_INFO))
            with(getInstrumentation().targetContext) {
                assertThat(planInfo).contains(when (planId) {
                            dailyPlan.id -> getString(R.string.daily_plain)
                            weeklyPlan.id -> getString(R.string.weekly_plain)
                            monthlyPlan.id -> getString(R.string.monthly_on_day, LocalDateTime.now().dayOfMonth)
                            else -> throw IllegalArgumentException()
                        })
            }

        } while (cursor.moveToNext())
        cursor.close()
    }

    /*
    We test here if PlanInfoCursorWrapper is searching for next instance in increasing intervals
     */
    fun testGetNextInstanceQueries() {
        object : MockContentProvider() {
            private var lastEnd: Long? = null

            //we do not find any instances, we just test that queries arrive in correct order
            override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
                val startMilliseconds = uri.pathSegments[2].toLong()
                val endMilliseconds = uri.pathSegments[3].toLong()
                Timber.d("from %d to %d", startMilliseconds, endMilliseconds)
                lastEnd?.let {
                    assertThat(startMilliseconds).isEqualTo(it)
                }
                assertThat(endMilliseconds).isGreaterThan(startMilliseconds)
                lastEnd = endMilliseconds
                return null
            }
        }.also {
            mockContentResolver.addProvider(CalendarProviderProxy.AUTHORITY, it)
        }
        //we only need/must add one row
        mDb.insertOrThrow(
                DatabaseConstants.TABLE_TEMPLATES,
                null,
                templateInfos[0].contentValues
        )
        mockContentResolver.query(
                TransactionProvider.TEMPLATES_URI.buildUpon()
                        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "2")
                        .build(),
                null, null, null, null)!!.apply {
            assertNotNull(this)
            close()
        }
    }

    fun testGetInstancesSorting() {
        object : MockContentProvider() {

            override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
                val planId = selectionArgs!![0].toLong()
                val nextInstance = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(
                        when (planId) {
                            dailyPlan.id -> 1
                            weeklyPlan.id -> 7
                            monthlyPlan.id -> 30
                            else -> 0
                        })
                return MatrixCursor(arrayOf(CalendarContractCompat.Instances.BEGIN)).apply {
                    addRow(arrayOf(nextInstance))
                }
            }
        }.also {
            mockContentResolver.addProvider(CalendarProviderProxy.AUTHORITY, it)
        }
        insertData()
        val cursor = mockContentResolver.query(
                TransactionProvider.TEMPLATES_URI.buildUpon()
                        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "2")
                        .build(),
                null, null, null, null)!!
        cursor.moveToFirst()
        assertThat(cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_PLANID))).isEqualTo(dailyPlan.id)
        cursor.moveToNext()
        assertThat(cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_PLANID))).isEqualTo(weeklyPlan.id)
        cursor.moveToNext()
        assertThat(cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_PLANID))).isEqualTo(monthlyPlan.id)
        cursor.close()
    }
}

class EventProvider : MockContentProvider() {
    private var eventResult: Cursor? = null

    fun addEventResult(expectedResult: Cursor) {
        this.eventResult = expectedResult
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return this.eventResult
    }
}

data class Plan(val id: Long, val rrule: String, val date: Long = System.currentTimeMillis()) {
    fun toMatrixRow() = arrayOf(id, rrule, date)
}

