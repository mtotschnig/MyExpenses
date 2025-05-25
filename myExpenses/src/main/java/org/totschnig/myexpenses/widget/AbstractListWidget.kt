package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import timber.log.Timber
import androidx.core.net.toUri

const val KEY_CLICK_ACTION = "clickAction"
const val WIDGET_CONTEXT_CHANGED = "org.totschnig.myexpenses.CONTEXT_CHANGED"
const val EXTRA_START_FROM_WIDGET = "startFromWidget"
const val EXTRA_START_FROM_WIDGET_DATA_ENTRY = "startFromWidgetDataEntry"
const val KEY_WIDTH = "width"
const val WIDGET_ROW_RESERVED_SPACE_FOR_INFO = 110

fun onConfigurationChanged(context: Context) {
    updateWidgets(context, AccountWidget::class.java, WIDGET_CONTEXT_CHANGED)
    updateWidgets(context, TemplateWidget::class.java, WIDGET_CONTEXT_CHANGED)
}

fun updateWidgets(
    context: Context,
    provider: Class<out AppWidgetProvider?>,
    action: String,
    appWidgetIds: IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, provider)),
) =
    context.sendBroadcast(Intent(context, provider).apply {
        this.action = action
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
    })

abstract class AbstractListWidget(
    private val clazz: Class<out RemoteViewsService>,
    protectionKey: PrefKey,
) : BaseWidget(protectionKey) {
    abstract val emptyTextResourceId: Int

    override fun onReceive(context: Context, intent: Intent) {
        context.injector.inject(this)
        val instance = AppWidgetManager.getInstance(context)
        val appWidgetIds = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        when (intent.action) {
            WIDGET_LIST_DATA_CHANGED ->
                appWidgetIds?.let { instance.notifyAppWidgetViewDataChanged(it, R.id.list) }

            else -> super.onReceive(context, intent)
        }
    }

    override suspend fun updateWidgetDo(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val clickPI = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickBaseIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        appWidgetManager.updateAppWidget(
            appWidgetId,
            RemoteViews(context.packageName, listLayout).apply {
                setEmptyView(R.id.list, R.id.emptyView)
                setOnClickPendingIntent(R.id.emptyView, clickPI)

                setRemoteAdapter(R.id.list, Intent(context, clazz).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    val availableWidth =
                        availableWidthForButtons(context, appWidgetManager, appWidgetId)
                    Timber.i("availableWidth: %d", availableWidth)
                    putExtra(KEY_WIDTH, availableWidth)
                    // When intents are compared, the extras are ignored, so we need to embed the extras
                    // into the data so that the extras will not be ignored.
                    data = toUri(Intent.URI_INTENT_SCHEME).toUri()
                })
                setTextViewText(
                    R.id.emptyView,
                    context.getString(emptyTextResourceId)
                )
                setPendingIntentTemplate(R.id.list, clickPI)
            }
        )
    }

    fun availableWidthForButtons(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) = availableWidth(
        context,
        appWidgetManager,
        appWidgetId
    ) - (WIDGET_ROW_RESERVED_SPACE_FOR_INFO * context.resources.configuration.fontScale).toInt()
}