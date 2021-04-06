package org.totschnig.myexpenses.fragment

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountWidgetConfigure
import org.totschnig.myexpenses.viewmodel.AccountWidgetConfigurationViewModel

@Suppress("unused")
class AccountWidgetConfigurationFragment : PreferenceFragmentCompat() {
    val viewModel: AccountWidgetConfigurationViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PREFS_NAME
        setPreferencesFromResource(R.xml.account_widget_configuration, rootKey)
        (requireActivity() as AccountWidgetConfigure).appWidgetId?.also {
            val accountPreference: ListPreference = preferenceScreen.getPreference(0) as ListPreference
            accountPreference.key = selectionKey(it)
            preferenceScreen.getPreference(1).key = sumKey(it)
            viewModel.accountsMinimal.observe(this) {
                with(accountPreference) {
                    entries = (it.map { it.label } + getString(R.string.budget_filter_all_accounts)).toTypedArray()
                    entryValues = (it.map { it.id.toString() } + Long.MAX_VALUE.toString()).toTypedArray()
                    value = Long.MAX_VALUE.toString()
                }
            }
        } ?: kotlin.run { requireActivity().finish() }
    }

    companion object {
        const val PREFS_NAME = "account_widget"

        fun selectionKey(appWidgetId: Int) = "ACCOUNT_WIDGET_SELECTION_$appWidgetId"

        fun sumKey(appWidgetId: Int) = "ACCOUNT_WIDGET_SUM_$appWidgetId"

        fun loadSelectionPref(context: Context, appWidgetId: Int) =
                sharedPreferences(context).getString(selectionKey(appWidgetId), Long.MAX_VALUE.toString())!!

        fun loadSumPref(context: Context, appWidgetId: Int) =
                sharedPreferences(context).getString(sumKey(appWidgetId), "current_balance")!!

        private fun sharedPreferences(context: Context) =
                context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        fun clearPreferences(context: Context, appWidgetId: Int) {
            sharedPreferences(context).edit().remove(selectionKey(appWidgetId)).remove(sumKey(appWidgetId)).apply()
        }
    }
}