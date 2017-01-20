package org.totschnig.myexpenses.util;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.model.ContribFeature;

public class ContribUtils {
  public static void showContribNotification(Context context, ContribFeature feature) {
    String notifTitle = Utils.concatResStrings(context, " ", R.string.app_name,
        feature.getLabelResIdOrThrow(context));
    CharSequence content = android.text.TextUtils.concat(
        context.getText(feature.getLimitReachedWarningResIdOrThrow(context)), " ",
        feature.buildRemoveLimitation(context, true));
    Intent contribIntent = new Intent(context, ContribInfoDialogActivity.class);
    contribIntent.putExtra(ContribInfoDialogActivity.KEY_FEATURE, feature);
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_home_dark)
            .setContentTitle(notifTitle)
            .setContentText(content)
            .setContentIntent(PendingIntent.getActivity(context, 0, contribIntent, PendingIntent.FLAG_CANCEL_CURRENT))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
    Notification notification = builder.build();
    notification.flags = Notification.FLAG_AUTO_CANCEL;
    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);

  }
}
