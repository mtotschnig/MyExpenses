package org.totschnig.myexpenses.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import androidx.core.app.JobIntentService;
import kotlin.Pair;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static org.totschnig.myexpenses.MyApplication.INVALID_CALENDAR_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.util.DateUtilsKt.epochMillis2LocalDate;
import static org.totschnig.myexpenses.util.DateUtilsKt.localDateTime2EpochMillis;

public class PlanExecutor extends JobIntentService {
  public static final String ACTION_EXECUTE_PLANS = BuildConfig.APPLICATION_ID + ".ACTION_EXECUTE_PLANS";
  public static final String ACTION_SCHEDULE_EXECUTE_PLANS = BuildConfig.APPLICATION_ID + ".ACTION_SCHEDULE_EXECUTE_PLANS";
  public static final String ACTION_CANCEL = "Cancel";
  public static final String ACTION_APPLY = "Apply";
  public static final String KEY_TITLE = "title";
  public static final long H24 = 24 * 60 * 60 * 1000;
  private static final long OVERLAPPING_WINDOW = (BuildConfig.DEBUG ? 1 : 5) * 60 * 1000;
  public static final String TAG = "PlanExecutor";
  public static final String KEY_FORCE_IMMEDIATE = "force_immediate";
  public static final int ADVANCE_DAYS = 30;

  @Inject
  PrefHandler prefHandler;
  @Inject
  CurrencyFormatter currencyFormatter;

  /**
   * Unique job ID for this service.
   */
  static final int JOB_ID = 1001;

  /**
   * Convenience method for enqueuing work in to this service.
   */
  static void enqueueWork(Context context, Intent work) {
    enqueueWork(context, PlanExecutor.class, JOB_ID, work);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    ((MyApplication) getApplication()).getAppComponent().inject(this);
  }

  @Override
  public void onHandleWork(Intent intent) {
    String action = intent.getAction();
    if (ACTION_EXECUTE_PLANS.equals(action)) {
      String plannerCalendarId;
      ZonedDateTime nowZDT = ZonedDateTime.now();
      final long beginningOfDay = localDateTime2EpochMillis(nowZDT.toLocalDate().atTime(LocalTime.MIN));
      final long endOfDay = localDateTime2EpochMillis(nowZDT.toLocalDate().atTime(LocalTime.MAX));
      long nowMillis = nowZDT.toEpochSecond() * 1000;
      final long lastExecution = prefHandler.getLong(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP, nowMillis - H24);
      log("now %d compared to System.currentTimeMillis %d", nowMillis, System.currentTimeMillis());
      if (!PermissionHelper.hasCalendarPermission(this)) {
        log("Calendar permission not granted");
        return;
      }
      try {
        plannerCalendarId = MyApplication.getInstance().checkPlanner();
      } catch (Exception e) {
        log(e);
        CrashHandler.report(e);
        return;
      }
      if (plannerCalendarId == null) {
        log("planner verification failed, try later");
        scheduleNextRun(false);
        return;
      }
      if (plannerCalendarId.equals(INVALID_CALENDAR_ID)) {
        log("no planner set, nothing to do");
        return;
      }
      //we use an overlapping window of 5 minutes to prevent plans that are just created by the user while
      //we are running from falling through
      long instancesFrom = Math.min(lastExecution - OVERLAPPING_WINDOW, beginningOfDay);
      if (nowMillis < instancesFrom) {
        log("Broken system time? Cannot execute plans.");
        return;
      }
      if (intent.getBooleanExtra(KEY_FORCE_IMMEDIATE, false) || beginningOfDay > lastExecution) {
        log("now %d compared to end of day %d", nowMillis, endOfDay);
        long instancesUntil = endOfDay + ADVANCE_DAYS * H24;
        log("executing plans from %d to %d", instancesFrom, instancesUntil);

        Uri.Builder eventsUriBuilder = CalendarProviderProxy.INSTANCES_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, instancesFrom);
        ContentUris.appendId(eventsUriBuilder, instancesUntil);
        Uri eventsUri = eventsUriBuilder.build();
        Cursor cursor;
        try {
          cursor = getContentResolver().query(eventsUri, null,
              Events.CALENDAR_ID + " = " + plannerCalendarId,
              null,
              CalendarContractCompat.Instances.BEGIN + " ASC");
        } catch (Exception e) {
          //} catch (SecurityException | IllegalArgumentException e) {
          CrashHandler.report(e);
          //android.permission.READ_CALENDAR or android.permission.WRITE_CALENDAR missing (SecurityException)
          //buggy calendar provider implementation on Sony (IllegalArgumentException)
          //sqlite database not yet available observed on samsung GT-N7100 (SQLiteException)
          return;
        }
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            LocalDate today = LocalDate.now();
            while (!cursor.isAfterLast()) {
              long planId = cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID));
              long date = cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN));
              LocalDate localDate = epochMillis2LocalDate(date);
              long diff = ChronoUnit.DAYS.between(today, localDate);
              long instanceId = CalendarProviderProxy.calculateId(date);
              //2) check if they are part of a plan linked to a template
              //3) execute the template
              log("found instance %d of plan %d", instanceId, planId);
              //TODO if we have multiple Event instances for one plan, we should maybe cache the template objects
              Template template = Template.getInstanceForPlanIfInstanceIsOpen(planId, instanceId);
              if (!(template == null || template.isSealed())) {
                if (template.getPlanExecutionAdvance() >= diff) {
                  Account account = Account.getInstanceFromDb(template.getAccountId());
                  if (account != null) {
                    log("belongs to template %d", template.getId());
                    Notification notification;
                    int notificationId = (int) ((instanceId * planId) % Integer.MAX_VALUE);
                    log("notification id %d", notificationId);
                    PendingIntent resultIntent;
                    NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    String title = account.getLabel() + " : " + template.getTitle();
                    NotificationBuilderWrapper builder =
                        new NotificationBuilderWrapper(this, NotificationBuilderWrapper.CHANNEL_ID_PLANNER)
                            .setSmallIcon(R.drawable.ic_stat_notification_sigma)
                            .setContentTitle(title);
                    builder.setWhen(date);
                    String content = template.getLabel();
                    if (TextUtils.isEmpty(content)) {
                      content = "";
                    } else {
                      content += " : ";
                    }
                    content += currencyFormatter.formatCurrency(template.getAmount());
                    builder.setContentText(content);
                    if (template.isPlanExecutionAutomatic()) {
                      Pair<Transaction, List<Tag>> pair = Transaction.getInstanceFromTemplateWithTags(template);
                      Transaction t = pair.getFirst();
                      t.setOriginPlanInstanceId(instanceId);
                      t.setDate(new Date(date));
                      if (t.save(true) != null && t.saveTags(pair.getSecond(), getContentResolver())) {
                        Intent displayIntent = new Intent(this, MyExpenses.class)
                            .putExtra(KEY_ROWID, template.getAccountId())
                            .putExtra(KEY_TRANSACTIONID, t.getId());
                        resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
                            FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(resultIntent);
                      } else {
                        builder.setContentText(getString(R.string.save_transaction_error));
                      }
                      builder.setAutoCancel(true);
                      notification = builder.build();
                    } else {
                      Intent cancelIntent = new Intent(this, PlanNotificationClickHandler.class)
                          .setAction(ACTION_CANCEL)
                          .putExtra(MyApplication.KEY_NOTIFICATION_ID, notificationId)
                          .putExtra(KEY_TEMPLATEID, template.getId())
                          .putExtra(KEY_INSTANCEID, instanceId)
                          //we also put the title in the intent, because we need it while we update the notification
                          .putExtra(KEY_TITLE, title);
                      builder.addAction(
                          android.R.drawable.ic_menu_close_clear_cancel,
                          R.drawable.ic_menu_close_clear_cancel,
                          getString(android.R.string.cancel),
                          PendingIntent.getService(this, notificationId, cancelIntent, FLAG_UPDATE_CURRENT));
                      Intent editIntent = new Intent(this, ExpenseEdit.class)
                          .putExtra(MyApplication.KEY_NOTIFICATION_ID, notificationId)
                          .putExtra(KEY_TEMPLATEID, template.getId())
                          .putExtra(KEY_INSTANCEID, instanceId);
                      final boolean useDateFromPlan = "noon".equals(prefHandler.getString(PrefKey.PLANNER_MANUAL_TIME, "noon"));
                      if (useDateFromPlan) {
                          editIntent.putExtra(KEY_DATE, date);
                      }
                      resultIntent = PendingIntent.getActivity(this, notificationId, editIntent, FLAG_UPDATE_CURRENT);
                      builder.addAction(
                          android.R.drawable.ic_menu_edit,
                          R.drawable.ic_menu_edit,
                          getString(R.string.menu_edit),
                          resultIntent);
                      Intent applyIntent = new Intent(this, PlanNotificationClickHandler.class);
                      applyIntent.setAction(ACTION_APPLY)
                          .putExtra(MyApplication.KEY_NOTIFICATION_ID, notificationId)
                          .putExtra(KEY_TITLE, title)
                          .putExtra(KEY_TEMPLATEID, template.getId())
                          .putExtra(KEY_INSTANCEID, instanceId);
                      if (useDateFromPlan) {
                        applyIntent.putExtra(KEY_DATE, date);
                      }
                      builder.addAction(
                          android.R.drawable.ic_menu_save,
                          R.drawable.ic_menu_save,
                          getString(R.string.menu_apply_template),
                          PendingIntent.getService(this, notificationId, applyIntent, FLAG_UPDATE_CURRENT));
                      builder.setContentIntent(resultIntent);
                      notification = builder.build();
                      notification.flags |= Notification.FLAG_NO_CLEAR;
                    }
                    notificationManager.notify(notificationId, notification);
                  } else {
                    log("Account.getInstanceFromDb returned null");
                  }
                } else {
                  log("Instance is not ready yet (%d days in the future), advance execution is %d", diff, template.getPlanExecutionAdvance());
                }
              } else {
                log(template == null ? "Template.getInstanceForPlanIfInstanceIsOpen returned null, instance might already have been dealt with" : "Plan refers to a closed account");
              }
              cursor.moveToNext();
            }
          }
          cursor.close();
        }

        prefHandler.putLong(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP, nowMillis);
      } else {
        log("Plans have already been executed today, nothing to do");
      }
      scheduleNextRun(false);
    } else if (ACTION_SCHEDULE_EXECUTE_PLANS.equals(action)) {
      scheduleNextRun(true);
    }
  }

  private void scheduleNextRun(boolean now) {
    DailyScheduler.updatePlannerAlarms(this, false, now);
  }

  private void log(Exception e) {
    Timber.tag(TAG).w(e);
  }

  private void log(String message, Object... args) {
    Timber.tag(TAG).i(message, args);
  }
}