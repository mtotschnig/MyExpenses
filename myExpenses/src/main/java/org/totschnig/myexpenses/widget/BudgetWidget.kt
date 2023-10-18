package org.totschnig.myexpenses.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.widget.RemoteViewsCompat.setProgressBarProgress
import androidx.core.widget.RemoteViewsCompat.setProgressBarProgressTintList
import androidx.core.widget.RemoteViewsCompat.setViewTranslationXDimen
import org.apache.commons.lang3.reflect.Typed
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.loadBudgetProgress
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.doAsync
import timber.log.Timber
import javax.inject.Inject

class BudgetWidget : BaseWidget(PrefKey.PROTECTION_ENABLE_BUDGET_WIDGET) {

    @Inject
    lateinit var repository: Repository

    override fun onReceive(context: Context, intent: Intent) {
        context.injector.inject(this)
        super.onReceive(context, intent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun updateWidgetDo(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        doAsync {
            val availableWidth = availableWidth(context, appWidgetManager, appWidgetId) - 16
            Timber.i("availableWidth: %d", availableWidth)
            val budgetId = 4L
            val progress = repository.loadBudgetProgress(budgetId)
            val widget = RemoteViews(
                context.packageName,
                R.layout.budget_widget
            ).apply {
                setViewLayoutWidth(R.id.budget_progress, availableWidth.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                setTextViewText(R.id.title, progress.title)
                setProgressBarProgress(R.id.budget_progress, 100)
                setProgressBarProgressTintList(
                    R.id.budget_progress, ColorStateList.valueOf(Color.RED)
                )
                setViewTranslationXDimen(R.id.todayMarker, availableWidth - 0.5F, TypedValue.COMPLEX_UNIT_DIP)
            }
            appWidgetManager.updateAppWidget(appWidgetId, widget)
        }
    }
}