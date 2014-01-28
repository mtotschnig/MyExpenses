package org.totschnig.myexpenses.service;

import java.util.Date;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.model.Transaction;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

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
    String message;
    Bundle extras = intent.getExtras();
    String title = extras.getString("title");
    if (intent.getAction().equals("Apply")) {
      Long templateId = extras.getLong("template_id");
      try {
        Transaction t = Transaction.getInstanceFromTemplate(templateId);
        t.setDate(new Date(extras.getLong("instance_date")));
        if (t.save() == null)
          message = getString(R.string.save_transaction_error);
        else
          message = getResources().getQuantityString(
            R.plurals.save_transaction_from_template_success,1);
      } catch (DataObjectNotFoundException e) {
        message = getString(R.string.save_transaction_template_deleted);
      }
    } else {
      message = getString(R.string.plan_execution_canceled);
    }
    int notificationId = extras.getInt("notification_id");
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.myexpenses)
        .setContentTitle(title)
        .setContentText(message);
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(notificationId,builder.build());
  }
}
