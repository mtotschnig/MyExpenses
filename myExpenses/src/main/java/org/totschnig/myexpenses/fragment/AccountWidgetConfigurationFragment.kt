package org.totschnig.myexpenses.fragment

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountWidgetConfigure
import org.totschnig.myexpenses.adapter.SortableItem
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TransactionType
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment.Button.Companion.marshall
import org.totschnig.myexpenses.fragment.AccountWidgetConfigurationFragment.Button.Companion.unmarshall
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.SimpleValuePreference
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel

@Suppress("unused")
class AccountWidgetConfigurationFragment : PreferenceFragmentCompat() {
    private val viewModel: ContentResolvingAndroidViewModel by viewModels()

    private val accountPreference: ListPreference
        get() = preferenceScreen.getPreference(0) as ListPreference

    private val buttonsPreference: SimpleValuePreference
        get() = preferenceScreen.getPreference(2) as SimpleValuePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(viewModel)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.accountsMinimal("$KEY_VISIBLE = 1")
                        .collect { list ->
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

    enum class Button(
        @StringRes val label: Int,
        @DrawableRes val icon: Int,
        @TransactionType val type: Int?
    ) {
        TRANSACTION(R.string.menu_create_transaction, R.drawable.ic_menu_add, TYPE_TRANSACTION),
        TRANSFER(R.string.menu_create_transfer, R.drawable.ic_menu_forward, TYPE_TRANSFER),
        SPLIT(R.string.menu_create_split, R.drawable.ic_menu_split, TYPE_SPLIT),
        SCAN(R.string.button_scan, R.drawable.ic_scan, null);

        companion object {

            private const val SEPARATOR = ","

            fun Iterable<Button>.marshall() = joinToString(SEPARATOR)

            fun unmarshall(value: String) = value.split(SEPARATOR).map { Button.valueOf(it) }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        preference.key.startsWith("ACCOUNT_WIDGET_BUTTONS") -> {
            SortUtilityDialogFragment.newInstance(
                ArrayList(
                    unmarshall((preference as SimpleValuePreference).value).map {
                        SortableItem(it.ordinal.toLong(), getString(it.label), it.icon)
                    }
                )
            ).show(childFragmentManager, "SORT_ACCOUNTS")
            true
        }

        else -> false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PREFS_NAME
        setPreferencesFromResource(R.xml.account_widget_configuration, rootKey)
        (requireActivity() as AccountWidgetConfigure).appWidgetId?.also { appWidgetId ->
            accountPreference.key = selectionKey(appWidgetId)
            preferenceScreen.getPreference(1).key = sumKey(appWidgetId)
            with(buttonsPreference) {
                title = "${getString(R.string.sort_order)} (${getString(R.string.buttons)})"
                summaryProvider = SummaryProvider<SimpleValuePreference> { preference ->
                    preference.value.split(",")
                        .joinToString { getString(Button.valueOf(it).label) }
                }
                key = buttonsKey(appWidgetId)
                value = Button.entries.marshall()
            }
        } ?: kotlin.run { requireActivity().finish() }
    }

    fun onSortOrderConfirmed(sortedIds: LongArray) {
        buttonsPreference.value = sortedIds.map { Button.entries[it.toInt()] }.marshall()
    }

    companion object {
        const val PREFS_NAME = "account_widget"

        fun selectionKey(appWidgetId: Int) = "ACCOUNT_WIDGET_SELECTION_$appWidgetId"

        fun sumKey(appWidgetId: Int) = "ACCOUNT_WIDGET_SUM_$appWidgetId"

        fun buttonsKey(appWidgetId: Int) = "ACCOUNT_WIDGET_BUTTONS_$appWidgetId"

        fun loadSelectionPref(context: Context, appWidgetId: Int) =
            sharedPreferences(context).getString(
                selectionKey(appWidgetId),
                Long.MAX_VALUE.toString()
            )!!

        fun loadSumPref(context: Context, appWidgetId: Int) =
            sharedPreferences(context).getString(sumKey(appWidgetId), "current_balance")!!

        fun loadButtons(context: Context, appWidgetId: Int) =
            sharedPreferences(context).getString(buttonsKey(appWidgetId), null)?.let {
                Button.unmarshall(it)
            } ?: Button.entries


        private fun sharedPreferences(context: Context) =
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        fun clearPreferences(context: Context, appWidgetId: Int) {
            sharedPreferences(context).edit {
                remove(selectionKey(appWidgetId))
                remove(sumKey(appWidgetId))
                remove(buttonsKey(appWidgetId))
            }
        }
    }
}