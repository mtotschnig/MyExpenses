package org.totschnig.myexpenses.test.provider

import android.Manifest
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.test.mock.MockContentProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.calendar.CalendarContractCompat
import org.junit.runner.RunWith
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseDbTest
import androidx.test.rule.provider.ProviderTestRule
import org.junit.Rule



@RunWith(AndroidJUnit4::class)
class PlanInfoTest {
    @Rule
    var mProviderRule = ProviderTestRule.Builder(TransactionProvider::class.java, TransactionProvider.AUTHORITY)
            .addProvider()
            .build()
    @Rule
    var grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)

    private val TRANSACTIONS: Array<TemplateInfo> by lazy {
        val testAccountId = mDb.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null,
                AccountInfo("Test account", AccountType.CASH, 0).getContentValues())
        val payeeId = mDb.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, PayeeInfo("N.N").contentValues)
        arrayOf(TemplateInfo(testAccountId, "Template 1", 100, payeeId, 1))
    }

    override fun setUp() {
        super.setUp()
        val calendarProvider = CalendarProvider()
        val projection = arrayOf(CalendarContractCompat.Events._ID, CalendarContractCompat.Events.DTSTART, CalendarContractCompat.Events.RRULE)
        val matrix = MatrixCursor(projection)
        matrix.addRow(arrayOf(1,0,"daily"))
        calendarProvider.addQueryResult(matrix)
        mMockResolver.addProvider(CalendarContractCompat.AUTHORITY, calendarProvider)
        insertData()
    }

    private fun insertData() {
        for (transactionInfo in TRANSACTIONS) {

            mDb.insertOrThrow(
                    DatabaseConstants.TABLE_TEMPLATES,
                    null,
                    transactionInfo.contentValues
            )
        }

    }

    fun testTemplatesUri() {
        val cursor = mMockResolver.query(
                TransactionProvider.TEMPLATES_URI.buildUpon()
                        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "1")
                        .build(),
                null, null, null, null)!!

        assertEquals(TRANSACTIONS.size, cursor.count)
        while (cursor.moveToNext()) {
            assertNotNull(cursor.getString(cursor.getColumnIndex(DatabaseConstants.KEY_PLAN_INFO)))
        }
        cursor.close()
    }
}

class CalendarProvider: MockContentProvider() {
    private var queryResult: Cursor? = null

    fun addQueryResult(expectedResult: Cursor) {
        this.queryResult = expectedResult
    }

    override fun query(uri: Uri, projection: Array<String>, selection: String, selectionArgs: Array<String>, sortOrder: String): Cursor? {
        return this.queryResult
    }
}