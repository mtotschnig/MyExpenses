package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

abstract class AbstractRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    protected var cursor: Cursor? = null
    protected val width: Int = intent.getIntExtra(KEY_WIDTH, 0).takeIf { it > 0 } ?: Int.MAX_VALUE

    override fun onCreate() {}

    override fun getLoadingView() = null

    override fun getItemId(position: Int) = cursor?.takeIf {
        !it.isClosed && it.moveToPosition(position)
    }?.let {
        it.getLong(it.getColumnIndexOrThrow(DatabaseConstants.KEY_ROWID))
    } ?: 0

    override fun hasStableIds() = true

    override fun getCount() = cursor?.count ?: 0

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
        cursor?.close()
    }

    override fun onDataSetChanged() {
        cursor?.close()
        val token = Binder.clearCallingIdentity()
        try {
            cursor = buildCursor()
        } catch (e: SQLiteException) {
            CrashHandler.report(e)
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    abstract fun buildCursor(): Cursor?

    override fun getViewAt(position: Int) =
        RemoteViews(context.packageName, R.layout.widget_row).apply {
            cursor?.takeIf { !it.isClosed && it.moveToPosition(position) }?.let {
                populate(it)
            }
        }

    abstract fun RemoteViews.populate(cursor: Cursor)
}

//http://stackoverflow.com/a/35633411/1199911
fun RemoteViews.setImageViewVectorDrawable(context: Context, viewId: Int, resId: Int) {
    setImageViewBitmap(
        viewId, UiUtils.getTintedBitmapForTheme(
            context, resId,
            R.style.DarkBackground
        )
    )
}

fun RemoteViews.setBackgroundColorSave(res: Int, color: Int) {
    setInt(res, "setBackgroundColor", color)
}
