package org.totschnig.myexpenses.fragment

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountWidgetConfigure
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.AccountWidgetConfigurationViewModel

@Suppress("unused")
class AccountWidgetConfigurationFragment : PreferenceFragmentCompat() {
    private val viewModel: AccountWidgetConfigurationViewModel by viewModels()

    private val accountPreference: ListPreference
        get() = preferenceScreen.getPreference(0) as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        (requireActivity().application as MyApplication).appComponent.inject(viewModel)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.accountsMinimal("${DatabaseConstants.KEY_HIDDEN} = 0").collect { list ->
                        with(accountPreference) {
                            entries =
                                (list.map { it.label } + getString(R.string.budget_filter_all_accounts)).toTypedArray()
                            entryValues =
                                (list.map { it.id.toString() } + Long.MAX_VALUE.toString()).toTypedArray()
                            if (value == null || !list.any { it.id.toString() == value }) {
                                value = Long.MAX_VALUE.toString()
                            }
                        }
                    }

                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PREFS_NAME
        setPreferencesFromResource(R.xml.account_widget_configuration, rootKey)
        (requireActivity() as AccountWidgetConfigure).appWidgetId?.also { appWidgetId ->
            accountPreference.key = selectionKey(appWidgetId)
            preferenceScreen.getPreference(1).key = sumKey(appWidgetId)
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
            sharedPreferences(context).edit {
                remove(selectionKey(appWidgetId))
                remove(sumKey(appWidgetId))
            }
        }
    }
}