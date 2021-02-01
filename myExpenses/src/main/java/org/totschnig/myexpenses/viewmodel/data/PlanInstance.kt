package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import android.os.Parcelable
import com.android.calendar.CalendarContractCompat
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.util.epochMillis2LocalDate

enum class PlanInstanceState {
    OPEN, APPLIED, CANCELLED
}

@Parcelize
data class PlanInstance(val templateId: Long, val transactionId: Long?, val title: String, val date: Long, val color: Int, val amount: Money, val state: PlanInstanceState, val sealed: Boolean) : Parcelable {
    constructor(templateId: Long, instanceId: Long?, transactionId: Long?, title: String, date: Long, color: Int, amount: Money, sealed: Boolean) :
            this(templateId, transactionId, title, date, color, amount,
                    when {
                        instanceId == null -> PlanInstanceState.OPEN
                        transactionId == null -> PlanInstanceState.CANCELLED
                        else -> PlanInstanceState.APPLIED
                    }, sealed)

    val localDate: LocalDate
        get() = epochMillis2LocalDate(date)

    val instanceId: Long
        get() = CalendarProviderProxy.calculateId(date)

    companion object {
        fun fromEventCursor(cursor: Cursor) = Template.getPlanInstance(
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID)),
                cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN)))
    }
}

@Parcelize
data class PlanInstanceSet(private val set: HashSet<PlanInstance>) : Parcelable {
    constructor() : this(HashSet())

    val size: Int
        get() = set.size

    fun toList() = set.toList()

    fun clear() {
        set.clear()
    }

    fun contains(planInstance: PlanInstance) = set.contains(planInstance)

    fun remove(planInstance: PlanInstance) {
        set.remove(planInstance)
    }

    fun add(planInstance: PlanInstance) {
        set.add(planInstance)
    }
}

data class PlanInstanceUpdate(val templateId: Long, val instanceId: Long, val newState: PlanInstanceState, val transactionId: Long?, val amount: Long?)
