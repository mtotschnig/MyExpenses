package org.totschnig.myexpenses.widget;

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import java.util.Arrays;

import org.totschnig.myexpenses.R;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;

public abstract class AbstractWidget extends AppWidgetProvider {

  public AbstractWidget() {
    super();
  }
  
  abstract String getPrefName();
  abstract RemoteViews buildUpdateForOther(Context context, int widgetId, int layoutId, long objectId, String action);
  abstract RemoteViews buildUpdateForCurrent(Context context, int widgetId, int layoutId, long objectId);
  
  protected static final String WIDGET_NEXT_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_NEXT";
  protected static final String WIDGET_PREVIOUS_ACTION = "org.totschnig.myexpenses.UPDATE_WIDGET_PREVIOUS";
  protected static final String WIDGET_ID = "widgetId";
  protected static final String PREF_PREFIX_KEY = "prefix_";

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
          Log.d("MyExpensesWidget", "updateWidgets " + Arrays.toString(appWidgetIds) + " -> " + (action != null ? action : ""));
          for (int id : appWidgetIds) {
              AppWidgetProviderInfo appWidgetInfo = manager.getAppWidgetInfo(id);
              if (appWidgetInfo != null) {
                  int layoutId = appWidgetInfo.initialLayout;
                      long accountId = loadForWidget(context, id);
                      RemoteViews remoteViews = action != null || accountId == -1
                              ? buildUpdateForOther(context, id, layoutId, accountId, action)
                              : buildUpdateForCurrent(context, id, layoutId, accountId);
                      manager.updateAppWidget(id, remoteViews);
              }
          }
      }

  @Override
  public void onReceive(Context context, Intent intent) {
      Log.d("MyExpensesWidget", "onReceive intent "+intent);
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

}