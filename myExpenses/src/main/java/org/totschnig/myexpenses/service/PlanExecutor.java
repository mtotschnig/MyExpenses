package org.totschnig.myexpenses.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.Date;

import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class PlanExecutor extends IntentService {
  public static final String ACTION_CANCEL = "Cancel";
  public static final String ACTION_APPLY = "Apply";
  public static final String KEY_TITLE = "title";
  //production: 21600000 6* 60 * 60 * 1000 6 hours; for testing: 60000 1 minute
  private static final long H24 = 24 * 60 * 60 * 1000;
  private static final long OVERLAPPING_WINDOW = (BuildConfig.DEBUG ? 1 : 5) * 60 * 1000;
  public static final String TAG = "PlanExecutor";

  public PlanExecutor() {
    super("PlanExexcutor");
  }

  @Override
  public void onHandleIntent(Intent intent) {
    String plannerCalendarId;
    ZonedDateTime nowZDT = ZonedDateTime.now();
    long beginningOfDay = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MIN), ZoneId.systemDefault()).toEpochSecond() * 1000;
    long endOfDay = ZonedDateTime.of(nowZDT.toLocalDate().atTime(LocalTime.MAX), ZoneId.systemDefault()).toEpochSecond() * 1000;
    LocalDateTime nextRun = nowZDT.toLocalDate().atTime(6,0).plusDays(1);
    long now = nowZDT.toEpochSecond() * 1000;
    log("now %d compared to System.currentTimeMillis %d", now, System.currentTimeMillis());
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
    if (plannerCalendarId.equals("-1")) {
      log("no planner set, nothing to do");
      return;
    }
    //we use an overlapping window of 5 minutes to prevent plans that are just created by the user while
    //we are running from falling through
    long instancesFrom = PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP.getLong(now - H24) - OVERLAPPING_WINDOW;
    if (now < instancesFrom) {
      log("Broken system time? Cannot execute plans.");
      return;
    }
    if (beginningOfDay < instancesFrom) {
      instancesFrom = beginningOfDay;
    }
    log("now %d compared to end of day %d", now, endOfDay);
    log("executing plans from %d to %d", instancesFrom, endOfDay);

    Uri.Builder eventsUriBuilder = CalendarProviderProxy.INSTANCES_URI.buildUpon();
    ContentUris.appendId(eventsUriBuilder, instancesFrom);
    ContentUris.appendId(eventsUriBuilder, endOfDay);
    Uri eventsUri = eventsUriBuilder.build();
    Cursor cursor;
    try {
      cursor = getContentResolver().query(eventsUri, null,
          Events.CALENDAR_ID + " = " + plannerCalendarId,
          null,
          null);
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
        while (!cursor.isAfterLast()) {
          long planId = cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.EVENT_ID));
          long date = cursor.getLong(cursor.getColumnIndex(CalendarContractCompat.Instances.BEGIN));
          long instanceId = CalendarProviderProxy.calculateId(date);
          //2) check if they are part of a plan linked to a template
          //3) execute the template
          log("found instance %d of plan %d", instanceId, planId);
          //TODO if we have multiple Event instances for one plan, we should maybe cache the template objects
          Template template = Template.getInstanceForPlanIfInstanceIsOpen(planId, instanceId);
          if (template != null) {
            log("belongs to template %d", template.getId());
            Notification notification;
            int notificationId = (int) ((instanceId * planId) % Integer.MAX_VALUE);
            log("notification id %d", notificationId);
            PendingIntent resultIntent;
            Account account = Account.getInstanceFromDb(template.getAccountId());
            if (account != null) {
              NotificationManager notificationManager =
                  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
              String title = account.getLabel() + " : " + template.getTitle();
              NotificationBuilderWrapper builder =
                  new NotificationBuilderWrapper(this, NotificationBuilderWrapper.CHANNEL_ID_PLANNER)
                      .setSmallIcon(R.drawable.ic_stat_notification_sigma)
                      .setContentTitle(title);
              if (account.isSealed()) {
                builder.setContentText(getString(R.string.warning_account_for_plan_is_closed, template.getTitle(), account.getLabel()));
                notification = builder.build();
              } else {
                String content = template.getLabel();
                if (!content.equals("")) {
                  content += " : ";
                }
                content += CurrencyFormatter.instance().formatCurrency(template.getAmount());
                builder.setContentText(content);
                if (template.isPlanExecutionAutomatic()) {
                  Transaction t = Transaction.getInstanceFromTemplate(template);
                  t.originPlanInstanceId = instanceId;
                  t.setDate(new Date(date));
                  if (t.save() != null) {
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
                      .putExtra(KEY_INSTANCEID, instanceId)
                      .putExtra(KEY_DATE, date);
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
                      .putExtra(KEY_INSTANCEID, instanceId)
                      .putExtra(KEY_DATE, date);
                  builder.addAction(
                      android.R.drawable.ic_menu_save,
                      R.drawable.ic_menu_save,
                      getString(R.string.menu_apply_template),
                      PendingIntent.getService(this, notificationId, applyIntent, FLAG_UPDATE_CURRENT));
                  builder.setContentIntent(resultIntent);
                  notification = builder.build();
                  notification.flags |= Notification.FLAG_NO_CLEAR;
                }
              }
              notificationManager.notify(notificationId, notification);
            } else {
              log("Account.getInstanceFromDb returned null");
            }
          } else {
            log("Template.getInstanceForPlanIfInstanceIsOpen returned null, instance might already have been dealt with");
          }
          cursor.moveToNext();
        }
      }
      cursor.close();
    }

    PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP.putLong(now);

    setAlarm(this, ZonedDateTime.of(nextRun, ZoneId.systemDefault()).toEpochSecond() * 1000);

  }

  private void log(Exception e) {
    Timber.tag(TAG).w(e);
  }

  private void log(String message, Object... args) {
    Timber.tag(TAG).i(message, args);
  }

  public static void setAlarm(Context ctx, long when) {
    if (CALENDAR.hasPermission(ctx)) {
      PendingIntent pendingIntent = getPendingIntent(ctx);
      AlarmManager manager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
      manager.set(AlarmManager.RTC, when, pendingIntent);
    }
  }

  private static PendingIntent getPendingIntent(Context ctx) {
    return PendingIntent.getService(ctx, 0, new Intent(ctx, PlanExecutor.class), 0);
  }

  public static void cancelPlans(Context ctx) {
    PendingIntent pendingIntent = getPendingIntent(ctx);
    AlarmManager manager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
    manager.cancel(pendingIntent);
  }
}