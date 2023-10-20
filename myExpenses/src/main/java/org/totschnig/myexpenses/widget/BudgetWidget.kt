package org.totschnig.myexpenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat.setProgressBarProgress
import androidx.core.widget.RemoteViewsCompat.setViewStubLayoutResource
import androidx.core.widget.RemoteViewsCompat.setViewTranslationXDimen
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.activity.BudgetWidgetConfigure
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
            val budgetInfo = repository.loadBudgetProgress(budgetId) ?: return@doAsync
            Timber.i("totalDays / currentDay : %d / %d", budgetInfo.totalDays, budgetInfo.currentDay)
            val progress = budgetInfo.spent / budgetInfo.allocated.toFloat()
            val todayPosition = budgetInfo.currentDay / budgetInfo.totalDays.toFloat()
            val color = when {
                progress > 1 -> R.layout.budget_widget_progress_red
                progress > todayPosition -> R.layout.budget_widget_progress_yellow
                else -> R.layout.budget_widget_progress_green
            }
            Timber.i("progress: %f - %f", progress, todayPosition)
            val widget = RemoteViews(
                context.packageName,
                R.layout.budget_widget
            ).apply {
                val titleVisible = availableHeight >= 110
                setViewVisibility(
                    R.id.title,
                    if (titleVisible) View.VISIBLE else View.GONE
                )
                setTextViewText(R.id.title, budgetInfo.title)
                setViewStubLayoutResource(R.id.budgetProgressStub, color)
                setViewVisibility(R.id.budgetProgressStub, View.VISIBLE)
                setProgressBarProgress(R.id.budget_progress, (progress * 100).toInt())
                val translation = availableWidth * todayPosition - 0.5F
                Timber.i("translation %f", translation)
                val remainingBudget = budgetInfo.remainingBudget
                val remainingDays = budgetInfo.remainingDays
                val info = buildList {
                    if (!titleVisible) {
                        add("${budgetInfo.title}:")
                    }
                    add("You have spent ${budgetInfo.spent} of ${budgetInfo.allocated}.")
                    if (remainingBudget > 0 && remainingDays > 0) {
                        add("Remainder: $remainingBudget (${remainingBudget / remainingDays.toFloat()} / day)")
                    }
                }.joinToString(separator = " ")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setViewTranslationXDimen(
                        R.id.todayMarker,
                        translation,
                        TypedValue.COMPLEX_UNIT_DIP
                    )
                } else {
                    val padding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        translation,
                        context.resources.displayMetrics
                    ).toInt()
                    setViewPadding(R.id.todayMarkerContainer, padding, 0, 0, 0)
                }
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

    /*    private fun RemoteViews.setProgressBarProgressColorCompat(progressBarId: Int, color: Int) {
            val tint = ColorStateList.valueOf(color)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setProgressBarProgressTintList(progressBarId, tint)
            } else {
                //This does not work on API 28..30, so we use ViewStubs to
                try {
                    RemoteViews::class.java.getMethod(
                        "setProgressTintList",
                        Int::class.javaPrimitiveType,
                        ColorStateList::class.java
                    )
                } catch (e: Exception) {
                    CrashHandler.report(e)
                    null
                }?.let {
                    try {
                        it.invoke(this, progressBarId, tint)
                    } catch (e: Exception) {
                        CrashHandler.report(e)
                    }
                }
            }
        }*/
}