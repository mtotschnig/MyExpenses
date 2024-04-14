package org.totschnig.myexpenses.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.OcrLauncher
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment.Button
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.doAsync
import org.totschnig.myexpenses.widget.AccountRemoteViewsFactory.Companion.buttons
import org.totschnig.myexpenses.widget.AccountRemoteViewsFactory.Companion.sumColumn

class AccountWidget :
    AbstractListWidget(AccountWidgetService::class.java, PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WIDGET_LIST_DATA_CHANGED) {
            val widgets = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?.map { it to AccountRemoteViewsFactory.accountId(context, it) }
                ?.filter { it.second != Long.MAX_VALUE.toString() }
            if (widgets?.isNotEmpty() == true) {
                doAsync {
                    widgets.forEach {
                        updateSingleAccountWidget(
                            context,
                            AppWidgetManager.getInstance(context),
                            it.first,
                            it.second
                        )
                    }
                }
            }
        }
    }

    override val emptyTextResourceId = R.string.no_accounts

    private fun updateSingleAccountWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        accountId: String
    ) {
        val widget = kotlin.runCatching {
            AccountRemoteViewsFactory.buildCursor(context, accountId)
        }.mapCatching {
            it?.use { cursor ->
                if (cursor.moveToFirst()) {
                    RemoteViews(
                        context.packageName,
                        AbstractRemoteViewsFactory.rowLayout
                    ).also { widget ->
                        AccountRemoteViewsFactory.populate(
                            context = context,
                            currencyContext = currencyContext,
                            currencyFormatter = currencyFormatter,
                            remoteViews = widget,
                            cursor = cursor,
                            sumColumn = sumColumn(context, appWidgetId),
                            availableWidth = availableWidthForButtons(
                                context,
                                appWidgetManager,
                                appWidgetId
                            ),
                            clickInfo = Pair(appWidgetId, clickBaseIntent(context)),
                            buttons = buttons(context, appWidgetId)
                        )
                    }
                } else {
                    throw NoDataException(context.getString(R.string.account_deleted))
                }
            } ?: throw Exception("Cursor returned null")
        }.getOrElse { errorView(context, it) }
        appWidgetManager.updateAppWidget(appWidgetId, widget)
    }

    override suspend fun updateWidgetDo(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val accountId = AccountRemoteViewsFactory.accountId(context, appWidgetId)
        if (accountId != Long.MAX_VALUE.toString() && !isProtected(context)) {
            updateSingleAccountWidget(context, appWidgetManager, appWidgetId, accountId)
        } else {
            super.updateWidgetDo(context, appWidgetManager, appWidgetId)
        }
    }

    override fun handleWidgetClick(context: Context, intent: Intent) {
        val accountId = intent.getLongExtra(KEY_ROWID, 0)
        val startIntent = when (val clickAction = intent.getStringExtra(KEY_CLICK_ACTION)) {
            null -> {
                Intent(context, MyExpenses::class.java).apply {
                    putExtra(KEY_ROWID, accountId)
                }
            }

            else -> {
                (if (clickAction == "SCAN") Intent(context, OcrLauncher::class.java)
                else Intent(context, ExpenseEdit::class.java).apply {
                    putExtra(EXTRA_START_FROM_WIDGET, true)
                    putExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
                    putExtra(OPERATION_TYPE, Button.valueOf(clickAction).type)
                }).apply {
                    if (accountId < 0) {
                        putExtra(
                            KEY_CURRENCY, intent.getStringExtra(KEY_CURRENCY)
                        )
                    } else {
                        putExtra(KEY_ACCOUNTID, accountId)
                    }
                    putExtra(KEY_COLOR, intent.getIntExtra(KEY_COLOR, 0))
                }
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(startIntent)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            AccountWidgetConfigurationFragment.clearPreferences(context, appWidgetId)
        }
    }

    companion object {
        val OBSERVED_URIS = arrayOf(
            TransactionProvider.ACCOUNTS_URI, //if color changes
            TransactionProvider.TRANSACTIONS_URI
        )
    }
}