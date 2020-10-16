package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.calendar.CalendarContractCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjusters
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.util.localDateTime2EpochMillis
import org.totschnig.myexpenses.viewmodel.data.Event
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceUpdate

class PlannerViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    data class Month(val year: Int, val month: Int) {
        init {
            if (month < 0 || month > 12) throw IllegalArgumentException()
        }

        fun next(): Month {
            var nextMonth = month + 1
            val nextYear = if (nextMonth > 12) {
                nextMonth = 1
                year + 1
            } else year
            return Month(nextYear, nextMonth)
        }

        fun prev(): Month {
            var prevMonth = month - 1
            val prevYear = if (prevMonth < 1) {
                prevMonth = 12
                year - 1
            } else year
            return Month(prevYear, prevMonth)
        }

        fun startMillis() = localDateTime2EpochMillis(startDate().atTime(LocalTime.MIN))

        fun endMillis() = localDateTime2EpochMillis(endDate().atTime(LocalTime.MAX))

        fun endDate() = startDate().with(TemporalAdjusters.lastDayOfMonth())

        fun startDate() = LocalDate.of(year, month, 1)
    }

    var first: Month
    var last: Month

    private val formatter: DateTimeFormatter

    private var updateDisposables = CompositeDisposable()

    init {
        val nowZDT = ZonedDateTime.now().toLocalDate()
        first = Month(nowZDT.year, nowZDT.monthValue)
        last = first.next()
        formatter = getDateTimeFormatter(application)
    }

    private val instances = MutableLiveData<Event<Pair<Boolean, List<PlanInstance>>>>()
    private val title = MutableLiveData<CharSequence>()
    private val updates = MutableLiveData<PlanInstanceUpdate>()
    private val bulkCompleted = MutableLiveData<Event<List<PlanInstance>>>()
    fun getInstances(): LiveData<Event<Pair<Boolean, List<PlanInstance>>>> = instances
    fun getTitle(): LiveData<CharSequence> = title
    fun getUpdates(): LiveData<PlanInstanceUpdate> = updates
    fun getBulkCompleted(): LiveData<Event<List<PlanInstance>>> = bulkCompleted
    fun loadInstances(later: Boolean? = null) {
        // Construct the query with the desired date range.
        val startMonth: Month
        val endMonth: Month
        if (later == null) {
            //first call
            startMonth = first
            endMonth = last
        } else {
            if (later) {
                last = last.next()
                startMonth = last
                endMonth = last
            } else {
                first = first.prev()
                startMonth = first
                endMonth = first
            }
        }
        val builder = CalendarProviderProxy.INSTANCES_URI.buildUpon()
        ContentUris.appendId(builder, startMonth.startMillis())
        ContentUris.appendId(builder, endMonth.endMillis())
        viewModelScope.launch {
            val plannerCalendarId = withContext(Dispatchers.Default) {
                MyApplication.getInstance().checkPlanner()
            }
            disposable = briteContentResolver.createQuery(builder.build(), null,
                    CalendarContractCompat.Events.CALENDAR_ID + " = " + plannerCalendarId,
                    null, CalendarContractCompat.Instances.BEGIN + " ASC", false)
                    .mapToList(PlanInstance.Companion::fromEventCursor)
                    .subscribe {
                        val start = SpannableString(first.startDate().format(formatter))
                        val end = SpannableString(last.endDate().format(formatter))
                        start.setSpan(ClickableDateSpan(false), 0, start.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        end.setSpan(ClickableDateSpan(true), 0, end.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        title.postValue(TextUtils.concat(start, " - ", end))
                        instances.postValue(Event(Pair(later ?: false, it.filterNotNull())))
                    }
        }
    }

    inner class ClickableDateSpan(val later: Boolean) : ClickableSpan() {
        override fun onClick(widget: View) {
            loadInstances(later)
        }
    }

    fun getUpdateFor(uri: Uri) {
        try {
            val templateId = uri.pathSegments[1].toLong()
            val instanceId = uri.pathSegments[2].toLong()
            val mapper = { cursor: Cursor ->
                val transactionId = DbUtils.getLongOrNull(cursor, KEY_TRANSACTIONID)
                val newState = if (transactionId == null) PlanInstanceState.CANCELLED else PlanInstanceState.APPLIED
                val amount = DbUtils.getLongOrNull(cursor, KEY_AMOUNT)
                PlanInstanceUpdate(templateId, instanceId, newState, transactionId, amount)
            }
            updateDisposables.add(briteContentResolver.createQuery(uri, null, null, null, null, false)
                    .mapToOneOrDefault(mapper, PlanInstanceUpdate(templateId, instanceId, PlanInstanceState.OPEN, null, null))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        updates.value = it
                    })
        } catch (e: Exception) {
            CrashHandler.report(java.lang.IllegalArgumentException("Cannot provide update for uri $uri"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateDisposables.dispose()
    }

    fun applyBulk(selectedInstances: List<PlanInstance>) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                selectedInstances.forEach { planInstance ->
                    val instanceId = planInstance.instanceId
                    val pair = Transaction.getInstanceFromTemplateIfOpen(planInstance.templateId, instanceId)
                    pair?.first?.let {
                        it.date = planInstance.date / 1000
                        it.originPlanInstanceId = instanceId
                        it.status = DatabaseConstants.STATUS_NONE
                        if (it.save(true) != null) {
                            it.saveTags(pair.second, contentResolver)
                        }
                    }
                }
            }
            bulkCompleted.postValue(Event(selectedInstances))
        }
    }
}
