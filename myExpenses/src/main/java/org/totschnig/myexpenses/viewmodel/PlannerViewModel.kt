package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.calendar.CalendarContractCompat
import io.reactivex.disposables.Disposable
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceUpdate

class PlannerViewModell(application: Application) : ContentResolvingAndroidViewModel(application) {
    var min: Long
    var max: Long

    private val formatter : DateTimeFormatter

    private var updateDisposable: Disposable? = null

    init {
        val nowZDT = ZonedDateTime.now()
        min = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MIN), ZoneId.systemDefault()).toEpochSecond() * 1000
        max = min
        formatter = getDateTimeFormatter(application)
    }
    private val instances = MutableLiveData<Pair<Boolean, List<PlanInstance>>>()
    private val title = MutableLiveData<String>()
    private val updates = MutableLiveData<PlanInstanceUpdate>()
    fun getInstances(): LiveData<Pair<Boolean, List<PlanInstance>>> = instances
    fun getTitle(): LiveData<String> = title
    fun getUpdates(): LiveData<PlanInstanceUpdate> = updates
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

    fun getUpdateFor(uri: Uri) {
        val templateId = uri.pathSegments[1].toLong()
        val instanceId = uri.pathSegments[2].toLong()
        val mapper = {cursor: Cursor ->
            val transactionId = DbUtils.getLongOrNull(cursor, KEY_TRANSACTIONID)
            val newState = if (transactionId == null) PlanInstanceState.CANCELLED else PlanInstanceState.APPLIED
            PlanInstanceUpdate(templateId, instanceId, newState, transactionId)
        }
        updateDisposable = briteContentResolver.createQuery(uri, null, null, null, null, false)
                .mapToOneOrDefault(mapper, PlanInstanceUpdate(templateId, instanceId, PlanInstanceState.OPEN, null))
                .subscribe {
                    updates.postValue(it)
                    updateDisposable?.dispose()
                }
    }
}
