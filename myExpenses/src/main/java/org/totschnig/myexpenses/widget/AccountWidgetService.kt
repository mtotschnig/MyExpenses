package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_FULL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES
import org.totschnig.myexpenses.util.formatMoney


class AccountWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AccountRemoteViewsFactory(this.applicationContext, intent)
    }
}

class AccountRemoteViewsFactory(
    val context: Context,
    intent: Intent
) : AbstractRemoteViewsFactory(context, intent) {
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private val accountId = accountId(context, appWidgetId)
    private val sumColumn = sumColumn(context, appWidgetId)

    override fun buildCursor() = buildCursor(context, accountId)

    override fun RemoteViews.populate(cursor: Cursor) {
        populate(context, this, cursor, sumColumn, width, null)
    }

    companion object {
        fun accountId(context: Context, appWidgetId: Int) =
            AccountWidgetConfigurationFragment.loadSelectionPref(context, appWidgetId)

        fun sumColumn(context: Context, appWidgetId: Int) =
            if (AccountWidgetConfigurationFragment.loadSumPref(
                    context,
                    appWidgetId
                ) == "current_balance"
            )
                KEY_CURRENT_BALANCE else KEY_TOTAL

        private fun RemoteViews.configureButton(
            context: Context,
            buttonId: Int,
            drawableResId: Int,
            action: String,
            contentDescriptionResId: Int,
            account: Account,
            availableWidth: Int,
            minimumWidth: Int,
            clickInfo: Pair<Int, Intent>?
        ) {
            if (account.isSealed || availableWidth < minimumWidth) {
                setViewVisibility(buttonId, View.GONE)
            } else {
                setViewVisibility(buttonId, View.VISIBLE)
                setImageViewVectorDrawable(context, buttonId, drawableResId)
                setContentDescription(buttonId, context.getString(contentDescriptionResId))
                val block: Intent.() -> Unit = {
                    putExtra(KEY_ROWID, account.id)
                    putExtra(KEY_CURRENCY, account.currencyUnit.code)
                    putExtra(KEY_CLICK_ACTION, action)
                }
                if (clickInfo == null) {
                    setOnClickFillInIntent(buttonId, Intent().apply(block))
                } else {
                    //noinspection InlinedApi
                    setOnClickPendingIntent(
                        buttonId, PendingIntent.getBroadcast(
                            context, clickInfo.first, clickInfo.second.apply(block).apply {
                                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                        )
                    )
                }
            }
        }

        /**
         * @param clickInfo If null, we are called from collection, and just need to call setOnClickFillInIntent,
         * otherwise this is the base intent for handling clicks
         */
        fun populate(
            context: Context,
            remoteViews: RemoteViews,
            cursor: Cursor,
            sumColumn: String,
            availableWidth: Int,
            clickInfo: Pair<Int, Intent>?
        ) {
            with(remoteViews) {
                val account = Account.fromCursor(cursor)
                setBackgroundColorSave(R.id.divider3, account.color)
                val currentBalance = Money(
                    account.currencyUnit,
                    cursor.getLong(cursor.getColumnIndexOrThrow(sumColumn))
                )
                setTextViewText(R.id.line1, account.getLabelForScreenTitle(context))
                setTextViewText(
                    R.id.note,
                    (context.applicationContext as MyApplication).appComponent.currencyFormatter()
                        .formatMoney(currentBalance)
                )
                val block: Intent.() -> Unit = {
                    putExtra(KEY_ROWID, account.id)
                }
                if (clickInfo == null) {
                    setOnClickFillInIntent(R.id.object_info, Intent().apply(block))
                } else {
                    //noinspection InlinedApi
                    setOnClickPendingIntent(
                        R.id.object_info, PendingIntent.getBroadcast(
                            context,
                            clickInfo.first,
                            clickInfo.second.apply(block),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
                configureButton(
                    context,
                    R.id.command1,
                    R.drawable.ic_menu_add,
                    CLICK_ACTION_NEW_TRANSACTION,
                    R.string.menu_create_transaction,
                    account,
                    availableWidth,
                    175,
                    clickInfo
                )
                configureButton(
                    context,
                    R.id.command2,
                    R.drawable.ic_menu_forward,
                    CLICK_ACTION_NEW_TRANSFER,
                    R.string.menu_create_transfer,
                    account,
                    availableWidth,
                    223,
                    clickInfo
                )
                configureButton(
                    context,
                    R.id.command3,
                    R.drawable.ic_menu_split,
                    CLICK_ACTION_NEW_SPLIT,
                    R.string.menu_create_split,
                    account,
                    availableWidth,
                    271,
                    clickInfo
                )
            }
        }

        fun buildCursor(context: Context, accountId: String): Cursor? {
            val uri: Uri
            val selection: String
            val selectionArgs: Array<String>?
            if (accountId.toLong().let { it > 0L && it != Long.MAX_VALUE }) {
                uri = ACCOUNTS_FULL_URI
                selection = "$KEY_ROWID = ?"
                selectionArgs = arrayOf(accountId)
            } else {
                uri = ACCOUNTS_FULL_URI.buildUpon().appendQueryParameter(
                    QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES,
                    accountId.takeIf { it != Long.MAX_VALUE.toString() } ?: "1"
                ).build()
                selection = "$KEY_HIDDEN = 0"
                selectionArgs = null
            }
            return context.contentResolver.query(uri, null, selection, selectionArgs, null)
        }
    }
}