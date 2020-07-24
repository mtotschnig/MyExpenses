package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.calendar.CalendarContractCompat
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.data.PlanInstance

class PlannerViewModell(application: Application) : ContentResolvingAndroidViewModel(application) {
    var min: Long
    var max: Long

    private val formatter : DateTimeFormatter

    init {
        val nowZDT = ZonedDateTime.now()
        min = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MIN), ZoneId.systemDefault()).toEpochSecond() * 1000
        max = min
        formatter = getDateTimeFormatter(application)
    }
    private val instances = MutableLiveData<Pair<Boolean, List<PlanInstance>>>()
    private val title = MutableLiveData<String>()
    fun getInstances(): LiveData<Pair<Boolean, List<PlanInstance>>> = instances
    fun getTitle(): LiveData<String> = title
    fun loadInstances(later: Boolean = true) {
        // Construct the query with the desired date range.
        val start: Long
        val end: Long
        if (later) {
            start = max
            max = max + 30 * PlanExecutor.H24
            end = max
        } else {
            end = min
            min = min - 30 * PlanExecutor.H24
            start = min
        }
        val builder = CalendarProviderProxy.INSTANCES_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val plannerCalendarId = MyApplication.getInstance().checkPlanner()
        disposable = briteContentResolver.createQuery(builder.build(), null,
                CalendarContractCompat.Events.CALENDAR_ID + " = " + plannerCalendarId,
                null, CalendarContractCompat.Instances.BEGIN + " ASC", false)
                .mapToList(PlanInstance.Companion::fromEventCursor)
                .subscribe {
                    title.postValue("%s - %s".format(epochMillis2LocalDate(min).format(formatter),
                            epochMillis2LocalDate(max).format(formatter)))
                    instances.postValue(Pair(later, it.filterNotNull()))
                }
    }
}
