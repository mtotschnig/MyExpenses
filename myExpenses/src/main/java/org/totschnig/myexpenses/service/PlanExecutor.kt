package org.totschnig.myexpenses.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.getLabelForAccount
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.planCount
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.PermissionHelper.hasCalendarPermission
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.toEpochMillis
import org.totschnig.myexpenses.util.safeMessage
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class PlanExecutor(context: Context, workerParameters: WorkerParameters) :
    NotifyingBaseWorker(context, workerParameters) {
    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter
    @Inject
    lateinit var repository: Repository
    @Inject
    lateinit var plannerUtils: PlannerUtils

    val contentResolver: ContentResolver
        get() = repository.contentResolver

    init {
        context.injector.inject(this)
    }

    override val channelId: String = NotificationBuilderWrapper.CHANNEL_ID_PLANNER
    override val notificationId = NotificationBuilderWrapper.NOTIFICATION_PLANNER_ERROR
    override val notificationTitleResId = R.string.planner_notification_channel_name

    companion object {
        const val TAG = "PlanExecutor"
        private const val WORK_NAME = "PlanExecutor"
        private val OVERLAPPING_WINDOW = ((if (BuildConfig.DEBUG) 1 else 5) * 60 * 1000).toLong()
        const val ACTION_CANCEL = "Cancel"
        const val ACTION_APPLY = "Apply"
        const val KEY_TITLE = "title"
        const val H24 = (24 * 60 * 60 * 1000).toLong()
        const val ADVANCE_DAYS = 30

        private fun buildWorkRequest(initialDelayMillis: Long?): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<PlanExecutor>().apply {
                initialDelayMillis?.let {
                    setInitialDelay(it, TimeUnit.MILLISECONDS)
                }
            }.build()
        }

        fun enqueueSelf(
            context: Context,
            prefHandler: PrefHandler,
            forceImmediate: Boolean = false
        ) {
            val hasPermission = PermissionGroup.CALENDAR.hasPermission(context)
            val planCount = planCount(context.contentResolver)
            if (hasPermission && planCount > 0) {
                val scheduledTime = if (forceImmediate) null else TimePreference.getScheduledTime(
                    prefHandler, PrefKey.PLANNER_EXECUTION_TIME
                )
                log("enqueueSelf %d", scheduledTime)
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    buildWorkRequest(scheduledTime)
                )
            } else {
                log("not enqueueing, has calendar permission: %b, planCount: %d", hasPermission, planCount)
            }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun log(message: String, vararg args: Any?) {
            Timber.tag(TAG).i(message, *args)
        }
    }

    private fun scheduleNextRun() {
        enqueueSelf(applicationContext, prefHandler)
    }

    private fun logAndNotifyError(message: String) {
        log(message)
        notify(message)
    }

    @SuppressLint("InlinedApi")
    override suspend fun doWork(): Result {
        val nowZDT = ZonedDateTime.now()
        val beginningOfDay = nowZDT.toLocalDate().atTime(LocalTime.MIN).toEpochMillis()
        val endOfDay = nowZDT.toLocalDate().atTime(LocalTime.MAX).toEpochMillis()
        val nowMillis = nowZDT.toEpochSecond() * 1000
        val lastExecution = prefHandler.getLong(
            PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP,
            nowMillis - H24
        )
        log("now %d compared to System.currentTimeMillis %d", nowMillis, System.currentTimeMillis())
        if (!hasCalendarPermission(applicationContext)) {
            logAndNotifyError("Calendar permission not granted")
            return Result.failure()
        }
        //noinspection MissingPermission
        val plannerCalendarId: String? = plannerUtils.checkPlanner()
        if (plannerCalendarId == null) {
            logAndNotifyError("planner verification failed, will try later")
            scheduleNextRun()
            return Result.failure()
        }
        if (plannerCalendarId == INVALID_CALENDAR_ID) {
            logAndNotifyError("no planner set, nothing to do")
            return Result.failure()
        }
        //we use an overlapping window of 5 minutes to prevent plans that are just created by the user while
        //we are running from falling through
        val instancesFrom =
            (lastExecution - OVERLAPPING_WINDOW).coerceAtMost(beginningOfDay)
        if (nowMillis < instancesFrom) {
            logAndNotifyError("Broken system time? Cannot execute plans.")
            return Result.failure()
        }
        log("now %d compared to end of day %d", nowMillis, endOfDay)
        val instancesUntil = endOfDay + ADVANCE_DAYS * H24
        log("executing plans from %d to %d", instancesFrom, instancesUntil)

        val eventsUriBuilder = CalendarProviderProxy.INSTANCES_URI.buildUpon()
        ContentUris.appendId(eventsUriBuilder, instancesFrom)
        ContentUris.appendId(eventsUriBuilder, instancesUntil)
        val eventsUri = eventsUriBuilder.build()
        try {
            contentResolver.query(
                eventsUri, null,
                CalendarContract.Events.CALENDAR_ID + " = " + plannerCalendarId,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )
        } catch (e: Exception) {
            //} catch (SecurityException | IllegalArgumentException e) {
            report(e, TAG)
            notify(e.safeMessage)
            //android.permission.READ_CALENDAR or android.permission.WRITE_CALENDAR missing (SecurityException)
            //buggy calendar provider implementation on Sony (IllegalArgumentException)
            //sqlite database not yet available observed on samsung GT-N7100 (SQLiteException)
            return Result.failure()
        }?.use { cursor ->
            if (cursor.moveToFirst()) {
                val today = LocalDate.now()
                while (!cursor.isAfterLast && !isStopped) {
                    val planId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID))
                    val date =
                        cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN))
                    val localDate = epochMillis2LocalDate(date)
                    val diff = ChronoUnit.DAYS.between(today, localDate)
                    val instanceId = CalendarProviderProxy.calculateId(date)
                    //2) check if they are part of a plan linked to a template
                    //3) execute the template
                    log("found instance %d of plan %d", instanceId, planId)
                    //TODO if we have multiple Event instances for one plan, we should maybe cache the template objects
                    val template = Template.getInstanceForPlanIfInstanceIsOpen(contentResolver, planId, instanceId)
                    if (!(template == null || template.isSealed)) {
                        if (template.planExecutionAdvance >= diff) {
                            val accountLabel = repository.getLabelForAccount(template.accountId)
                            if (accountLabel != null) {
                                log("belongs to template %d", template.id)
                                var notification: Notification
                                val notificationId = (instanceId * planId % Int.MAX_VALUE).toInt()
                                log("notification id %d", notificationId)
                                var resultIntent: PendingIntent?
                                val title = accountLabel + " : " + template.title
                                val builder = NotificationBuilderWrapper(
                                    applicationContext,
                                    NotificationBuilderWrapper.CHANNEL_ID_PLANNER
                                )
                                    .setSmallIcon(R.drawable.ic_stat_notification_sigma)
                                    .setContentTitle(title)
                                builder.setWhen(date)
                                var content: String = template.categoryPath?.let { "$it : " } ?: ""
                                content += currencyFormatter.formatMoney(template.amount)
                                builder.setContentText(content)
                                if (template.isPlanExecutionAutomatic) {
                                    val (t, second) = Transaction.getInstanceFromTemplateWithTags(
                                        contentResolver, template
                                    )
                                    t.originPlanInstanceId = instanceId
                                    t.date = date / 1000
                                    if (t.save(contentResolver, true) != null) {
                                        t.saveTags(contentResolver, second)
                                        val displayIntent: Intent =
                                            Intent(applicationContext, MyExpenses::class.java)
                                                .putExtra(
                                                    DatabaseConstants.KEY_ROWID,
                                                    template.accountId
                                                )
                                                .putExtra(
                                                    DatabaseConstants.KEY_TRANSACTIONID,
                                                    t.id
                                                )
                                        resultIntent = PendingIntent.getActivity(
                                            applicationContext, notificationId, displayIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        builder.setContentIntent(resultIntent)
                                    } else {
                                        builder.setContentText(wrappedContext.getString(R.string.save_transaction_error))
                                    }
                                    builder.setAutoCancel(true)
                                    notification = builder.build()
                                } else {
                                    val cancelIntent: Intent =
                                        Intent(
                                            applicationContext,
                                            PlanNotificationClickHandler::class.java
                                        )
                                            .setAction(ACTION_CANCEL)
                                            .putExtra(
                                                MyApplication.KEY_NOTIFICATION_ID,
                                                notificationId
                                            )
                                            .putExtra(
                                                DatabaseConstants.KEY_TEMPLATEID,
                                                template.id
                                            )
                                            .putExtra(
                                                DatabaseConstants.KEY_INSTANCEID,
                                                instanceId
                                            ) //we also put the title in the intent, because we need it while we update the notification
                                            .putExtra(KEY_TITLE, title)
                                    builder.addAction(
                                        R.drawable.ic_menu_close_clear_cancel,
                                        R.drawable.ic_menu_close_clear_cancel,
                                        wrappedContext.getString(android.R.string.cancel),
                                        PendingIntent.getService(
                                            applicationContext,
                                            notificationId,
                                            cancelIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                    )
                                    val editIntent: Intent =
                                        Intent(applicationContext, ExpenseEdit::class.java)
                                            .putExtra(
                                                MyApplication.KEY_NOTIFICATION_ID,
                                                notificationId
                                            )
                                            .putExtra(
                                                DatabaseConstants.KEY_TEMPLATEID,
                                                template.id
                                            )
                                            .putExtra(DatabaseConstants.KEY_INSTANCEID, instanceId)
                                    val useDateFromPlan =
                                        "noon" == prefHandler.getString(
                                            PrefKey.PLANNER_MANUAL_TIME,
                                            "noon"
                                        )
                                    if (useDateFromPlan) {
                                        editIntent.putExtra(DatabaseConstants.KEY_DATE, date / 1000)
                                    }
                                    resultIntent = PendingIntent.getActivity(
                                        applicationContext,
                                        notificationId,
                                        editIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    builder.addAction(
                                        R.drawable.ic_menu_edit,
                                        R.drawable.ic_menu_edit,
                                        wrappedContext.getString(R.string.menu_edit),
                                        resultIntent
                                    )
                                    val applyIntent =
                                        Intent(
                                            applicationContext,
                                            PlanNotificationClickHandler::class.java
                                        )
                                    applyIntent.setAction(ACTION_APPLY)
                                        .putExtra(MyApplication.KEY_NOTIFICATION_ID, notificationId)
                                        .putExtra(KEY_TITLE, title)
                                        .putExtra(
                                            DatabaseConstants.KEY_TEMPLATEID,
                                            template.id
                                        )
                                        .putExtra(DatabaseConstants.KEY_INSTANCEID, instanceId)
                                    if (useDateFromPlan) {
                                        applyIntent.putExtra(DatabaseConstants.KEY_DATE, date)
                                    }
                                    builder.addAction(
                                        R.drawable.ic_menu_save,
                                        R.drawable.ic_menu_save,
                                        wrappedContext.getString(R.string.menu_apply_template),
                                        PendingIntent.getService(
                                            applicationContext,
                                            notificationId,
                                            applyIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                    )
                                    builder.setContentIntent(resultIntent)
                                    notification = builder.build()
                                    notification.flags =
                                        notification.flags or Notification.FLAG_NO_CLEAR
                                }
                                notificationManager.notify(notificationId, notification)
                            } else {
                                log("Account.getInstanceFromDb returned null")
                            }
                        } else {
                            log(
                                "Instance is not ready yet (%d days in the future), advance execution is %d",
                                diff,
                                template.planExecutionAdvance
                            )
                        }
                    } else {
                        log(if (template == null) "Template.getInstanceForPlanIfInstanceIsOpen returned null, instance might already have been dealt with" else "Plan refers to a closed account or debt")
                    }
                    cursor.moveToNext()
                }
            }
        }

        prefHandler.putLong(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP, nowMillis)
        scheduleNextRun()
        return Result.success()
    }
}