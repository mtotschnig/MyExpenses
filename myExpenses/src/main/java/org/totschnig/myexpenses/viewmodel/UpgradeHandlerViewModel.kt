package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.DateCriteria
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.validateDateFormat
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UpgradeHandlerViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    private val _upgradeInfo: MutableStateFlow<Int?> = MutableStateFlow(null)
    val upgradeInfo: StateFlow<Int?> = _upgradeInfo

    fun upgrade(fromVersion: Int, @Suppress("UNUSED_PARAMETER") toVersion: Int) {
        if (fromVersion < 385) {
            val hasIncomeColumn = "max(amount * (transfer_peer is null)) > 0 "
            val projection = arrayOf(hasIncomeColumn)
            disposable = briteContentResolver.createQuery(
                TransactionProvider.TRANSACTIONS_URI,
                projection, null, null, null, false
            )
                .subscribe { query ->
                    query.run()?.let { cursor ->
                        if (cursor.moveToFirst()) {
                            if (cursor.getInt(0) > 0) {
                                discoveryHelper.markDiscovered(DiscoveryHelper.Feature.expense_income_switch)
                            }
                        }
                        cursor.close()
                    }
                }
        }
        if (fromVersion < 391) {
            val dateFilterList =
                settings.all.entries.map { it.key }.filter { it.startsWith("filter_date") }
            dateFilterList.forEach { key ->
                prefHandler.getString(key, null)?.let { legacy ->
                    try {
                        DateCriteria.fromLegacy(legacy).toStringExtra().also { new ->
                            prefHandler.putString(key, new)
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        prefHandler.remove(key)
                    }
                }
            }
        }
        if (fromVersion < 393) {
            arrayOf(
                PrefKey.SORT_ORDER_ACCOUNTS,
                PrefKey.SORT_ORDER_CATEGORIES,
                PrefKey.SORT_ORDER_BUDGET_CATEGORIES
            ).forEach {
                if (Sort.TITLE.name == prefHandler.getString(it, null)) {
                    prefHandler.putString(it, Sort.LABEL.name)
                }
            }
        }
        if (fromVersion < 417) {
            prefHandler.getString(PrefKey.CUSTOM_DATE_FORMAT, null)?.let {
                if (validateDateFormat(it) != null) {
                    Timber.d("Removed erroneous dateFormat %s ", it)
                    prefHandler.remove(PrefKey.CUSTOM_DATE_FORMAT)
                }
            }

            disposable = briteContentResolver.createQuery(
                TransactionProvider.TEMPLATES_URI, null, String.format(
                    Locale.ROOT, "%s is not null",
                    DatabaseConstants.KEY_PLANID
                ), null, null, false
            )
                .mapToList { cursor -> Template(cursor) }
                .subscribe { list ->
                    try {
                        for (template in list) {
                            Plan.updateDescription(
                                template.planId,
                                template.compileDescription(getApplication())
                            )
                        }
                    } catch (e: SecurityException) {
                        //permission missing
                        CrashHandler.report(e)

                    }
                    dispose()
                }
        }
        if (fromVersion < 429 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            prefHandler.putString(PrefKey.UI_THEME_KEY, "default")
        }
        if (fromVersion < 486) {
            with(prefHandler) {
                putBoolean(
                    PrefKey.AUTO_FILL_SWITCH, (getBoolean(PrefKey.AUTO_FILL_AMOUNT, false)
                            || getBoolean(PrefKey.AUTO_FILL_CATEGORY, false)
                            || getBoolean(PrefKey.AUTO_FILL_COMMENT, false)
                            || getBoolean(PrefKey.AUTO_FILL_METHOD, false)
                            || getString(PrefKey.AUTO_FILL_ACCOUNT, "never") != "never")
                )
            }
        }
        if (fromVersion < 518) {
            with(prefHandler) {
                //semantics of pref has changed, previously true meant do Aggregate, false means dont
                //now unset means do Aggregate, on Distribution Screen true means show income, false means show Expenses
                if (getBoolean(
                        PrefKey.DISTRIBUTION_AGGREGATE_TYPES,
                        true
                    )
                ) remove(PrefKey.DISTRIBUTION_AGGREGATE_TYPES)
                if (getBoolean(
                        PrefKey.BUDGET_AGGREGATE_TYPES,
                        true
                    )
                ) remove(PrefKey.BUDGET_AGGREGATE_TYPES)
            }
        }
        if (fromVersion < 539) {
            viewModelScope.launch(coroutineDispatcher) {
                contentResolver.call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_CHECK_CORRUPTED_DATA_987, null, null
                )?.getLongArray(TransactionProvider.KEY_RESULT)?.size?.let { corruptedCount ->
                    if (corruptedCount > 0) {
                        _upgradeInfo.update {
                            R.string.corrupted_data_detected
                        }
                        CrashHandler.report(Exception("Bug 987: $corruptedCount corrupted transactions detected"))
                    }
                }
            }
        }
        if (fromVersion < 552) {
            viewModelScope.launch(coroutineDispatcher) {
                val budgetIds: List<Long> = contentResolver.query(
                    TransactionProvider.BUDGETS_URI,
                    arrayOf("${DatabaseConstants.TABLE_BUDGETS}.${DatabaseConstants.KEY_ROWID}"),
                    null, null, null
                )?.use { cursor -> cursor.asSequence.map { it.getLong(0) }.toList() }
                    ?: emptyList()
                val defaultBudgetKeys =
                    settings.all.entries.map { it.key }.filter { it.startsWith("defaultBudget") }
                defaultBudgetKeys.forEach {
                    prefHandler.getLong(it, 0).let { budgetId ->
                        if (!budgetIds.contains(budgetId)) {
                            Timber.w("Removing stale entry for $it, because budget $budgetId no longer exists")
                            prefHandler.remove(it)
                        }
                    }
                }
            }
        }
    }

    fun messageShown() {
        _upgradeInfo.update {
            null
        }
    }
}