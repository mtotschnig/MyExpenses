package org.totschnig.myexpenses.service;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.Distrib;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.vending.licensing.PreferenceObfuscator;

/**
 * This handler is used in two different scenarios:
 * 1) MyExpensesContrib calls MyExpenses.MyService when having retrieved the license status and posts a message to this handler
 * 2) MyExpenses.MyApplication onStartup calls MyExpensesContrib.MyService and sets this handler as replyTo to retrieve the license status
 * this handler is subclassed in MyApplication, so that we can handle unbinding from the service there
 */
public class UnlockHandler extends Handler {
  private static final int STATUS_RETRY = 2;
  private static final int STATUS_TEMPORARY = 3;
  private static final int STATUS_PERMANENT = 4;
  
  @Override
  public void handleMessage(Message msg) {
    MyApplication app = MyApplication.getInstance();
    Log.i(MyApplication.TAG,"Now handling answer from license verification service; got status "+msg.what);
    if (msg.what == STATUS_PERMANENT || msg.what == STATUS_TEMPORARY || msg.what == STATUS_RETRY) {
      PreferenceObfuscator mPreferences = Distrib.getLicenseStatusPrefs(app);
      app.setContribEnabled(true);
      //TODO show notification
      if (msg.what == STATUS_PERMANENT) {
        mPreferences.putString(MyApplication.PrefKey.LICENSE_STATUS.getKey(), "1");
        mPreferences.commit();
      }
      if (msg.what == STATUS_RETRY) {
        mPreferences.putString(MyApplication.PrefKey.LICENSE_RETRY_COUNT.getKey(),
            String.valueOf(Integer.parseInt(mPreferences.getString(
                MyApplication.PrefKey.LICENSE_RETRY_COUNT.getKey(),"0"))+1));
        mPreferences.commit();
      }
    }
  }
}
