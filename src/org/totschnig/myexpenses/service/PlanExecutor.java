package org.totschnig.myexpenses.service;

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.util.Log;
import android.widget.Toast;

public class PlanExecutor extends IntentService {

  public PlanExecutor() {
    super("PlanExexcutor");

  }

  @SuppressLint("NewApi")
  @Override
  public void onHandleIntent(Intent intent) {
    Log.i("DEBUG","Inside plan executor onHandleIntent");
    MyApplication app = MyApplication.getInstance();
    app.requirePlaner();
    long lastExecutionTimeStamp = app.getSettings().getLong(
        MyApplication.PREFKEY_PLANER_LAST_EXECUTION_TIMESTAMP, 0);
    long now = System.currentTimeMillis();
    if (lastExecutionTimeStamp == 0) {
      Log.i("DEBUG", "first call, nothting to do");
    } else {
      Log.i("DEBUG", String.format(
          "executing plans from %s to %s",
          new Date(lastExecutionTimeStamp).toString(),
          new Date(now).toString()));
      String[] INSTANCE_PROJECTION = new String[] {
          Instances.EVENT_ID,
          Instances._ID
        };
      Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI
          .buildUpon();
      ContentUris.appendId(eventsUriBuilder, lastExecutionTimeStamp);
      ContentUris.appendId(eventsUriBuilder, now);
      Uri eventsUri = eventsUriBuilder.build();
      //Instances.Content_URI returns events that fall totally or partially in a given range
      //we additionally select only instances where the begin is inside the range
      //because we want to deal with each instance only once
      Cursor cursor = getContentResolver().query(eventsUri, INSTANCE_PROJECTION,
          Events.CALENDAR_ID + " = ? AND "+ Instances.BEGIN + " BETWEEN ? AND ?",
          new String[]{
            String.valueOf(MyApplication.getInstance().planerCalenderId),
            String.valueOf(lastExecutionTimeStamp),
            String.valueOf(now)}, 
            null);
      if (cursor.moveToFirst()) {
        while (cursor.isAfterLast() == false) {
          long planId = cursor.getLong(0);
          long instanceId = cursor.getLong(1);
          //2) check if they are part of a plan linked to a template
          //3) execute the template
          Log.i("DEBUG",String.format("found instance %d of plan %d",instanceId,planId));
          Template te = Template.getInstanceForPlan(planId);
          //TODO show notification handle automatic and manual execution
          if (te != null)
            Transaction.getInstanceFromTemplate(te).save();
          cursor.moveToNext();
        }
      }
      cursor.close();
    }
    SharedPreferencesCompat.apply(app.getSettings().edit()
        .putLong(MyApplication.PREFKEY_PLANER_LAST_EXECUTION_TIMESTAMP, now));
  }


}