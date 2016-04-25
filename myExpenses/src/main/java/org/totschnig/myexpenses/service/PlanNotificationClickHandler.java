package org.totschnig.myexpenses.service;

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

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
    String title = extras.getString(PlanExecutor.KEY_TITLE);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
    .setSmallIcon(R.drawable.ic_stat_planner)
    .setContentTitle(title);
    int notificationId = extras.getInt(MyApplication.KEY_NOTIFICATION_ID);
    Long templateId = extras.getLong(DatabaseConstants.KEY_TEMPLATEID);
    Long instanceId = extras.getLong(DatabaseConstants.KEY_INSTANCEID);
    if (intent.getAction().equals(PlanExecutor.ACTION_APPLY)) {
      Transaction t = Transaction.getInstanceFromTemplate(templateId);
      if (t==null) {
        message = getString(R.string.save_transaction_template_deleted);
      } else {
        t.setDate(new Date(extras.getLong(DatabaseConstants.KEY_DATE)));
        t.originPlanInstanceId = instanceId;
        if (t.save() == null)
          message = getString(R.string.save_transaction_error);
        else {
          message = getResources().getQuantityString(
            R.plurals.save_transaction_from_template_success,1);
          Intent displayIntent = new Intent(this, MyExpenses.class)
          .putExtra(DatabaseConstants.KEY_ROWID, t.accountId)
          .putExtra(DatabaseConstants.KEY_TRANSACTIONID, t.getId());
          PendingIntent resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultIntent);
        builder.setAutoCancel(true);
        }
      }
    } else if (intent.getAction().equals(PlanExecutor.ACTION_CANCEL)) {
      message = getString(R.string.plan_execution_canceled);
      ContentValues values = new ContentValues();
      values.putNull(KEY_TRANSACTIONID);
      values.put(KEY_TEMPLATEID, templateId);
      values.put(KEY_INSTANCEID, instanceId);
      getContentResolver().insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
    } else {
      return;
    }
    builder.setContentText(message);
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(notificationId,builder.build());
  }
}
