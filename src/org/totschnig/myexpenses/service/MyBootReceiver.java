package org.totschnig.myexpenses.service;

import org.totschnig.myexpenses.MyApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(MyApplication.TAG,"Boot broadcast received, calling initPlanner");
    MyApplication.getInstance().initPlanner();
  }
}