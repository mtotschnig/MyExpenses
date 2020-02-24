package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.WindowManager
import android.widget.RemoteViews
import org.totschnig.myexpenses.MyWidgetService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID

const val CLICK_ACTION_NEW_TRANSACTION = "newTransaction"
const val CLICK_ACTION_NEW_TRANSFER = "newTransfer"
const val CLICK_ACTION_NEW_SPLIT = "newSplit"


class AccountWidget2 : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val instance = AppWidgetManager.getInstance(context)
        when (intent.action) {
            AbstractWidget.WIDGET_LIST_DATA_CHANGED -> {
                instance.notifyAppWidgetViewDataChanged(intent.extras!!.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS), R.id.list)
            }
            AbstractWidget.WIDGET_CONTEXT_CHANGED -> {
                intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.let { onUpdate(context, instance, it) }
            }
            AbstractWidget.WIDGET_CLICK -> {
                val action = intent.getStringExtra(AbstractWidget.KEY_CLICK_ACTION)
                when (action) {
                    null -> context.startActivity(Intent(context, MyExpenses::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(KEY_ROWID, intent.getLongExtra(KEY_ROWID, 0))
                    })
                    else -> context.startActivity(Intent(context, ExpenseEdit::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.getLongExtra(KEY_ROWID, 0L).let {
                            if (it < 0) {
                                putExtra(KEY_CURRENCY, intent.getStringExtra(KEY_CURRENCY))
                            } else {
                                putExtra(KEY_ACCOUNTID, it)
                            }
                        }
                        putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, true)
                        putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
                        putExtra(Transactions.OPERATION_TYPE, when(action) {
                            CLICK_ACTION_NEW_TRANSACTION ->  Transactions.TYPE_TRANSACTION
                            CLICK_ACTION_NEW_TRANSFER -> Transactions.TYPE_TRANSFER
                            CLICK_ACTION_NEW_SPLIT -> Transactions.TYPE_SPLIT
                            else -> throw IllegalArgumentException()
                        })
                    })
                }

            }
            else -> {
                super.onReceive(context, intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val orientation = when ((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay().getOrientation()) {
            ROTATION_0, ROTATION_180 -> ORIENTATION_PORTRAIT
            else -> ORIENTATION_LANDSCAPE
        }
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val svcIntent = Intent(context, MyWidgetService::class.java)
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            svcIntent.putExtra(AbstractWidget.KEY_WIDTH, when ((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay().rotation) {
                ROTATION_0, ROTATION_180 -> /*ORIENTATION_PORTRAIT*/ options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                else -> /*ORIENTATION_LANDSCAPE*/ options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            })
        }
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)))
        val widget = RemoteViews(context.getPackageName(), R.layout.widget_list)
        widget.setRemoteAdapter(R.id.list, svcIntent)
        widget.setEmptyView(R.id.list, R.id.emptyView);
        val clickIntent = Intent(AbstractWidget.WIDGET_CLICK, null, context, javaClass)
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val clickPI = PendingIntent.getBroadcast(context, appWidgetId, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        widget.setPendingIntentTemplate(R.id.list, clickPI)
        appWidgetManager.updateAppWidget(appWidgetId, widget)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }
}