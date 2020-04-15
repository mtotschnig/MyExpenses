package org.totschnig.myexpenses.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

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
  protected void onHandleIntent(@Nullable Intent intent) {
    String message;
    if (intent == null) return;
    final Bundle extras = intent.getExtras();
    final String action = intent.getAction();
    if (extras == null || action == null) return;
    String title = extras.getString(PlanExecutor.KEY_TITLE);
    NotificationBuilderWrapper builder = new NotificationBuilderWrapper(this,
        NotificationBuilderWrapper.CHANNEL_ID_PLANNER)
        .setSmallIcon(R.drawable.ic_stat_notification_sigma)
        .setContentTitle(title);
    int notificationId = extras.getInt(MyApplication.KEY_NOTIFICATION_ID);
    Long templateId = extras.getLong(DatabaseConstants.KEY_TEMPLATEID);
    Long instanceId = extras.getLong(DatabaseConstants.KEY_INSTANCEID);
    switch (action) {
      case PlanExecutor.ACTION_APPLY:
        Pair<Transaction, List<Tag>> pair = Transaction.getInstanceFromTemplate(templateId);
        if (pair == null) {
          message = getString(R.string.save_transaction_template_deleted);
        } else {
          Transaction t = pair.first;
          t.setDate(new Date(extras.getLong(DatabaseConstants.KEY_DATE)));
          t.originPlanInstanceId = instanceId;
          if (t.save(true) != null && t.saveTags(pair.second, getContentResolver())) {
            message = getResources().getQuantityString(
                R.plurals.save_transaction_from_template_success, 1, 1);
            Intent displayIntent = new Intent(this, MyExpenses.class)
                .putExtra(DatabaseConstants.KEY_ROWID, t.getAccountId())
                .putExtra(DatabaseConstants.KEY_TRANSACTIONID, t.getId());
            PendingIntent resultIntent = PendingIntent.getActivity(this, notificationId, displayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultIntent);
            builder.setAutoCancel(true);
          } else {
            message = getString(R.string.save_transaction_error);
          }
        }
        break;
      case PlanExecutor.ACTION_CANCEL:
        ContentValues values = new ContentValues();
        values.putNull(KEY_TRANSACTIONID);
        values.put(KEY_TEMPLATEID, templateId);
        values.put(KEY_INSTANCEID, instanceId);
        try {
          getContentResolver().insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
          message = getString(R.string.plan_execution_canceled);
        } catch (SQLiteConstraintException e) {
          message = getString(R.string.save_transaction_template_deleted);
        }
        break;
      default:
        return;
    }
    builder.setContentText(message);
    final NotificationManager systemService = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    if (systemService != null) {
      systemService.notify(notificationId, builder.build());
    }
  }
}
