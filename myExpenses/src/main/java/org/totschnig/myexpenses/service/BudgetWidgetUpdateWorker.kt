package org.totschnig.myexpenses.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.totschnig.myexpenses.activity.BudgetWidgetConfigure
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Grouping.DAY
import org.totschnig.myexpenses.model.Grouping.MONTH
import org.totschnig.myexpenses.model.Grouping.NONE
import org.totschnig.myexpenses.model.Grouping.WEEK
import org.totschnig.myexpenses.model.Grouping.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.updateWidgets
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class BudgetWidgetUpdateWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
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
        private const val WORK_NAME_PREFIX = "BudgetWidgetUpdate"
        private fun workName(grouping: Grouping) = WORK_NAME_PREFIX + grouping.name

        fun enqueueSelf(context: Context, grouping: Grouping) {
            val manager = WorkManager.getInstance(context)
            if (grouping == NONE) return
            val workName = workName(grouping)
            val widgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, BudgetWidget::class.java))
                .filter {
                    BudgetWidgetConfigure.loadSelectionPref(context, it).second == grouping.name
                }.toIntArray()
            if (widgetIds.isEmpty()) {
                manager.cancelUniqueWork(workName)
            } else {
                val nextScheduledTime = getNextScheduledTime(grouping)
                val delay = Duration.between(Clock.systemDefaultZone().instant(), nextScheduledTime).toSeconds()
                manager.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<BudgetWidgetUpdateWorker>()
                        .apply {
                            setInitialDelay(delay, TimeUnit.SECONDS)
                            setInputData(Data.Builder()
                                .putIntArray(EXTRA_APPWIDGET_IDS, widgetIds)
                                .putString(KEY_GROUPING, grouping.name)
                                .build())
                        }.build()
                )
            }
        }

        private fun getNextScheduledTime(grouping: Grouping) = when(grouping) {
            NONE -> throw IllegalArgumentException()
            DAY -> ZonedDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0), ZoneId.systemDefault()).toInstant()
            WEEK -> TODO()
            MONTH -> TODO()
            YEAR -> TODO()
        }
    }
}