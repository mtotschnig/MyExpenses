package org.totschnig.myexpenses.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.ComponentName
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.totschnig.myexpenses.activity.BudgetWidgetConfigure
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Grouping.DAY
import org.totschnig.myexpenses.model.Grouping.MONTH
import org.totschnig.myexpenses.model.Grouping.NONE
import org.totschnig.myexpenses.model.Grouping.WEEK
import org.totschnig.myexpenses.model.Grouping.YEAR
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.updateWidgets
import timber.log.Timber
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters.next
import java.util.concurrent.TimeUnit

class BudgetWidgetUpdateWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        Timber.d("doWork: %s", inputData)
        updateWidgets(
            applicationContext,
            BudgetWidget::class.java,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            inputData.getIntArray(EXTRA_APPWIDGET_IDS)!!
        )
        scheduleNextRun(Grouping.valueOf(inputData.getString(KEY_GROUPING)!!))
        return Result.success()
    }

    private fun scheduleNextRun(grouping: Grouping) {
        enqueueSelf(applicationContext, grouping)
    }

    companion object {
        private const val DEBUG = false
        private const val WORK_NAME_PREFIX = "BudgetWidgetUpdate"
        private fun workName(grouping: Grouping) = WORK_NAME_PREFIX + grouping.name

        fun enqueueSelf(
            context: Context,
            grouping: Grouping,
            forceImmediate: Boolean = false,
            clock: Clock = Clock.systemDefaultZone()
        ) {
            val prefHandler = context.injector.prefHandler()
            val manager = WorkManager.getInstance(context)
            if (grouping == NONE) return
            val workName = workName(grouping)
            val widgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, BudgetWidget::class.java))
                .filter {
                    BudgetWidgetConfigure.loadGroupingString(context, it) == grouping.name
                }.toIntArray()
            if (widgetIds.isEmpty()) {
                manager.cancelUniqueWork(workName)
            } else {
                val nextScheduledTime = getNextScheduledTime(grouping, prefHandler, clock)
                //extend delay for 5 seconds into next day to prevent it from running too early
                val delay = Duration.between(clock.instant(), nextScheduledTime).toMillis() + 5000
                manager.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<BudgetWidgetUpdateWorker>()
                        .apply {
                            if (!forceImmediate) {
                                setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            }
                            setInputData(
                                Data.Builder()
                                    .putIntArray(EXTRA_APPWIDGET_IDS, widgetIds)
                                    .putString(KEY_GROUPING, grouping.name)
                                    .build()
                            )
                        }.build()
                )
            }
        }

        @VisibleForTesting
        fun getNextScheduledTime(
            grouping: Grouping,
            prefHandler: PrefHandler,
            clock: Clock = Clock.systemDefaultZone()
        ): Instant {
            val now = LocalDate.now(clock)
            return (if (DEBUG) ZonedDateTime.of(
                now,
                LocalTime.now().plusMinutes(1),
                ZoneId.systemDefault()
            )
            else when (grouping) {
                NONE -> throw IllegalArgumentException()
                DAY -> ZonedDateTime.of(
                    now.plusDays(1),
                    LocalTime.MIDNIGHT,
                    ZoneId.systemDefault()
                )

                WEEK -> ZonedDateTime.of(
                    now.with(next(prefHandler.weekStartAsDayOfWeek)),
                    LocalTime.MIDNIGHT,
                    ZoneId.systemDefault()
                )

                MONTH -> {
                    val monthStart = prefHandler.monthStart
                    ZonedDateTime.of(
                        now.plusMonths(if (now.dayOfMonth < monthStart) 0 else 1).withDayOfMonth(monthStart),
                        LocalTime.MIDNIGHT,
                        ZoneId.systemDefault()
                    )
                }

                YEAR -> ZonedDateTime.of(
                    now.plusYears(1).withMonth(1).withDayOfMonth(1),
                    LocalTime.MIDNIGHT,
                    ZoneId.systemDefault()
                )
            }).toInstant()
        }
    }
}