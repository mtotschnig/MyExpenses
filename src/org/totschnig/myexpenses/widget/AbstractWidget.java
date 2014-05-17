package org.totschnig.myexpenses.widget;

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import java.util.Arrays;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Model;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public abstract class AbstractWidget<T extends Model> extends AppWidgetProvider {

  public AbstractWidget() {
    super();
  }
  
  abstract String getPrefName();
  abstract Uri getContentUri();
  abstract T getObject(long objectId);
  abstract T getObject(Cursor c);
  abstract Cursor getCursor(Context c);
  abstract RemoteViews updateWidgetFrom(Context context,
      int widgetId, int layoutId, T o);
  
  protected static final String WIDGET_NEXT_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_NEXT";
  protected static final String WIDGET_PREVIOUS_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_PREVIOUS";
  protected static final String WIDGET_ID = "widgetId";
  protected static final String PREF_PREFIX_KEY = "prefix_";
  protected static final int REQUEST_CODE_ADD_TRANSACTION = 0;
  protected static final int REQUEST_CODE_ADD_TRANSFER = 1;
  protected static final int REQUEST_CODE_INSTANCE_EDIT = 2;

  public static void updateWidgets(Context context,Class provider) {
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
    prefs.commit();
  }

  private void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds,
      String action) {
          Log.d("AbstractWidget", "updateWidgets " + Arrays.toString(appWidgetIds) + " -> " + (action != null ? action : ""));
          for (int id : appWidgetIds) {
              AppWidgetProviderInfo appWidgetInfo = manager.getAppWidgetInfo(id);
              if (appWidgetInfo != null) {
                  int layoutId = appWidgetInfo.initialLayout;
                      long objectId = loadForWidget(context, id);
                      Log.d("AbstractWidget", "loaded object id " + objectId);
                      RemoteViews remoteViews = action != null || objectId == -1
                              ? buildUpdateForOther(context, id, layoutId, objectId, action)
                              : buildUpdateForCurrent(context, id, layoutId, objectId);
                      manager.updateAppWidget(id, remoteViews);
              }
          }
      }

  @Override
  public void onReceive(Context context, Intent intent) {
      Log.d("AbstractWidget", "onReceive intent "+intent);
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
      updateWidgets(context, manager, appWidgetIds, null);
  }

  private RemoteViews errorUpdate(Context context) {
      RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_no_data);
      updateViews.setTextViewText(R.id.line1, "Error!");
      updateViews.setTextColor(R.id.line1, Color.RED);
      return updateViews;
  }

  protected  RemoteViews noDataUpdate(Context context) {
      return new RemoteViews(context.getPackageName(), R.layout.widget_no_data);
  }
  RemoteViews buildUpdateForCurrent(Context context,
      int widgetId, int layoutId, long objectId) {
    T o = getObject(objectId);
    if (o != null) {
      Log.d("AbstractWidget",
          "buildUpdateForCurrentAccount building update for " + widgetId
              + " -> " + objectId);
      return updateWidgetFrom(context, widgetId, layoutId, o);
    } else {
      Log.d("AbstractWidget", "buildUpdateForCurrentAccount not found "
          + widgetId + " -> " + objectId);
      return buildUpdateForOther(context, widgetId, layoutId, -1, null);
    }
  }
  RemoteViews buildUpdateForOther(Context context,
      int widgetId, int layoutId, long objectId, String action) {
    Cursor c = getCursor(context);
    T o;
    try {
      int count = c.getCount();
      Log.d("AbstractWidget", "count " + count);
      if (count > 0) {
        Log.d("AbstractWidget", "buildUpdateForOther " + widgetId
            + " -> " + objectId);
        if (count == 1 || objectId == -1) {
          if (c.moveToNext()) {
            o = getObject(c);
            return updateWidgetFrom(context, widgetId, layoutId, o);
          }
        } else {
          boolean found = false;
          Log.d("AbstractWidget", "looking for " + objectId);
          while (c.moveToNext()) {
            o = getObject(c);
            Log.d("AbstractWidget", "looking at " + o.id);
            if (o.id == objectId) {
              found = true;
              Log.d("AbstractWidget", "buildUpdateForOther found -> "
                  + objectId);
              if (action == WIDGET_PREVIOUS_ACTION) {
                if (!c.moveToPrevious()) {
                  c.moveToLast();
                }
                o = getObject(c);
                return updateWidgetFrom(context, widgetId, layoutId, o);
              }
            } else {
              if (found) {
                Log.d("AbstractWidget",
                    "buildUpdateForOther building update for -> " + o.id);
                return updateWidgetFrom(context, widgetId, layoutId, o);
              }
            }
          }
          c.moveToFirst();
          o = getObject(c);
          Log.d("AbstractWidget",
              "buildUpdateForOther not found, taking the first one -> "
                  + o.id);
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
    Uri widgetUri = ContentUris.withAppendedId(getContentUri(), widgetId);
    Intent intent = new Intent(WIDGET_NEXT_ACTION, widgetUri, context,
        getClass());
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.down_icon, pendingIntent);
    intent = new Intent(WIDGET_PREVIOUS_ACTION, widgetUri, context,
        getClass());
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.up_icon, pendingIntent);
  }
}