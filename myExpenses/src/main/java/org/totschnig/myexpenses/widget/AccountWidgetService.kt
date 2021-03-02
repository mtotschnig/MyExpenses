package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
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

    override fun buildCursor(): Cursor? {
        val builder = TransactionProvider.ACCOUNTS_URI.buildUpon()
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1")
        return context.contentResolver.query(
                builder.build(), null, DatabaseConstants.KEY_HIDDEN + " = 0", null, null)
    }

    override fun RemoteViews.populate(cursor: Cursor) {
        val account = Account.fromCursor(cursor)
        setBackgroundColorSave(R.id.divider3, account.color)
        val currentBalance = Money(account.currencyUnit,
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CURRENT_BALANCE)))
        setTextViewText(R.id.line1, account.label)
        setTextViewText(R.id.note, (context.applicationContext as MyApplication).appComponent.currencyFormatter().formatCurrency(currentBalance))
        setOnClickFillInIntent(R.id.object_info, Intent().apply {
            putExtra(KEY_ROWID, account.id)
        })
        configureButton(R.id.command1, R.drawable.ic_menu_add, CLICK_ACTION_NEW_TRANSACTION, R.string.menu_create_transaction, account, 175)
        configureButton(R.id.command2, R.drawable.ic_menu_forward, CLICK_ACTION_NEW_TRANSFER, R.string.menu_create_transfer, account, 223)
        configureButton(R.id.command3, R.drawable.ic_menu_split, CLICK_ACTION_NEW_SPLIT, R.string.menu_create_split, account, 271)
    }

    private fun RemoteViews.configureButton(buttonId: Int, drawableResId: Int, action: String, contentDescriptionResId: Int, account: Account, minimumWidth: Int) {
        if (account.isSealed || width < minimumWidth) {
            setViewVisibility(buttonId, View.GONE)
        } else {
            setViewVisibility(buttonId, View.VISIBLE)
            setImageViewVectorDrawable(buttonId, drawableResId)
            setContentDescription(buttonId, context.getString(contentDescriptionResId))
            setOnClickFillInIntent(buttonId, Intent().apply {
                putExtra(KEY_ROWID, account.id)
                putExtra(KEY_CURRENCY, account.currencyUnit.code)
                putExtra(KEY_CLICK_ACTION, action)
            })
        }
    }
}