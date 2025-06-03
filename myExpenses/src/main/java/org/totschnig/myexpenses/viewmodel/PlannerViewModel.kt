package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.instantiateTemplate
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.util.toEpochMillis
import org.totschnig.myexpenses.viewmodel.data.Event
import org.totschnig.myexpenses.viewmodel.data.PlanInstance
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceUpdate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class PlannerViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var plannerUtils: PlannerUtils

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    data class Month(val year: Int, val month: Int, val startDay: Int = 1) {
        init {
            if (month < 0 || month > 12) throw IllegalArgumentException()
        }

        fun next(): Month {
            var nextMonth = month + 1
            val nextYear = if (nextMonth > 12) {
                nextMonth = 1
                year + 1
            } else year
            return Month(nextYear, nextMonth, startDay)
        }

        fun prev(): Month {
            var prevMonth = month - 1
            val prevYear = if (prevMonth < 1) {
                prevMonth = 12
                year - 1
            } else year
            return Month(prevYear, prevMonth, startDay)
        }

        fun startMillis() = startDate().atTime(LocalTime.MIN).toEpochMillis()

        fun endMillis() = endDate().atTime(LocalTime.MAX).toEpochMillis()

        fun endDate(): LocalDate = if (startDay == 1)
            startDate().with(TemporalAdjusters.lastDayOfMonth()) else
            startDate().minusDays(1).plusMonths(1)

        fun startDate(): LocalDate = LocalDate.of(year, month, startDay)
    }

    var first: Month
    var last: Month

    private val formatter: DateTimeFormatter

    private var updateMap: MutableMap<Uri, Flow<PlanInstanceUpdate>> = mutableMapOf()

    init {
        application.injector.inject(this)
        val nowZDT = ZonedDateTime.now().toLocalDate()
        first = Month(nowZDT.year, nowZDT.monthValue, prefHandler.monthStart)
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
            val plannerCalendarId = withContext(Dispatchers.Default) { plannerUtils.checkPlanner() }
            contentResolver.observeQuery( builder.build(), null,
                CalendarContract.Events.CALENDAR_ID + " = " + plannerCalendarId,
                null, CalendarContract.Instances.BEGIN + " ASC", false)
                .mapToList { PlanInstance.fromEventCursor(it, contentResolver, currencyContext) }
                .collect {
                    val start = SpannableString(first.startDate().format(formatter))
                    val end = SpannableString(last.endDate().format(formatter))
                    start.setSpan(
                        ClickableDateSpan(false),
                        0,
                        start.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    end.setSpan(
                        ClickableDateSpan(true),
                        0,
                        end.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    title.postValue(TextUtils.concat(start, " - ", end))
                    instances.postValue(Event(Pair(later == true, it.filterNotNull())))
                }
        }
    }

    inner class ClickableDateSpan(val later: Boolean) : ClickableSpan() {
        override fun onClick(widget: View) {
            loadInstances(later)
        }
    }

    fun getUpdateFor(uri: Uri) {
        if (!updateMap.contains(uri)) {
            val templateId = uri.pathSegments[1].toLong()
            val instanceId = uri.pathSegments[2].toLong()
            val mapper = { cursor: Cursor ->
                val transactionId = cursor.getLongOrNull(KEY_TRANSACTIONID)
                val newState =
                    if (transactionId == null) PlanInstanceState.CANCELLED else PlanInstanceState.APPLIED
                val amount = cursor.getLongOrNull(KEY_AMOUNT)
                PlanInstanceUpdate(templateId, instanceId, newState, transactionId, amount)
            }
            updateMap[uri] = contentResolver.observeQuery(uri, null, null, null, null, false)
                .mapToOne(
                    default = PlanInstanceUpdate(
                        templateId,
                        instanceId,
                        PlanInstanceState.OPEN,
                        null,
                        null
                    ),
                    mapper = mapper
                ).also {
                    viewModelScope.launch {
                        it.collect {
                            updates.value = it
                        }
                    }
                }
        }
    }

    fun applyBulk(selectedInstances: List<PlanInstance>) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                selectedInstances.forEach { planInstance ->
                    instantiateTemplate(
                        repository,
                        exchangeRateHandler,
                        PlanInstanceInfo(
                            planInstance.templateId,
                            planInstance.instanceId,
                            planInstance.date
                        ),
                        currencyContext.homeCurrencyUnit,
                        ifOpen = true
                    )
                }
            }
            bulkCompleted.postValue(Event(selectedInstances))
        }
    }
}
