package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.DateCriteria
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.validateDateFormat
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UpgradeHandlerViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var settings: SharedPreferences
    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    init {
        (application as MyApplication).appComponent.inject(this)
    }
    fun upgrade(fromVersion: Int, toVersion: Int) {
        if (fromVersion < 385) {
            val hasIncomeColumn = "max(amount * (transfer_peer is null)) > 0 "
            val projection = arrayOf(hasIncomeColumn)
            disposable = briteContentResolver.createQuery(TransactionProvider.TRANSACTIONS_URI,
                    projection, null, null, null, false)
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
            val dateFilterList = settings.all.entries.map { it.key }.filter { it.startsWith("filter_date") }
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
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
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
            arrayOf(PrefKey.SORT_ORDER_ACCOUNTS, PrefKey.SORT_ORDER_CATEGORIES, PrefKey.SORT_ORDER_BUDGET_CATEGORIES).forEach {
                if (Sort.TITLE.name == prefHandler.getString(it, null)) {
                    prefHandler.putString(it, Sort.LABEL.name)
                }
            }
        }
        if (fromVersion < 417) {
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
            prefHandler.getString(PrefKey.CUSTOM_DATE_FORMAT, null)?.let {
                if (validateDateFormat(it) != null) {
                    Timber.d("Removed erroneous dateFormat %s ", it)
                    prefHandler.remove(PrefKey.CUSTOM_DATE_FORMAT)
                }
            }

            disposable = briteContentResolver.createQuery(TransactionProvider.TEMPLATES_URI, null, String.format(Locale.ROOT, "%s is not null",
                    DatabaseConstants.KEY_PLANID), null, null, false)
                    .mapToList { cursor -> Template(cursor) }
                    .subscribe { list ->
                        try {
                            for (template in list) {
                                Plan.updateDescription(template.planId, template.compileDescription(getApplication()))
                            }
                        } catch (e: SecurityException) {
                            //permission missing
                            CrashHandler.report(e)

                        }
                        dispose()
                    }
        }
        if (fromVersion < 429 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
            prefHandler.putString(PrefKey.UI_THEME_KEY, "default")
        }
    }
}