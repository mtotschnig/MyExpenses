package org.totschnig.myexpenses.testutils

import android.database.MatrixCursor
import android.provider.CalendarContract
import org.totschnig.myexpenses.provider.AccountInfo
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.PayeeInfo
import org.totschnig.myexpenses.provider.TemplateInfo
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.test.provider.EventProvider
import org.totschnig.myexpenses.test.provider.Plan


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
            AccountInfo("Test account", cashAccount.id, 0).contentValues
        )
    }
    val templateInfos: Array<TemplateInfo> by lazy {
        val payeeId = mDb.insert(DatabaseConstants.TABLE_PAYEES, PayeeInfo("N.N").contentValues)
        arrayOf(
            TemplateInfo(
                accountId = testAccountId,
                title = "Template monthly",
                amount = 150,
                payeeId = payeeId,
                planId = monthlyPlan.id
            ),
            TemplateInfo(
                accountId = testAccountId,
                title = "Template weekly",
                amount = 200,
                payeeId = payeeId,
                planId = weeklyPlan.id
            ),
            TemplateInfo(
                accountId = testAccountId,
                title = "Template daily",
                amount = 100,
                payeeId = payeeId,
                planId = dailyPlan.id
            )
        )
    }


    @Deprecated("Deprecated in Java")
    override fun setUp() {
        super.setUp()
        EventProvider().also {
            MatrixCursor(
                arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.RRULE,
                    CalendarContract.Events.DTSTART
                )
            ).apply {
                addRow(dailyPlan.toMatrixRow())
                addRow(weeklyPlan.toMatrixRow())
                addRow(monthlyPlan.toMatrixRow())
                it.addEventResult(this)
            }
            mockContentResolver.addProvider(CalendarContract.AUTHORITY, it)
        }
    }
}