package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import javax.inject.Inject
import kotlin.math.sign

abstract class AbstractRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    @Inject
    lateinit var prefHandler: PrefHandler

    protected var cursor: Cursor? = null
    protected val width: Int = intent.getIntExtra(KEY_WIDTH, Int.MAX_VALUE)

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
        RemoteViews(context.packageName, rowLayout).apply {
            cursor?.takeIf { !it.isClosed && it.moveToPosition(position) }?.let {
                populate(it)
            }
        }

    abstract fun RemoteViews.populate(cursor: Cursor)

    companion object {

        val rowLayout: Int
            get() = when(AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.layout.widget_row_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.layout.widget_row_dark
                else -> R.layout.widget_row
            }
    }
}

fun RemoteViews.setBackgroundColorSave(viewId: Int, color: Int) {
    setInt(viewId, "setBackgroundColor", color)
}

fun themedContext(context: Context) = ContextThemeWrapper(context, when (AppCompatDelegate.getDefaultNightMode()) {
    AppCompatDelegate.MODE_NIGHT_NO -> R.style.WidgetLight
    AppCompatDelegate.MODE_NIGHT_YES -> R.style.WidgetDark
    else -> R.style.WidgetDayNight
})

fun RemoteViews.setAmountColor(context: Context, viewId: Int, amount: Long) {
    setTextColor(viewId,  UiUtils.getColor(themedContext(context), when (amount.sign) {
        1 -> R.attr.colorIncome
        -1 -> R.attr.colorExpense
        else -> android.R.attr.textColorPrimary
    }))
}
