package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import com.android.calendar.CalendarContractCompat
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import java.time.LocalDate

enum class PlanInstanceState {
    OPEN, APPLIED, CANCELLED
}

data class PlanInstance(val templateId: Long, val transactionId: Long?, val title: String, val date: Long, val color: Int, val amount: Money, val state: PlanInstanceState, val sealed: Boolean) {
    constructor(templateId: Long, instanceId: Long?, transactionId: Long?, title: String, date: Long, color: Int, amount: Money, sealed: Boolean) :
            this(templateId, transactionId, title, date, color, amount,
                    if (instanceId == null) PlanInstanceState.OPEN else
                        if (transactionId == null) PlanInstanceState.CANCELLED else PlanInstanceState.APPLIED, sealed)
    val localDate: LocalDate

    val instanceId: Long
        get() = CalendarProviderProxy.calculateId(date)

    init {
        localDate = epochMillis2LocalDate(date)
    }
    companion object {
        fun fromEventCursor(cursor: Cursor) = Template.getPlanInstance(
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID)),
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN)))
    }
}

data class PlanInstanceUpdate(val templateId: Long, val instanceId: Long, val newState: PlanInstanceState, val transactionId: Long?, val amount: Long?)
