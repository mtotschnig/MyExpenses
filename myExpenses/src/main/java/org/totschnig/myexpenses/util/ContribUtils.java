package org.totschnig.myexpenses.util;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.model.ContribFeature;

import static org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_CONTRIB;

public class ContribUtils {
  public static void showContribNotification(Context context, ContribFeature feature) {
    String notifTitle = TextUtils.concatResStrings(context, " ", R.string.app_name,
        feature.getLabelResIdOrThrow(context));
    CharSequence content = android.text.TextUtils.concat(
        feature.getLimitReachedWarning(context), " ",
        feature.buildRemoveLimitation(context, true));

    Intent contribIntent = ContribInfoDialogActivity.getIntentFor(context, feature);
    NotificationBuilderWrapper builder =
        NotificationBuilderWrapper.defaultBigTextStyleBuilder(context, notifTitle, content)
            .setContentIntent(PendingIntent.getActivity(context, 0, contribIntent,
                PendingIntent.FLAG_CANCEL_CURRENT));
    Notification notification = builder.build();
    notification.flags = Notification.FLAG_AUTO_CANCEL;
    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(NOTIFICATION_CONTRIB, notification);
  }
}
