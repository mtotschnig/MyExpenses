package org.totschnig.myexpenses.test.provider

import android.database.MatrixCursor
import android.provider.CalendarContract
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.testutils.BaseDbTest

abstract class BaseTemplateTest : BaseDbTest() {

    fun insertData() {
        for (transactionInfo in templateInfos) {

            mDb.insert(
                DatabaseConstants.TABLE_TEMPLATES,
                transactionInfo.contentValues
            )
        }
    }

    val dailyPlan = Plan(1, "FREQ=DAILY")
    val weeklyPlan = Plan(2, "FREQ=WEEKLY")
    val monthlyPlan = Plan(3, "FREQ=MONTHLY")
    val testAccountId by lazy {
        mDb.insert(
        DatabaseConstants.TABLE_ACCOUNTS,
        AccountInfo("Test account", AccountType.CASH, 0).contentValues)
    }
    val templateInfos: Array<TemplateInfo> by lazy {
        val payeeId = mDb.insert(DatabaseConstants.TABLE_PAYEES, PayeeInfo("N.N").contentValues)
        arrayOf(TemplateInfo(testAccountId, "Template monthly", 150, payeeId, monthlyPlan.id),
            TemplateInfo(testAccountId, "Template weekly", 200, payeeId, weeklyPlan.id),
            TemplateInfo(testAccountId, "Template daily", 100, payeeId, dailyPlan.id))
    }


    @Deprecated("Deprecated in Java")
    override fun setUp() {
        super.setUp()
        EventProvider().also {
            MatrixCursor(arrayOf(CalendarContract.Events._ID, CalendarContract.Events.RRULE, CalendarContract.Events.DTSTART)).apply {
                addRow(dailyPlan.toMatrixRow())
                addRow(weeklyPlan.toMatrixRow())
                addRow(monthlyPlan.toMatrixRow())
                it.addEventResult(this)
            }
            mockContentResolver.addProvider(CalendarContract.AUTHORITY, it)
        }
    }
}