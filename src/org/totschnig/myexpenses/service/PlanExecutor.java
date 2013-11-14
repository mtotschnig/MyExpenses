package org.totschnig.myexpenses.service;

import java.util.Calendar;

import org.totschnig.myexpenses.MyApplication;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.util.Log;
import android.widget.Toast;

public class PlanExecutor extends Service {


  @Override
  public IBinder onBind(Intent arg0) {
      return null;
  }

  @SuppressLint("NewApi")
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    MyApplication.getInstance().requirePlaner();
    //1) get all event instances for the current date
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.set(Calendar.HOUR_OF_DAY, 0); //set hours to zero
    cal.set(Calendar.MINUTE, 0); // set minutes to zero
    cal.set(Calendar.SECOND, 0); //set seconds to zero
    cal.set(Calendar.MILLISECOND, 0);
    Log.i("Start of Day ", cal.getTime().toString());
    long startOfDay = cal.getTimeInMillis();
    String[] INSTANCE_PROJECTION = new String[] {
        Instances.EVENT_ID,      // 0
      };
    Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI
        .buildUpon();
    ContentUris.appendId(eventsUriBuilder, startOfDay);
    ContentUris.appendId(eventsUriBuilder, startOfDay+86400000);
    Uri eventsUri = eventsUriBuilder.build();
    Cursor cursor = getContentResolver().query(eventsUri, INSTANCE_PROJECTION,
        Events.CALENDAR_ID + " = ?",
        new String[]{String.valueOf(MyApplication.getInstance().planerCalenderId)}, null);
    if (cursor.moveToFirst()) {
      while (cursor.isAfterLast() == false) {
        Log.i("DEBUG","found instance of plan "+cursor.getLong(0));
        cursor.moveToNext();
      }
    }
    //2) check if they are part of a plan linked to a template
    //3) execute the template
    return Service.START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
      super.onDestroy();
      Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onCreate() {
      super.onCreate();
  }

}