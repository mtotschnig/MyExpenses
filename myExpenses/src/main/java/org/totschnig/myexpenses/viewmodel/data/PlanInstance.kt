package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import com.android.calendar.CalendarContractCompat
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.util.epochMillis2LocalDate

enum class PlanInstanceState {
    OPEN, APPLIED, CANCELLED
}

data class PlanInstance(val templateId: Long, val instanceId: Long?, val transactionId: Long?, val title: String, val date: Long, val color: Int, val amount: Money) {
    val localDate: LocalDate
    val state: PlanInstanceState
    init {
        localDate = epochMillis2LocalDate(date)
        state = if (instanceId == null) PlanInstanceState.OPEN else if (transactionId == null) PlanInstanceState.CANCELLED else PlanInstanceState.APPLIED
    }
    companion object {
        fun fromEventCursor(cursor: Cursor) = Template.getPlanInstance(
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID)),
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN)))
    }
}
