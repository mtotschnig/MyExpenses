package org.totschnig.myexpenses.service;

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

  public PlanExecutor() {
    super("PlanExexcutor");
  }

  @Override
  public void onHandleIntent(Intent intent) {
    Log.i("DEBUG","Inside plan executor onHandleIntent");
    String planerCalendarId = MyApplication.getInstance().requirePlaner();
    if (planerCalendarId.equals("-1")) {
      return;
    }
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    long lastExecutionTimeStamp = settings.getLong(
        MyApplication.PREFKEY_PLANER_LAST_EXECUTION_TIMESTAMP, 0);
    long now = System.currentTimeMillis();
    if (lastExecutionTimeStamp == 0) {
      Log.i("DEBUG", "first call, nothting to do");
    } else {
      Log.i("DEBUG", String.format(
          "executing plans from %d to %d",
          lastExecutionTimeStamp,
          now));
      String[] INSTANCE_PROJECTION = new String[] {
          Instances.EVENT_ID,
          Instances._ID
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
          Events.CALENDAR_ID + " = " + planerCalendarId + " AND "+ Instances.BEGIN +
              " BETWEEN " + lastExecutionTimeStamp + " AND " + now,
          null,
          null);

      if (cursor.moveToFirst()) {
        while (cursor.isAfterLast() == false) {
          long planId = cursor.getLong(0);
          Long instanceId = cursor.getLong(1);
          //2) check if they are part of a plan linked to a template
          //3) execute the template
          Log.i("DEBUG",String.format("found instance %d of plan %d",instanceId,planId));
          Template template = Template.getInstanceForPlan(planId);
          if (template != null) {
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
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(title)
                    .setContentText(content);
            if (template.planExecutionAutomatic) {
              Uri uri = Transaction.getInstanceFromTemplate(template).save();
              long id = ContentUris.parseId(uri);
              Intent displayIntent = new Intent(this, MyExpenses.class);
              displayIntent.putExtra(DatabaseConstants.KEY_ROWID, template.accountId);
              displayIntent.putExtra("transaction_id", id);
              resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT);
              builder.setContentIntent(resultIntent);
              builder.setAutoCancel(true);
              notification = builder.build();
            } else {
              Intent cancelIntent = new Intent(this, PlanNotificationClickHandler.class);
              cancelIntent.setAction("Cancel");
              cancelIntent.putExtra("notification_id", notificationId);
              //we also put the title in the intent, because we need it while we update the notification
              cancelIntent.putExtra("title", title);
              builder.addAction(
                  android.R.drawable.ic_menu_close_clear_cancel,
                  getString(android.R.string.cancel),
                  PendingIntent.getService(this, notificationId, cancelIntent, 0));
              Intent editIntent = new Intent(this,ExpenseEdit.class);
              editIntent.putExtra("notification_id", notificationId);
              editIntent.putExtra("template_id", template.id);
              editIntent.putExtra("instantiate", true);
              resultIntent = PendingIntent.getActivity(this, notificationId, editIntent, 0);
              builder.addAction(
                  android.R.drawable.ic_menu_edit,
                  getString(R.string.menu_edit),
                  resultIntent);
              Intent applyIntent = new Intent(this, PlanNotificationClickHandler.class);
              applyIntent.setAction("Apply");
              applyIntent.putExtra("notification_id", notificationId);
              applyIntent.putExtra("title", title);
              applyIntent.putExtra("template_id", template.id);
              builder.addAction(
                  android.R.drawable.ic_menu_save,
                  getString(R.string.menu_apply),
                  PendingIntent.getService(this, notificationId, applyIntent, 0));
              builder.setContentIntent(resultIntent);
              notification = builder.build();
              notification.flags |= Notification.FLAG_NO_CLEAR;
            }
            Log.i("DEBUG","notifying with id " + notificationId);
            mNotificationManager.notify(notificationId, notification);
          }
          cursor.moveToNext();
        }
      }
      cursor.close();
    }
    SharedPreferencesCompat.apply(settings.edit()
        .putLong(MyApplication.PREFKEY_PLANER_LAST_EXECUTION_TIMESTAMP, now));
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
    AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
    long interval = 21600000; //6* 60 * 60 * 1000 6 hours
    //long interval = 60000; // 1 minute
    manager.set(AlarmManager.RTC, now+interval, 
        pendingIntent);
  }
}