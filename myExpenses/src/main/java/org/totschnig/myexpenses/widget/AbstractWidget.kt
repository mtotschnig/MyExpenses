package org.totschnig.myexpenses.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.WindowManager
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import javax.inject.Inject

const val WIDGET_CLICK = "org.totschnig.myexpenses.WIDGET_CLICK"
const val KEY_CLICK_ACTION = "clickAction"
const val WIDGET_LIST_DATA_CHANGED = "org.totschnig.myexpenses.LIST_DATA_CHANGED"
const val WIDGET_CONTEXT_CHANGED = "org.totschnig.myexpenses.CONTEXT_CHANGED"
const val EXTRA_START_FROM_WIDGET = "startFromWidget"
const val EXTRA_START_FROM_WIDGET_DATA_ENTRY = "startFromWidgetDataEntry"
const val KEY_WIDTH = "width"

fun onConfigurationChanged(context: Context) {
    updateWidgets(context, AccountWidget::class.java, WIDGET_CONTEXT_CHANGED)
    updateWidgets(context, TemplateWidget::class.java, WIDGET_CONTEXT_CHANGED)
}

fun updateWidgets(
    context: Context, provider: Class<out AppWidgetProvider?>, action: String,
    appWidgetIds: IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, provider))
) =
    context.sendBroadcast(Intent(context, provider).apply {
        this.action = action
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
    })

abstract class AbstractWidget(
    private val clazz: Class<out RemoteViewsService>,
    private val protectionKey: PrefKey
) : AppWidgetProvider() {
    abstract val emptyTextResourceId: Int

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as MyApplication).appComponent.inject(this)
        val instance = AppWidgetManager.getInstance(context)
        val appWidgetIds = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        when (intent.action) {
            WIDGET_LIST_DATA_CHANGED -> {
                appWidgetIds?.let { instance.notifyAppWidgetViewDataChanged(it, R.id.list) }
            }
            WIDGET_CONTEXT_CHANGED -> {
                appWidgetIds?.let { onUpdate(context, instance, it) }
            }
            WIDGET_CLICK -> {
                handleWidgetClick(context, intent)
            }
            else -> {
                super.onReceive(context, intent)
            }
        }
    }

    abstract fun handleWidgetClick(context: Context, intent: Intent)

    @Suppress("DEPRECATION")
    fun availableWidth(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): Int =
        appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(
            when ((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
                ROTATION_0, ROTATION_180 -> /*ORIENTATION_PORTRAIT*/ AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                else -> /*ORIENTATION_LANDSCAPE*/ AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            }
        )

    fun clickBaseIntent(context: Context) = Intent(WIDGET_CLICK, null, context, javaClass)

    open fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widget = RemoteViews(context.packageName, R.layout.widget_list)
        widget.setEmptyView(R.id.list, R.id.emptyView)
        //noinspection InlinedApi
        val clickPI = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickBaseIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        widget.setOnClickPendingIntent(R.id.emptyView, clickPI)
        if (isProtected(context)) {
            widget.setTextViewText(
                R.id.emptyView, context.getString(R.string.warning_password_protected) + " " +
                        context.getString(R.string.warning_widget_disabled)
            )
        } else {
            val svcIntent = Intent(context, clazz)
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            svcIntent.putExtra(KEY_WIDTH, availableWidth(context, appWidgetManager, appWidgetId))
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            svcIntent.data = Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))
            widget.setRemoteAdapter(R.id.list, svcIntent)
            widget.setTextViewText(
                R.id.emptyView,
                context.getString(emptyTextResourceId)
            )
            widget.setPendingIntentTemplate(R.id.list, clickPI)
        }
        appWidgetManager.updateAppWidget(appWidgetId, widget)
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

    protected open fun isProtected(context: Context): Boolean {
        return (context.applicationContext as MyApplication).isProtected &&
                !prefHandler.getBoolean(protectionKey, false)
    }
}