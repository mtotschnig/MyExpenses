package org.totschnig.myexpenses.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL
import org.totschnig.myexpenses.provider.TransactionProvider


class AccountWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AccountRemoteViewsFactory(this.applicationContext, intent)
    }
}

class AccountRemoteViewsFactory(
        val context: Context,
        intent: Intent
) : AbstractRemoteViewsFactory(context, intent) {
    private val appWidgetId= intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val accountId = accountId(context, appWidgetId)
    private val sumColumn = sumColumn(context, appWidgetId)

    override fun buildCursor() = buildCursor(context, accountId)

    override fun RemoteViews.populate(cursor: Cursor) {
        populate(context, this, cursor, sumColumn, width)
    }

    companion object {
        fun accountId(context: Context, appWidgetId: Int) = AccountWidgetConfigurationFragment.loadSelectionPref(context, appWidgetId)
        fun sumColumn(context: Context, appWidgetId: Int) = if (AccountWidgetConfigurationFragment.loadSumPref(context, appWidgetId) == "current_balance")
            KEY_CURRENT_BALANCE  else KEY_TOTAL

                private fun RemoteViews.configureButton(context: Context, buttonId: Int, drawableResId: Int, action: String, contentDescriptionResId: Int, account: Account, availableWidth: Int, minimumWidth: Int) {
            if (account.isSealed || availableWidth < minimumWidth) {
                setViewVisibility(buttonId, View.GONE)
            } else {
                setViewVisibility(buttonId, View.VISIBLE)
                setImageViewVectorDrawable(context, buttonId, drawableResId)
                setContentDescription(buttonId, context.getString(contentDescriptionResId))
                setOnClickFillInIntent(buttonId, Intent().apply {
                    putExtra(KEY_ROWID, account.id)
                    putExtra(KEY_CURRENCY, account.currencyUnit.code)
                    putExtra(KEY_CLICK_ACTION, action)
                })
            }
        }
       fun populate(context: Context, remoteViews: RemoteViews, cursor: Cursor, sumColumn: String, availableWidth: Int) {
            with(remoteViews) {
                val account = Account.fromCursor(cursor)
                setBackgroundColorSave(R.id.divider3, account.color)
                val currentBalance = Money(account.currencyUnit,
                        cursor.getLong(cursor.getColumnIndexOrThrow(sumColumn)))
                setTextViewText(R.id.line1, account.getLabelForScreenTitle(context))
                setTextViewText(R.id.note, (context.applicationContext as MyApplication).appComponent.currencyFormatter().formatCurrency(currentBalance))
                setOnClickFillInIntent(R.id.object_info, Intent().apply {
                    putExtra(KEY_ROWID, account.id)
                })
                configureButton(context, R.id.command1, R.drawable.ic_menu_add, CLICK_ACTION_NEW_TRANSACTION, R.string.menu_create_transaction, account, availableWidth , 175)
                configureButton(context, R.id.command2, R.drawable.ic_menu_forward, CLICK_ACTION_NEW_TRANSFER, R.string.menu_create_transfer, account, availableWidth, 223)
                configureButton(context, R.id.command3, R.drawable.ic_menu_split, CLICK_ACTION_NEW_SPLIT, R.string.menu_create_split, account, availableWidth, 271)
            }
        }
        fun buildCursor(context: Context, accountId: String): Cursor? {
            val builder = TransactionProvider.ACCOUNTS_URI.buildUpon()
            var selection = "${DatabaseConstants.KEY_HIDDEN} = 0"
            var selectionArgs: Array<String>? = null
            var projection: Array<String>? = null
            if (accountId.toLong().let { it > 0L && it != Long.MAX_VALUE }) {
                selection += " AND $KEY_ROWID = ?"
                selectionArgs = arrayOf(accountId)
                projection = Account.PROJECTION_FULL
            } else {
                builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, accountId.takeIf { it != Long.MAX_VALUE.toString() }
                        ?: "1")
            }
            return context.contentResolver.query(
                    builder.build(), projection, selection, selectionArgs, null)
        }
    }
}