package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import javax.inject.Inject

abstract class BaseWidget(private val protectionKey: PrefKey) : AppWidgetProvider() {

    @Inject
    lateinit var prefHandler: PrefHandler

    protected open fun isProtected(context: Context): Boolean {
        return (context.myApplication).isProtected &&
                !prefHandler.getBoolean(protectionKey, false)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    abstract fun updateWidgetDo(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    open fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {

        if (isProtected(context))
            appWidgetManager.updateAppWidget(appWidgetId,
                RemoteViews(context.packageName, R.layout.widget_locked).apply {
                    setOnClickPendingIntent(
                        R.id.text,
                        PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            PreferenceActivity.getIntent(
                                context, prefHandler.getKey(PrefKey.CATEGORY_SECURITY)
                            ),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    setTextViewText(
                        R.id.text, context.getString(R.string.warning_password_protected) + " " +
                                context.getString(R.string.warning_widget_disabled)
                    )
                })
        else updateWidgetDo(context, appWidgetManager, appWidgetId)
    }

    fun availableWidth(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else -> AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        }, Int.MAX_VALUE
    )

    fun availableHeight(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            else -> AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        }, Int.MAX_VALUE
    )
}