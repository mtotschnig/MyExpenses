package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.*
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.validateDateFormat
import timber.log.Timber
import javax.inject.Inject

class UpgradeHandlerViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val _upgradeInfo: MutableStateFlow<Int?> = MutableStateFlow(null)
    val upgradeInfo: StateFlow<Int?> = _upgradeInfo

    fun upgrade(fromVersion: Int, @Suppress("UNUSED_PARAMETER") toVersion: Int) {
        if (fromVersion < 385) {
            val hasIncomeColumn = "max(amount * (transfer_peer is null)) > 0 "
            val projection = arrayOf(hasIncomeColumn)
            disposable = briteContentResolver.createQuery(
                TRANSACTIONS_URI,
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
                        DateCriterion.fromLegacy(legacy).toStringExtra().also { new ->
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
                TEMPLATES_URI, null, "$KEY_PLANID is not null", null, null, false
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
                //semantics of pref has changed, previously true meant do Aggregate, false means don't
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
                contentResolver.call(DUAL_URI, METHOD_CHECK_CORRUPTED_DATA_987, null, null)
                    ?.getLongArray(KEY_RESULT)?.size?.let { corruptedCount ->
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
                    BUDGETS_URI,
                    arrayOf("$TABLE_BUDGETS.$KEY_ROWID"),
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
        if (fromVersion < 557) {
            viewModelScope.launch(coroutineDispatcher) {
                settings.all.entries.filter { it.key.startsWith("defaultBudget") && it.value is Long }
                    .forEach { entry ->
                        val (_, accountIdAsString, grouping) = entry.key.split('_')
                        val accountId = accountIdAsString.toLong()
                        val (selection, selectionArgs) = when {
                            accountId > 0L -> {
                                "$KEY_ACCOUNTID = ? AND $KEY_GROUPING = ?" to
                                        arrayOf(accountIdAsString, grouping)
                            }
                            accountId == AggregateAccount.HOME_AGGREGATE_ID -> {
                                "$KEY_CURRENCY = ? AND $KEY_GROUPING = ?" to
                                        arrayOf(AGGREGATE_HOME_CURRENCY_CODE, grouping)
                            }
                            else -> {
                                "$KEY_CURRENCY = (SELECT $KEY_CODE FROM $TABLE_CURRENCIES WHERE -$KEY_ROWID = ?) AND $KEY_GROUPING = ?" to
                                        arrayOf(accountIdAsString, grouping)
                            }
                        }
                        val updateCount = contentResolver.update(
                            ContentUris.withAppendedId(BUDGETS_URI, entry.value as Long),
                            ContentValues(1).also { it.put(KEY_IS_DEFAULT, 1) },
                            selection,
                            selectionArgs
                        )
                        if (updateCount != 1) {
                            CrashHandler.report(IllegalStateException("Expected one budget for ${entry.key} to be updated, but updateCount is $updateCount"))
                        }
                        prefHandler.remove(entry.key)
                    }

                val accountExpansionPrefs = settings.all.entries
                    .filter { it.key.startsWith("ACCOUNT_EXPANSION") }
                accountExpansionPrefs
                    .filter { it.value as? Boolean == false }
                    .map { it.key.substringAfterLast('_') }
                    .takeIf { it.isNotEmpty() }
                    ?.toSet()?.let {
                        val collapsedIdsPrefKey = stringSetPreferencesKey("collapsedAccounts")
                       dataStore.edit { settings ->
                            settings[collapsedIdsPrefKey] = it
                        }
                    }
                accountExpansionPrefs.forEach {
                    prefHandler.remove(it.key)
                }

                val collapsedHeaderPrefs = settings.all.entries
                    .filter { it.key.startsWith("collapsedHeaders") }

                collapsedHeaderPrefs
                    .filter { !(it.value as? String).isNullOrBlank() }
                    .forEach { entry ->
                        val collapsedIdsPrefKey = stringSetPreferencesKey(entry.key)
                        dataStore.edit { settings ->
                            settings[collapsedIdsPrefKey] = (entry.value as String).split(',')
                                .mapNotNull { headerId ->
                                    if (entry.key == "collapsedHeadersDrawer_CURRENCY") {
                                        if (headerId == Long.MAX_VALUE.toString()) {
                                            AGGREGATE_HOME_CURRENCY_CODE
                                        } else {
                                            CurrencyEnum.values()
                                                .find { it.name.hashCode() == headerId.toInt() }?.name
                                        }
                                    } else headerId
                                }
                                .toSet()
                        }
                    }
                collapsedHeaderPrefs.forEach {
                    prefHandler.remove(it.key)
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