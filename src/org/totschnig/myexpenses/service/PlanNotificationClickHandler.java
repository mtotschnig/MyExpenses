package org.totschnig.myexpenses.service;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PlanNotificationClickHandler extends IntentService {
  public PlanNotificationClickHandler() {
    super("PlanNotificationClickHandler");
  }
  @Override
  public void onCreate() {
      super.onCreate();
  }
  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i("DEBUG","Inside PlanNotificationClickHandler onHandleIntent");
    final int message;
    String title = intent.getExtras().getString("title");
    if (intent.getAction().equals("Apply")) {
      Long templateId = intent.getExtras().getLong("template_id");
      if (Transaction.getInstanceFromTemplate(templateId).save() == null)
        message = R.string.save_transaction_error;
      else
        message = R.string.save_transaction_from_template_success;
    } else {
      message = R.string.plan_execution_canceled;
    }
    int notificationId = intent.getExtras().getInt("notification_id");
    Log.i("DEBUG","updating with id " + notificationId);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.icon)
        .setContentTitle(title)
        .setContentText(getString(message));
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(notificationId,builder.build());
  }
}
