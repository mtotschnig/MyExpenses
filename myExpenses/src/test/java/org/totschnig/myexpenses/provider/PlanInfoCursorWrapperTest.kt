package org.totschnig.myexpenses.provider

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_INFO

@RunWith(RobolectricTestRunner::class)
class PlanInfoCursorWrapperTest {
    @Test
    fun testGetColumnNameForPlanInfo() {
        val resolver = ApplicationProvider.getApplicationContext<MyApplication>().contentResolver
        val cursor = resolver.query(TransactionProvider.TEMPLATES_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "2")
                .build(), null, null, null, null)!!
        val planInfoColumnIndex = cursor.getColumnIndex(KEY_PLAN_INFO)
        Assertions.assertThat(cursor.getColumnName(planInfoColumnIndex)).isEqualTo(KEY_PLAN_INFO)
    }
}