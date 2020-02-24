package org.totschnig.myexpenses

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.widget.AbstractWidget
import org.totschnig.myexpenses.widget.AbstractWidget.KEY_CLICK_ACTION
import org.totschnig.myexpenses.widget.CLICK_ACTION_NEW_SPLIT
import org.totschnig.myexpenses.widget.CLICK_ACTION_NEW_TRANSACTION
import org.totschnig.myexpenses.widget.CLICK_ACTION_NEW_TRANSFER
import timber.log.Timber


class MyWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MyRemoteViewsFactory(this.applicationContext, intent)
    }
}

class MyRemoteViewsFactory(
        private val context: Context,
        intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    private val appWidgetId: Int
    private var cursor: Cursor? = null
    private val width: Int

    init {
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        width = intent.getIntExtra(AbstractWidget.KEY_WIDTH, 0)
    }

    override fun onCreate() {}

    override fun getLoadingView() = null

    override fun getItemId(position: Int) = with(cursor!!) {
        moveToPosition(position)
        getLong(getColumnIndex(KEY_ROWID))
    }

    override fun onDataSetChanged() {
        Timber.w("onDataSetchanged")
        cursor?.close()
        val builder = TransactionProvider.ACCOUNTS_URI.buildUpon()
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1")
        val token = Binder.clearCallingIdentity();
        try {
            cursor = context.getContentResolver().query( //TODO find out if we should implement an optimized provider method that only returns current balance
                    builder.build(), null, null, null, null)
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    override fun hasStableIds() = true

    override fun getViewAt(position: Int) = RemoteViews(context.getPackageName(), R.layout.account_row_widget).apply {
        cursor?.let {
            it.moveToPosition(position)
            val a = Account.fromCursor(it)
            setBackgroundColorSave(this, R.id.divider3, a.color)
            val currentBalance = Money(a.currencyUnit,
                    it.getLong(it.getColumnIndexOrThrow(DatabaseConstants.KEY_CURRENT_BALANCE)))
            setTextViewText(R.id.line1, it.getString(it.getColumnIndexOrThrow(KEY_LABEL)))
            setTextViewText(R.id.note,  CurrencyFormatter.instance().formatCurrency(currentBalance))
            // Next, we set a fill-intent which will be used to fill-in the pending intent template
// which is set on the collection view in StackWidgetProvider.
            setOnClickFillInIntent(R.id.object_info, Intent().apply {
                putExtra(KEY_ROWID, it.getLong(it.getColumnIndexOrThrow(KEY_ROWID)))
            })
            configureButton(R.id.command1, R.drawable.ic_menu_add, CLICK_ACTION_NEW_TRANSACTION, a, 175)
            configureButton(R.id.command2, R.drawable.ic_menu_forward, CLICK_ACTION_NEW_TRANSFER, a, 223)
            configureButton(R.id.command3, R.drawable.ic_menu_split, CLICK_ACTION_NEW_SPLIT, a, 271)
        }
    }

    private fun RemoteViews.configureButton(buttonId: Int, drawableResId: Int, action: String, account: Account, minimumWidth: Int) {
        if (account.isSealed || width < minimumWidth) {
            setViewVisibility(buttonId, View.GONE)
        } else {
            setViewVisibility(buttonId, View.VISIBLE)
            setImageViewVectorDrawable(buttonId, drawableResId)
            setOnClickFillInIntent(buttonId, Intent().apply {
                putExtra(KEY_ROWID, account.id)
                putExtra(KEY_CURRENCY, account.getCurrencyUnit().code())
                putExtra(KEY_CLICK_ACTION, action)
            })
        }
    }

    override fun getCount() = cursor?.count ?: 0

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
        cursor?.close()
    }

    protected fun setBackgroundColorSave(updateViews: RemoteViews, res: Int, color: Int) {
        updateViews.setInt(res, "setBackgroundColor", color)
    }

    //http://stackoverflow.com/a/35633411/1199911
    protected fun RemoteViews.setImageViewVectorDrawable(viewId: Int, resId: Int) {
        setImageViewBitmap(viewId, UiUtils.getTintedBitmapForTheme(context, resId,
                R.style.ThemeDark))
    }
}