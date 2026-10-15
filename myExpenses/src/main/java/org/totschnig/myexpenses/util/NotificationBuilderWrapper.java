package org.totschnig.myexpenses.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.ui.UiUtils;

public class NotificationBuilderWrapper {
  public static int NOTIFICATION_AUTO_BACKUP = -2;
  public static int NOTIFICATION_CONTRIB = -3;
  public static int NOTIFICATION_WEB_UI = -4;

  public static int NOTIFICATION_PLANNER_ERROR = -5;
  public static int NOTIFICATION_EXCHANGE_RATE_DOWNLOAD_ERROR = -6;
  public static String CHANNEL_ID_SYNC = "sync";
  public static String CHANNEL_ID_PLANNER = "planner";
  public static String CHANNEL_ID_DEFAULT = "default";
  public static String CHANNEL_ID_AUTO_BACKUP = "autoBackup";
  private final Context context;
  private final Notification.Builder api23Builder;

    public static void createChannels(Context context) {
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
      mNotificationManager.createNotificationChannel(
              new NotificationChannel(CHANNEL_ID_AUTO_BACKUP, context.getString(R.string.pref_auto_backup_title),
                      importance));
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
      api23Builder.setStyle(new Notification.BigTextStyle().bigText(content));
      return this;
  }

  public NotificationBuilderWrapper(Context context, String channel) {
    this.context = context;
      this.api23Builder = new Notification.Builder(context, channel);
  }

  public NotificationBuilderWrapper setSmallIcon(int smallIcon) {
      api23Builder.setSmallIcon(smallIcon);
      return this;
  }

  public NotificationBuilderWrapper setContentTitle(String title) {
      api23Builder.setContentTitle(title);
      return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public NotificationBuilderWrapper setContentText(CharSequence content) {
      api23Builder.setContentText(content);
      return this;
  }

  public NotificationBuilderWrapper setContentIntent(PendingIntent contentIntent) {
      api23Builder.setContentIntent(contentIntent);
      return this;
  }

  public NotificationBuilderWrapper setDeleteIntent(PendingIntent deleteIntent) {
      api23Builder.setDeleteIntent(deleteIntent);
      return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public NotificationBuilderWrapper setAutoCancel(boolean autoCancel) {
      api23Builder.setAutoCancel(autoCancel);
      return this;
  }

  public NotificationBuilderWrapper setWhen(long when) {
      api23Builder.setWhen(when);
      api23Builder.setShowWhen(true);
      return this;
  }

  public NotificationBuilderWrapper addAction(int icon, String title, PendingIntent intent) {
      api23Builder.addAction(new Notification.Action.Builder(
              //the icon is only shown on API 23, starting with Nougat notification actions only show text. Hence light background is ok
              icon == 0 ? null : Icon.createWithBitmap(UiUtils.getTintedBitmapForTheme(context, icon, R.style.LightBackground)),
              title, intent).build());
      return this;
  }

  public Notification build() {
      return api23Builder.build();
  }
}
