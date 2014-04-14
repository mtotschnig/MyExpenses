package org.totschnig.myexpenses.service;

import java.util.Date;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
    .setSmallIcon(R.drawable.ic_stat_planner)
    .setContentTitle(title);
    int notificationId = extras.getInt("notification_id");
    if (intent.getAction().equals("Apply")) {
      Long templateId = extras.getLong("template_id");
      Transaction t = Transaction.getInstanceFromTemplate(templateId);
      if (t==null) {
        message = getString(R.string.save_transaction_template_deleted);
      } else {
        t.setDate(new Date(extras.getLong("instance_date")));
        if (t.save() == null)
          message = getString(R.string.save_transaction_error);
        else {
          message = getResources().getQuantityString(
            R.plurals.save_transaction_from_template_success,1);
          Intent displayIntent = new Intent(this, MyExpenses.class)
          .putExtra(DatabaseConstants.KEY_ROWID, t.accountId)
          .putExtra("transaction_id", t.id);
          PendingIntent resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultIntent);
        builder.setAutoCancel(true);
        }
      }
    } else {
      message = getString(R.string.plan_execution_canceled);
    }
    builder.setContentText(message);
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(notificationId,builder.build());
  }
}
