package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.CurrencyFormatter


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
        return context.getContentResolver().query( //TODO find out if we should implement an optimized provider method that only returns current balance
                builder.build(), null, null, null, null)
    }

    override fun getViewAt(position: Int) = RemoteViews(context.getPackageName(), R.layout.account_row_widget).apply {
        cursor?.let {
            it.moveToPosition(position)
            val account = Account.fromCursor(it)
            setBackgroundColorSave(R.id.divider3, account.color)
            val currentBalance = Money(account.currencyUnit,
                    it.getLong(it.getColumnIndexOrThrow(KEY_CURRENT_BALANCE)))
            setTextViewText(R.id.line1, it.getString(it.getColumnIndexOrThrow(KEY_LABEL)))
            setTextViewText(R.id.note,  CurrencyFormatter.instance().formatCurrency(currentBalance))
            // Next, we set a fill-intent which will be used to fill-in the pending intent template
// which is set on the collection view in StackWidgetProvider.
            setOnClickFillInIntent(R.id.object_info, Intent().apply {
                putExtra(KEY_ROWID, it.getLong(it.getColumnIndexOrThrow(KEY_ROWID)))
            })
            configureButton(R.id.command1, R.drawable.ic_menu_add, CLICK_ACTION_NEW_TRANSACTION, R.string.menu_create_transaction, account, 175)
            configureButton(R.id.command2, R.drawable.ic_menu_forward, CLICK_ACTION_NEW_TRANSFER, R.string.menu_create_transfer, account, 223)
            configureButton(R.id.command3, R.drawable.ic_menu_split, CLICK_ACTION_NEW_SPLIT, R.string.menu_create_split, account, 271)
        }
    }

    protected fun RemoteViews.configureButton(buttonId: Int, drawableResId: Int, action: String, contentDescriptionResId: Int, account: Account, minimumWidth: Int) {
        if (account.isSealed || width < minimumWidth) {
            setViewVisibility(buttonId, View.GONE)
        } else {
            setViewVisibility(buttonId, View.VISIBLE)
            setImageViewVectorDrawable(buttonId, drawableResId)
            setContentDescription(buttonId, context.getString(contentDescriptionResId))
            setOnClickFillInIntent(buttonId, Intent().apply {
                putExtra(KEY_ROWID, account.id)
                putExtra(KEY_CURRENCY, account.getCurrencyUnit().code())
                putExtra(AbstractWidget.KEY_CLICK_ACTION, action)
            })
        }
    }
}