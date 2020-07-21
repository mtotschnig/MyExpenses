package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.calendar.CalendarContractCompat
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.viewmodel.data.PlanInstance

class PlannerViewModell(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val instances = MutableLiveData<List<PlanInstance>>()
    fun getInstances(): LiveData<List<PlanInstance>> {
        return instances
    }
    fun loadInstances() {
        // Construct the query with the desired date range.
        val nowZDT = ZonedDateTime.now()
        val start = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MIN), ZoneId.systemDefault()).toEpochSecond() * 1000
        val end = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MAX), ZoneId.systemDefault()).toEpochSecond() * 1000 + 30 * PlanExecutor.H24
        val builder = CalendarProviderProxy.INSTANCES_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val plannerCalendarId = MyApplication.getInstance().checkPlanner()
        disposable = briteContentResolver.createQuery(builder.build(), null,
                CalendarContractCompat.Events.CALENDAR_ID + " = " + plannerCalendarId,
                null, CalendarContractCompat.Instances.BEGIN + " ASC", false)
                .mapToList(PlanInstance.Companion::fromCursor)
                .subscribe {
                    instances.postValue(it.filterNotNull())
                }
    }
}
