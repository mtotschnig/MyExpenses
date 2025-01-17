package org.totschnig.myexpenses.viewmodel

import android.app.Application
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget
import timber.log.Timber
import java.util.Locale

open class BudgetViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val data: Flow<List<Budget>> by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.BUDGETS_URI,
            notifyForDescendants = true,
            sortOrder = KEY_TITLE

        ).onEach {
            Timber.i("budget data received")
        }
            .mapToList(mapper = repository.budgetCreatorFunction)
    }

    companion object {
        fun prefNameForCriteria(budgetId: Long): String =
            "budgetFilter_%d".format(Locale.ROOT, budgetId)

        fun prefNameForCriteriaLegacy(budgetId: Long): String =
            "budgetFilter_%%s_%d".format(Locale.ROOT, budgetId)
    }
}
