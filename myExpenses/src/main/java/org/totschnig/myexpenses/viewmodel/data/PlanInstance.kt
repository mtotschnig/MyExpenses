package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import com.android.calendar.CalendarContractCompat
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.Template

enum class PlanInstanceState {
    open, applied, cancelled
}

data class PlanInstance(val title: String, val date: LocalDate, val state: PlanInstanceState) {
    companion object {
        fun fromEventCursor(cursor: Cursor) = Template.getPlanInstance(
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID)),
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN)))
    }
}
