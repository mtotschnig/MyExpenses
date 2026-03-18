package org.totschnig.myexpenses.fragment.preferences

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.Version
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.dialog.AccountListDisplayConfigurationDialogFragment
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment
import org.totschnig.myexpenses.dialog.CustomizeMenuDialogFragment
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getLocale
import timber.log.Timber
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Keep
class PreferenceUiFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_ui

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<TwoStatePreference>(PrefKey.UI_ITEM_RENDERER_LEGACY).let {
            it.title = requireContext().compactItemRendererTitle()
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    preferenceDataStore.handleToggle(it)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceDataStore.handleToggle(requirePreference(PrefKey.UI_ITEM_RENDERER_CATEGORY_ICON))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceDataStore.handleToggle(requirePreference(PrefKey.GROUP_HEADER))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceDataStore.handleList(requirePreference(PrefKey.CRITERION_FUTURE)) {
                    requireContext().contentResolver.notifyChange(ACCOUNTS_URI, null, false)
                }
            }
        }

        val colorSourcePreference =
            requirePreference<ListPreference>(PrefKey.TRANSACTION_AMOUNT_COLOR_SOURCE)
        val expenseColor = getColor(resources, R.color.colorExpense, null)
        val incomeColor = getColor(resources, R.color.colorIncome, null)
        val transferColor = getColor(resources, R.color.colorTransfer, null)
        val colorSources = arrayOf(
            ColorSource.TYPE to buildSpannedString {
                color(expenseColor) {
                    append(getString(R.string.expense))
                }
                append(" / ")
                color(incomeColor) {
                    append(getString(R.string.income))
                }
                append(" / ")
                color(transferColor) {
                    append(getString(R.string.transfer))
                }
            },
            ColorSource.SIGN to buildSpannedString {
                color(expenseColor) {
                    append(getString(R.string.pm_type_debit))
                }
                append(" / ")
                color(incomeColor) {
                    append(getString(R.string.pm_type_credit))
                }
            },
            ColorSource.TYPE_WITH_SIGN to buildSpannedString {
                color(expenseColor) {
                    append(getString(R.string.pm_type_debit))
                }
                append(" / ")
                color(incomeColor) {
                    append(getString(R.string.pm_type_credit))
                }
                append(" / ")
                color(transferColor) {
                    append(getString(R.string.transfer))
                }
            }
        )
        colorSourcePreference.entryValues = colorSources.map { it.first.name }.toTypedArray()
        colorSourcePreference.entries = colorSources.map { it.second }.toTypedArray()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceDataStore.handleList(colorSourcePreference)
            }
        }

        with(requirePreference<Preference>(PrefKey.SHORTCUT_CREATE_SPLIT)) {
            if (licenceHandler.isContribEnabled) {
                isEnabled = true
            } else {
                summary =
                    ContribFeature.SPLIT_TRANSACTION.buildRequiresString(requireActivity())
            }
        }

        with(requirePreference<Preference>(PrefKey.SHORTCUT_CREATE_TRANSFER)) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val transferEnabled = viewModel.isTransferEnabled
                    withContext(Dispatchers.Main) {
                        if (transferEnabled) {
                            isEnabled = true
                        } else {
                            summary =
                                context.getString(R.string.dialog_command_disabled_insert_transfer)
                        }
                    }
                }
            }
        }

        with(requirePreference<ListPreference>(PrefKey.GROUP_WEEK_STARTS)) {
            val locale = preferenceActivity.getLocale()
            val dfs = DateFormatSymbols(locale)
            val entries = arrayOfNulls<String>(7)
            System.arraycopy(dfs.weekdays, 1, entries, 0, 7)
            this.entries = entries
            entryValues = arrayOf(
                (Calendar.SUNDAY).toString(),
                (Calendar.MONDAY).toString(),
                (Calendar.TUESDAY).toString(),
                (Calendar.WEDNESDAY).toString(),
                (Calendar.THURSDAY).toString(),
                (Calendar.FRIDAY).toString(),
                (Calendar.SATURDAY).toString()
            )
            if (!prefHandler.isSet(PrefKey.GROUP_WEEK_STARTS)) {
                value = Utils.getFirstDayOfWeek(locale).toString()
            }
        }

        with(requirePreference<ListPreference>(PrefKey.GROUP_MONTH_STARTS)) {
            val daysEntries = arrayOfNulls<String>(31)
            val daysValues = arrayOfNulls<String>(31)
            for (i in 1..31) {
                daysEntries[i - 1] = Utils.toLocalizedString(i)
                daysValues[i - 1] = (i).toString()
            }
            entries = daysEntries
            entryValues = daysValues
        }

        with(requirePreference<Preference>(PrefKey.ACCOUNT_LIST_DISPLAY_CONFIGURATION)) {
            title =
                getString(R.string.menu_grouping) + " / " + getString(R.string.display_options_sort_list_by)
        }

        childFragmentManager.setFragmentResultListener(
            LegacyUIDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            if (bundle.getBoolean(LegacyUIDialogFragment.RESULT_CONFIRMED)) {
                requirePreference<ListPreference>(PrefKey.UI_MAIN_SCREEN_VERSION).value = Version.V1.name
                configureUiVersionDependencies()
            }
        }

        with(requirePreference<Preference>(PrefKey.UI_MAIN_SCREEN_VERSION)) {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == Version.V2.name) true else {
                    LegacyUIDialogFragment().show(childFragmentManager, "LEGACY_UI")
                    false
                }
            }
        }

        configureUiVersionDependencies()
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.SHORTCUT_CREATE_TRANSACTION) -> {
            addShortcut(
                TransactionsContract.Transactions.TYPE_TRANSACTION
            )
            true
        }

        matches(preference, PrefKey.SHORTCUT_CREATE_TRANSFER) -> {
            addShortcut(
                TransactionsContract.Transactions.TYPE_TRANSFER
            )
            true
        }

        matches(preference, PrefKey.SHORTCUT_CREATE_SPLIT) -> {
            addShortcut(
                TransactionsContract.Transactions.TYPE_SPLIT
            )
            true
        }

        matches(preference, PrefKey.CUSTOMIZE_MAIN_MENU) -> {
            CustomizeMenuDialogFragment.newInstance()
                .show(childFragmentManager, "CUSTOMIZE_MENU")
            true
        }

        matches(preference, PrefKey.CUSTOMIZE_MENU_V2) -> {
            (preference as PopupMenuPreference).showPopupMenu(
                "Main Navigation", getString(R.string.import_select_transactions)
            ) { item ->
                CustomizeMenuDialogFragment.newInstance(
                    if (item.itemId == 0) MenuItem.MenuContext.V2Navigation
                    else MenuItem.MenuContext.V2Transactions
                ).show(childFragmentManager, "CUSTOMIZE_MENU")
                true
            }
            true
        }

        matches(preference, PrefKey.ACCOUNT_LIST_DISPLAY_CONFIGURATION) -> {
            AccountListDisplayConfigurationDialogFragment().show(
                childFragmentManager,
                "ACCOUNT_LIST_DISPLAY_CONFIGURATION"
            )
            true
        }

        else -> false
    }

    private fun addShortcut(
        @TransactionsContract.Transactions.TransactionType operationType: Int,
    ) {
        try {
            requireContext().getSystemService(ShortcutManager::class.java)
                .requestPinShortcut(
                    ShortcutInfo.Builder(
                        requireContext(), when (operationType) {
                            TransactionsContract.Transactions.TYPE_SPLIT -> ShortcutHelper.ID_SPLIT
                            TransactionsContract.Transactions.TYPE_TRANSACTION -> ShortcutHelper.ID_TRANSACTION
                            TransactionsContract.Transactions.TYPE_TRANSFER -> ShortcutHelper.ID_TRANSFER
                            else -> throw IllegalStateException()
                        }
                    ).build(),
                    null
                )
        } catch (e: IllegalArgumentException) {
            Timber.w("requestPinShortcut failed for %d", operationType)
            CrashHandler.report(e)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        //we configure language picker here, so that we can override the outdated value
        //that might get set in ListPreference onRestoreInstanceState, when activity is recreated
        //due to user changing app language in Android 13 system settings
        findPreference<ListPreference>(PrefKey.UI_LANGUAGE)?.apply {
            entries = getLocaleArray()
            value = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
                ?: MyApplication.DEFAULT_LANGUAGE
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val newLocale = newValue as String
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && newLocale != MyApplication.DEFAULT_LANGUAGE) {
                        featureManager.requestLocale(newLocale)
                    } else {
                        preferenceActivity.setLanguage(newLocale)
                    }
                    value = newValue
                    false
                }
        }
    }


    private fun getLocaleArray() =
        requireContext().resources.getStringArray(R.array.pref_ui_language_values)
            .map(this::getLocaleDisplayName)
            .toTypedArray()

    private fun getLocaleDisplayName(localeString: String) =
        if (localeString == "default") {
            requireContext().getString(R.string.system_default)
        } else {
            val locale = Locale.forLanguageTag(localeString)
            locale.getDisplayName(locale)
        }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            getKey(PrefKey.UI_MAIN_SCREEN_VERSION) -> configureUiVersionDependencies()
        }
    }

    private fun configureUiVersionDependencies() {
        val legacy = prefHandler.mainScreenLegacy
        requirePreference<Preference>(PrefKey.CUSTOMIZE_MAIN_MENU).isVisible = legacy
        requirePreference<Preference>(PrefKey.UI_START_SCREEN).isVisible = !legacy
        requirePreference<Preference>(PrefKey.CUSTOMIZE_MENU_V2).isVisible = !legacy
    }

    companion object {
        fun Context.compactItemRendererTitle() =
            "${getString(R.string.style)} : ${getString(R.string.compact)}"
    }
}

class LegacyUIDialogFragment : ComposeBaseDialogFragment(), DialogInterface.OnClickListener {
    @Composable
    override fun BuildContent() {
        MigrationFeedbackCard()
    }

    @Composable
    private fun MigrationFeedbackCard() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.migration_v2_opt_out_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.migration_v2_opt_out_warning),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.migration_v2_confirm_opt_out, this@LegacyUIDialogFragment)
            setNeutralButton(R.string.feedback, this@LegacyUIDialogFragment)
        }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            AlertDialog.BUTTON_POSITIVE -> {
                prefHandler.mainScreenLegacy = true
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_CONFIRMED to true))
                dismiss()
            }

            AlertDialog.BUTTON_NEUTRAL -> {

                (requireActivity() as BaseActivity).sendEmail(
                    recipient = getString(R.string.support_email),
                    subject = "[" + getString(R.string.app_name) + "] " + getString(R.string.feedback) + " : " + getString(R.string.migration_v2_feedback_title),
                    body = ""
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "LEGACY_UI_REQUEST"
        const val RESULT_CONFIRMED = "confirmed"
    }
}