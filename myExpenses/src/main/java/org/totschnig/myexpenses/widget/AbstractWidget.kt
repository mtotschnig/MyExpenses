package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import timber.log.Timber
import javax.inject.Inject


const val WIDGET_CLICK = "org.totschnig.myexpenses.WIDGET_CLICK"
const val KEY_CLICK_ACTION = "clickAction"
const val WIDGET_LIST_DATA_CHANGED = "org.totschnig.myexpenses.LIST_DATA_CHANGED"
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

    @Inject
    lateinit var currencyContext: CurrencyContext

    override fun onReceive(context: Context, intent: Intent) {
        context.injector.inject(this)
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

    fun availableWidth(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else -> AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        }, Int.MAX_VALUE
    ) - (WIDGET_ROW_RESERVED_SPACE_FOR_INFO * context.resources.configuration.fontScale).toInt()

    fun clickBaseIntent(context: Context) = Intent(WIDGET_CLICK, null, context, javaClass)

    open fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {

        appWidgetManager.updateAppWidget(appWidgetId, if (isProtected(context)) {
            RemoteViews(context.packageName, R.layout.widget_locked).apply {
                setOnClickPendingIntent(
                    R.id.text,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        Intent(context, MyPreferenceActivity::class.java).putExtra(
                            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                            context.getString(R.string.pref_screen_protection_key)
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                setTextViewText(
                    R.id.text, context.getString(R.string.warning_password_protected) + " " +
                            context.getString(R.string.warning_widget_disabled)
                )
            }
        } else {
            val clickPI = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                clickBaseIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            RemoteViews(
                context.packageName,
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_NO -> R.layout.widget_list_light
                    AppCompatDelegate.MODE_NIGHT_YES -> R.layout.widget_list_dark
                    else -> R.layout.widget_list
                }
            ).apply {
                setEmptyView(R.id.list, R.id.emptyView)
                setOnClickPendingIntent(R.id.emptyView, clickPI)

                setRemoteAdapter(R.id.list, Intent(context, clazz).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    val availableWidth = availableWidth(context, appWidgetManager, appWidgetId)
                    Timber.i("availableWidth: %d", availableWidth)
                    putExtra(KEY_WIDTH, availableWidth)
                    // When intents are compared, the extras are ignored, so we need to embed the extras
                    // into the data so that the extras will not be ignored.
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                })
                setTextViewText(
                    R.id.emptyView,
                    context.getString(emptyTextResourceId)
                )
                setPendingIntentTemplate(R.id.list, clickPI)
            }
        })
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
        return (context.myApplication).isProtected &&
                !prefHandler.getBoolean(protectionKey, false)
    }
}