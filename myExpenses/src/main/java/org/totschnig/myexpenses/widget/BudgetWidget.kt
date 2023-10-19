package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.widget.RemoteViewsCompat.setProgressBarProgress
import androidx.core.widget.RemoteViewsCompat.setProgressBarProgressTintList
import androidx.core.widget.RemoteViewsCompat.setViewTranslationXDimen
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.activity.BudgetWidgetConfigure
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.loadBudgetProgress
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
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
            val availableWidth = availableWidth(context, appWidgetManager, appWidgetId) - 32
            val availableHeight = availableHeight(context, appWidgetManager, appWidgetId)
            Timber.i("availableHeight: %d", availableHeight)
            val budgetId = BudgetWidgetConfigure.loadSelectionPref(context, appWidgetId)
            val budgetInfo = repository.loadBudgetProgress(budgetId)
            val progress = budgetInfo.spent / budgetInfo.allocated.toFloat()
            val todayPosition = budgetInfo.currentDay / budgetInfo.totalDays.toFloat()
            val color = when {
                progress > 1 -> Color.RED
                progress > todayPosition -> Color.YELLOW
                else -> Color.GREEN
            }
            Timber.i("progress: %f - %f", progress, todayPosition)
            val widget = RemoteViews(
                context.packageName,
                R.layout.budget_widget
            ).apply {
                setViewVisibility(R.id.title, if (availableHeight < 110) View.GONE else View.VISIBLE)
                setTextViewText(R.id.title, budgetInfo.title)
                //setViewLayoutWidth(R.id.budget_progress, availableWidth.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                setProgressBarProgress(R.id.budget_progress, (progress * 100).toInt())
                setProgressBarProgressTintList(
                    R.id.budget_progress, ColorStateList.valueOf(color)
                )
                val translation = availableWidth * todayPosition - 0.5F
                Timber.i("translation %f", translation)
                val remainingBudget = budgetInfo.remainingBudget
                val remainingDays = budgetInfo.remainingDays
                val info = buildList {
                    add("You have spent ${budgetInfo.spent} of ${budgetInfo.allocated}.")
                    if (remainingBudget > 0 && remainingDays > 0) {
                        add("Remainder: $remainingBudget (${remainingBudget / remainingDays.toFloat()} / day)")
                    }
                }.joinToString(separator = " ")
                setViewTranslationXDimen(R.id.todayMarker, translation, TypedValue.COMPLEX_UNIT_DIP)
                setTextViewText(R.id.spent, info)
                setOnClickPendingIntent(
                    R.id.layout,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        Intent(context, BudgetActivity::class.java).apply {
                            putExtra(DatabaseConstants.KEY_ROWID, budgetId)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )
            }
            appWidgetManager.updateAppWidget(appWidgetId, widget)
        }
    }
}