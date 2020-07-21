package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import com.android.calendar.CalendarContractCompat
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.Template

data class PlanInstance(val title: String, val date: LocalDate, val transactionID: Long?) {
    companion object {
        fun fromCursor(cursor: Cursor): PlanInstance? {
            return Template.getPlanInstance(
                    cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID)),
                    cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN)))
        }
    }
}