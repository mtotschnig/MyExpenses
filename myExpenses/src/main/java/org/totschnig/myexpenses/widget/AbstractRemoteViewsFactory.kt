package org.totschnig.myexpenses.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.UiUtils
import timber.log.Timber

abstract class AbstractRemoteViewsFactory(
        private val context: Context,
        intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    protected val appWidgetId: Int
    protected var cursor: Cursor? = null
    protected val width: Int

    init {
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        width = intent.getIntExtra(KEY_WIDTH, 0)
    }

    override fun onCreate() {}

    override fun getLoadingView() = null

    override fun getItemId(position: Int) = with(cursor!!) {
        moveToPosition(position)
        getLong(getColumnIndex(DatabaseConstants.KEY_ROWID))
    }

    override fun hasStableIds() = true

    override fun getCount() = cursor?.count ?: 0

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
        cursor?.close()
    }

    override fun onDataSetChanged() {
        Timber.w("onDataSetchanged")
        cursor?.close()
        val builder = TransactionProvider.ACCOUNTS_URI.buildUpon()
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1")
        val token = Binder.clearCallingIdentity();
        try {
            cursor = buildCursor()
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    abstract fun buildCursor(): Cursor?

    override fun getViewAt(position: Int) = RemoteViews(context.getPackageName(), R.layout.widget_row).apply {
        cursor?.let {
            it.moveToPosition(position)
            populate(it)
        }
    }

    protected fun RemoteViews.setBackgroundColorSave(res: Int, color: Int) {
        setInt(res, "setBackgroundColor", color)
    }

    //http://stackoverflow.com/a/35633411/1199911
    protected fun RemoteViews.setImageViewVectorDrawable(viewId: Int, resId: Int) {
        setImageViewBitmap(viewId, UiUtils.getTintedBitmapForTheme(context, resId,
                R.style.ThemeDark))
    }

    abstract fun RemoteViews.populate(cursor: Cursor)
}
