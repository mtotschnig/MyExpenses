package org.totschnig.myexpenses.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.UiUtils;

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

public abstract class AbstractWidget<T extends Model> extends AppWidgetProvider {

  public AbstractWidget() {
    super();
  }

  abstract String getPrefName();

  abstract PrefKey getProtectionKey();

  abstract T getObject(Cursor c);

  abstract Cursor getCursor(Context c);

  protected RemoteViews updateWidgetFrom(Context context, int widgetId, int layoutId, T o) {
    RemoteViews updateViews = new RemoteViews(context.getPackageName(), layoutId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      float dimension = context.getResources().getDimension(getFontSizeResId());
      updateViews.setTextViewTextSize(R.id.line1, TypedValue.COMPLEX_UNIT_PX, dimension);
      updateViews.setTextViewTextSize(R.id.note, TypedValue.COMPLEX_UNIT_PX, dimension);
    }
    return updateViews;
  }

  protected static final String WIDGET_NEXT_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_NEXT";
  protected static final String WIDGET_PREVIOUS_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_PREVIOUS";
  public static final String EXTRA_START_FROM_WIDGET = "startFromWidget";
  public static final String EXTRA_START_FROM_WIDGET_DATA_ENTRY = "startFromWidgetDataEntry";
  protected static final String WIDGET_ID = "widgetId";
  protected static final String PREF_PREFIX_KEY = "prefix_";

  public static void updateWidgets(Context context, Class<? extends AbstractWidget<?>> provider) {
    Intent i = new Intent(context, provider);
    i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    int[] widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, provider));
    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
    context.sendBroadcast(i);
  }

  protected long loadForWidget(Context context, int widgetId) {
    SharedPreferences prefs = context.getSharedPreferences(getPrefName(), 0);
    return prefs.getLong(PREF_PREFIX_KEY + widgetId, -1);
  }

  protected void saveForWidget(Context context, int widgetId,
                               long objectId) {
    SharedPreferences.Editor prefs = context
        .getSharedPreferences(getPrefName(), 0).edit();
    prefs.putLong(PREF_PREFIX_KEY + widgetId, objectId);
    prefs.apply();
  }

  protected boolean isProtected() {
    return MyApplication.getInstance().isProtected() &&
        !getProtectionKey().getBoolean(false);
  }

  protected void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds,
                               String action) {
    boolean isProtected = isProtected();
    for (int id : appWidgetIds) {
      AppWidgetProviderInfo appWidgetInfo = manager.getAppWidgetInfo(id);
      if (appWidgetInfo != null) {
        RemoteViews remoteViews;
        if (isProtected) {
          remoteViews = protectedUpdate(context);
        } else {
          int layoutId = appWidgetInfo.initialLayout;
          long objectId = loadForWidget(context, id);
          remoteViews = buildUpdate(context, id, layoutId, objectId, action);
        }
        manager.updateAppWidget(id, remoteViews);
      }
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (WIDGET_NEXT_ACTION.equals(action) || WIDGET_PREVIOUS_ACTION.equals(action)) {
      int widgetId = intent.getIntExtra(WIDGET_ID, INVALID_APPWIDGET_ID);
      if (widgetId != INVALID_APPWIDGET_ID) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateWidgets(context, manager, new int[]{widgetId}, action);
      }
    } else {
      super.onReceive(context, intent);
    }
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
    updateWidgets(context, manager, appWidgetIds, AppWidgetManager.ACTION_APPWIDGET_UPDATE);
  }


  protected RemoteViews errorUpdate(Context context, String message) {
    RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_no_data);
    updateViews.setTextViewText(R.id.object_info, message);
    updateViews.setTextColor(R.id.object_info, Color.RED);
    return updateViews;
  }

  protected RemoteViews protectedUpdate(Context context) {
    String message = context.getString(R.string.warning_password_protected) + " " +
        context.getString(R.string.warning_widget_disabled);
    return errorUpdate(context, message);
  }

  protected RemoteViews noDataUpdate(Context context) {
    return new RemoteViews(context.getPackageName(), R.layout.widget_no_data);
  }

  RemoteViews buildUpdate(Context context,
                          int widgetId, int layoutId, long objectId, String action) {
    Cursor c = getCursor(context);
    T o;
    try {
      int count = c.getCount();
      if (count > 0) {
        if (count == 1 || objectId == -1) {
          if (c.moveToNext()) {
            o = getObject(c);
            return updateWidgetFrom(context, widgetId, layoutId, o);
          }
        } else {
          boolean found = false;
          while (c.moveToNext()) {
            o = getObject(c);
            if (o.getId() == objectId) {
              found = true;
              if (action.equals(WIDGET_NEXT_ACTION)) {
                continue;
              }
              if (action.equals(WIDGET_PREVIOUS_ACTION) && !c.moveToPrevious()) {
                c.moveToLast();
              }
              o = getObject(c);
              return updateWidgetFrom(context, widgetId, layoutId, o);
            } else {
              if (found) {
                return updateWidgetFrom(context, widgetId, layoutId, o);
              }
            }
          }
          c.moveToFirst();
          o = getObject(c);
          return updateWidgetFrom(context, widgetId, layoutId, o);
        }
      }
      return noDataUpdate(context);
    } finally {
      c.close();
    }
  }

  protected void addScrollOnClick(Context context,
                                  RemoteViews updateViews, int widgetId) {
    Intent intent = new Intent(WIDGET_NEXT_ACTION, null, context,
        getClass());
    intent.putExtra(WIDGET_ID, widgetId);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId,
        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.down_icon, pendingIntent);
    intent = new Intent(WIDGET_PREVIOUS_ACTION, null, context,
        getClass());
    intent.putExtra(WIDGET_ID, widgetId);
    pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.up_icon, pendingIntent);
  }

  protected void setBackgroundColorSave(RemoteViews updateViews, int res, int color) {
    updateViews.setInt(res, "setBackgroundColor", color);
  }

  //http://stackoverflow.com/a/35633411/1199911
  protected void setImageViewVectorDrawable(Context context, RemoteViews remoteViews,
                                            int viewId, int resId) {
    remoteViews.setImageViewBitmap(viewId, UiUtils.getTintedBitmapForTheme(context, resId,
        R.style.ThemeDark));
  }

  private @DimenRes int getFontSizeResId() {
    switch(PrefKey.UI_FONTSIZE.getInt(0)) {
      case 3: return R.dimen.textSizeSmallS3;
      case 2: return R.dimen.textSizeSmallS2;
      case 1: return R.dimen.textSizeSmallS1;
      default: return R.dimen.textSizeSmallBase;
    }
  }
}