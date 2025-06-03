package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.doAsync
import org.totschnig.myexpenses.util.safeMessage
import javax.inject.Inject

class NoDataException(message: String) : Exception(message)

const val WIDGET_CLICK = "org.totschnig.myexpenses.WIDGET_CLICK"
const val WIDGET_LIST_DATA_CHANGED = "org.totschnig.myexpenses.LIST_DATA_CHANGED"

abstract class BaseWidget(private val protectionKey: PrefKey) : AppWidgetProvider() {

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    @Inject
    lateinit var repository: Repository

    protected open fun isProtected(context: Context) = prefHandler.isProtected &&
            !prefHandler.getBoolean(protectionKey, false)


    abstract fun handleWidgetClick(context: Context, intent: Intent)

    fun clickBaseIntent(context: Context) = Intent(WIDGET_CLICK, null, context, javaClass)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WIDGET_CLICK -> handleWidgetClick(context, intent)
            WIDGET_CONTEXT_CHANGED -> intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?.let { onUpdate(context, AppWidgetManager.getInstance(context), it) }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        doAsync {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        doAsync {
            appWidgetIds.forEach { appWidgetId ->
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    abstract suspend fun updateWidgetDo(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        if (isProtected(context)) {
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
        } else {
            updateWidgetDo(context, appWidgetManager, appWidgetId)
        }
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
            Configuration.ORIENTATION_LANDSCAPE -> AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            else -> AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
        }, Int.MAX_VALUE
    )

    val listLayout: Int
        get() = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.layout.widget_list_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.layout.widget_list_dark
            else -> R.layout.widget_list
        }

    fun errorView(context: Context, throwable: Throwable): RemoteViews {
        if (throwable !is NoDataException) {
            CrashHandler.report(throwable)
        }
        return RemoteViews(context.packageName, listLayout).apply {
            setTextViewText(R.id.emptyView, throwable.safeMessage)
        }
    }
}