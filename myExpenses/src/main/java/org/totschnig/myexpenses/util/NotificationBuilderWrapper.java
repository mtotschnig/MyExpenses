package org.totschnig.myexpenses.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;

import org.totschnig.myexpenses.R;

import androidx.core.app.NotificationCompat;

public class NotificationBuilderWrapper {
  public static int NOTIFICATION_SYNC = -1;
  public static int NOTIFICATION_AUTO_BACKUP = -2;
  public static int NOTIFICATION_CONTRIB = -3;
  public static int NOTIFICATION_WEB_UI = -4;
  public static String CHANNEL_ID_SYNC = "sync";
  public static String CHANNEL_ID_PLANNER = "planner";
  public static String CHANNEL_ID_DEFAULT = "default";
  private Context context;
  private Notification.Builder api23Builder;
  private NotificationCompat.Builder compatBuilder;

  @TargetApi(Build.VERSION_CODES.O)
  public static void createChannels(Context context) {
    if (shouldUseChannel()) {
      NotificationManager mNotificationManager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel syncChannel = new NotificationChannel(CHANNEL_ID_SYNC, context.getString(R.string.synchronization),
          importance);
      syncChannel.setSound(null, null);
      mNotificationManager.createNotificationChannel(syncChannel);
      mNotificationManager.createNotificationChannel(
          new NotificationChannel(CHANNEL_ID_PLANNER, context.getString(R.string.planner_notification_channel_name),
              importance));
      mNotificationManager.createNotificationChannel(
          new NotificationChannel(CHANNEL_ID_DEFAULT, context.getString(R.string.app_name),
              importance));
    }
  }

  public static NotificationBuilderWrapper defaultBigTextStyleBuilder(
      Context context, String title, CharSequence content) {
    return bigTextStyleBuilder(context, CHANNEL_ID_DEFAULT, title, content);
  }

  public static NotificationBuilderWrapper bigTextStyleBuilder(
      Context context, String channel, String title, CharSequence content) {
    return new NotificationBuilderWrapper(context, channel)
        .setSmallIcon(R.drawable.ic_stat_notification_sigma)
        .setContentTitle(title)
        .setBigContentText(content);
  }

  private NotificationBuilderWrapper setBigContentText(CharSequence content) {
    setContentText(content);
    if (shouldUseNative()) {
      api23Builder.setStyle(new Notification.BigTextStyle().bigText(content));
    } else {
      compatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
    }
    return this;
  }

  private static boolean shouldUseNative() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  private static boolean shouldUseChannel() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  public NotificationBuilderWrapper(Context context, String channel) {
    this.context = context;
    if (shouldUseNative()) {
      this.api23Builder = shouldUseChannel() ? new Notification.Builder(context, channel) :
          new Notification.Builder(context);
    } else {
      this.compatBuilder = new NotificationCompat.Builder(context);
    }
  }

  public NotificationBuilderWrapper setSmallIcon(int smallIcon) {
    if (shouldUseNative()) {
      api23Builder.setSmallIcon(smallIcon);
    } else {
      compatBuilder.setSmallIcon(smallIcon);
    }
    return this;
  }

  public NotificationBuilderWrapper setContentTitle(String title) {
    if (shouldUseNative()) {
      api23Builder.setContentTitle(title);
    } else {
      compatBuilder.setContentTitle(title);
    }
    return this;
  }

  public NotificationBuilderWrapper setContentText(CharSequence content) {
    if (shouldUseNative()) {
      api23Builder.setContentText(content);
    } else {
      compatBuilder.setContentText(content);
    }
    return this;
  }

  public NotificationBuilderWrapper setContentIntent(PendingIntent contentIntent) {
    if (shouldUseNative()) {
      api23Builder.setContentIntent(contentIntent);
    } else {
      compatBuilder.setContentIntent(contentIntent);
    }
    return this;
  }

  public NotificationBuilderWrapper setDeleteIntent(PendingIntent deleteIntent) {
    if (shouldUseNative()) {
      api23Builder.setDeleteIntent(deleteIntent);
    } else {
      compatBuilder.setDeleteIntent(deleteIntent);
    }
    return this;
  }

  public NotificationBuilderWrapper setAutoCancel(boolean autoCancel) {
    if (shouldUseNative()) {
      api23Builder.setAutoCancel(autoCancel);
    } else {
      compatBuilder.setAutoCancel(autoCancel);
    }
    return this;
  }

  public NotificationBuilderWrapper setWhen(long when) {
    if (shouldUseNative()) {
      api23Builder.setWhen(when);
      api23Builder.setShowWhen(true);
    } else {
      compatBuilder.setWhen(when);
      compatBuilder.setShowWhen(true);
    }
    return this;
  }

  public NotificationBuilderWrapper addAction(int iconCompat, int iconApi23, String title, PendingIntent intent) {
    if (shouldUseNative()) {
      api23Builder.addAction(new Notification.Action.Builder(
          //the icon is only shown on API 23, starting with Nougat notification actions only show text. Hence light background is ok
          iconApi23 == 0 ? null : Icon.createWithBitmap(UiUtils.getTintedBitmapForTheme(context, iconApi23, R.style.LightBackground)),
          title, intent).build());
    } else {
      compatBuilder.addAction(iconCompat, title, intent);
    }
    return this;
  }

  public Notification build() {
    if (shouldUseNative()) {
      return api23Builder.build();
    } else {
      return compatBuilder.build();
    }
  }
}
