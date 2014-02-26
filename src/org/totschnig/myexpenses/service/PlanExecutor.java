package org.totschnig.myexpenses.service;

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendar.CalendarContractCompat.Instances;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PlanExecutor extends IntentService {
  //production: 21600000 6* 60 * 60 * 1000 6 hours; for testing: 60000 1 minute
  public static long INTERVAL = MyApplication.debug ? 60000 : 21600000;
  public PlanExecutor() {
    super("PlanExexcutor");
  }

  @Override
  public void onHandleIntent(Intent intent) {
    String plannerCalendarId = MyApplication.getInstance().checkPlanner();
    if (plannerCalendarId.equals("-1")) {
      Log.i(MyApplication.TAG,"PlanExecutor: no planner set, nothing to do");
      return;
    }
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    long lastExecutionTimeStamp = settings.getLong(
        MyApplication.PREFKEY_PLANNER_LAST_EXECUTION_TIMESTAMP, 0);
    long now = System.currentTimeMillis();
    if (lastExecutionTimeStamp == 0) {
      Log.i(MyApplication.TAG, "PlanExecutor started first time, nothing to do");
    } else {
      Log.i(MyApplication.TAG, String.format(
          "executing plans from %d to %d",
          lastExecutionTimeStamp,
          now));
      String[] INSTANCE_PROJECTION = new String[] {
          Instances.EVENT_ID,
          Instances._ID,
          Instances.BEGIN
        };
      Uri.Builder eventsUriBuilder = CalendarContractCompat.Instances.CONTENT_URI
          .buildUpon();
      ContentUris.appendId(eventsUriBuilder, lastExecutionTimeStamp);
      ContentUris.appendId(eventsUriBuilder, now);
      Uri eventsUri = eventsUriBuilder.build();
      //Instances.Content_URI returns events that fall totally or partially in a given range
      //we additionally select only instances where the begin is inside the range
      //because we want to deal with each instance only once
      //the calendar content provider on Android < 4 does not interpret the selection arguments
      //hence we put them into the selection
      Cursor cursor = getContentResolver().query(eventsUri, INSTANCE_PROJECTION,
          Events.CALENDAR_ID + " = " + plannerCalendarId + " AND "+ Instances.BEGIN +
              " BETWEEN " + lastExecutionTimeStamp + " AND " + now,
          null,
          null);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          while (cursor.isAfterLast() == false) {
            long planId = cursor.getLong(0);
            Long instanceId = cursor.getLong(1);
            long date = cursor.getLong(2);
            //2) check if they are part of a plan linked to a template
            //3) execute the template
            Log.i(MyApplication.TAG,String.format("found instance %d of plan %d",instanceId,planId));
            //TODO if we have multiple Event instances for one plan, we should maybe cache the template objects
            //TODO we should set the date of the Event instance on the created transactions
            Template template = Template.getInstanceForPlanIfInstanceIsOpen(planId,instanceId);
            if (template != null) {
              Log.i(MyApplication.TAG,String.format("belongs to template %d",template.id));
              Notification notification;
              int notificationId = instanceId.hashCode();
              PendingIntent resultIntent;
              Account account = Account.getInstanceFromDb(template.accountId);
              NotificationManager mNotificationManager =
                  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
              String content = template.label;
              if (!content.equals(""))
                content += " : ";
              content += Utils.formatCurrency(template.amount);
              String title = account.label + " : " + template.title;
              NotificationCompat.Builder builder =
                  new NotificationCompat.Builder(this)
                      .setSmallIcon(R.drawable.ic_stat_planner)
                      .setContentTitle(title)
                      .setContentText(content);
              if (template.planExecutionAutomatic) {
                Transaction t = Transaction.getInstanceFromTemplate(template);
                t.setDate(new Date(date));
                Uri uri = t.save();
                long id = ContentUris.parseId(uri);
                Intent displayIntent = new Intent(this, MyExpenses.class)
                  .putExtra(DatabaseConstants.KEY_ROWID, template.accountId)
                  .putExtra("transaction_id", id);
                resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(resultIntent);
                builder.setAutoCancel(true);
                notification = builder.build();
              } else {
                Intent cancelIntent = new Intent(this, PlanNotificationClickHandler.class)
                  .setAction("Cancel")
                  .putExtra("notification_id", notificationId)
                  //we also put the title in the intent, because we need it while we update the notification
                  .putExtra("title", title);
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(android.R.string.cancel),
                    PendingIntent.getService(this, notificationId, cancelIntent, 0));
                Intent editIntent = new Intent(this,ExpenseEdit.class)
                  .putExtra("notification_id", notificationId)
                  .putExtra("template_id", template.id)
                  .putExtra("instance_id", -1L)
                  .putExtra("instance_date", date);
                resultIntent = PendingIntent.getActivity(this, notificationId, editIntent, 0);
                builder.addAction(
                    android.R.drawable.ic_menu_edit,
                    getString(R.string.menu_edit),
                    resultIntent);
                Intent applyIntent = new Intent(this, PlanNotificationClickHandler.class);
                applyIntent.setAction("Apply")
                  .putExtra("notification_id", notificationId)
                  .putExtra("title", title)
                  .putExtra("template_id", template.id)
                  .putExtra("instance_date", date);
                builder.addAction(
                    android.R.drawable.ic_menu_save,
                    getString(R.string.menu_apply_template),
                    PendingIntent.getService(this, notificationId, applyIntent, 0));
                builder.setContentIntent(resultIntent);
                notification = builder.build();
                notification.flags |= Notification.FLAG_NO_CLEAR;
              }
              mNotificationManager.notify(notificationId, notification);
            } else {
              Log.i(MyApplication.TAG,"Template.getInstanceForPlanIfInstanceIsOpen returned null, instance might already have been dealt with");
            }
            cursor.moveToNext();
          }
        }
        cursor.close();
      }
    }
    SharedPreferencesCompat.apply(settings.edit()
        .putLong(MyApplication.PREFKEY_PLANNER_LAST_EXECUTION_TIMESTAMP, now));
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
    AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
    manager.set(AlarmManager.RTC, now + INTERVAL, 
        pendingIntent);
  }
}