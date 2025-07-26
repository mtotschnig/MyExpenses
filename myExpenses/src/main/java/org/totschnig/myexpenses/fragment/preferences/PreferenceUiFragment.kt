package org.totschnig.myexpenses.fragment.preferences

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.text.buildSpannedString
import androidx.core.text.color
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
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.dialog.AccountListDisplayConfigurationDialogFragment
import org.totschnig.myexpenses.dialog.CustomizeMenuDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.ui.UiUtils
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

        val colorSourcePreference = requirePreference<ListPreference>(PrefKey.TRANSACTION_AMOUNT_COLOR_SOURCE)
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
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.SHORTCUT_CREATE_TRANSACTION) -> {
            addShortcut(
                R.string.transaction, TransactionsContract.Transactions.TYPE_TRANSACTION,
                R.drawable.shortcut_create_transaction_icon_lollipop
            )
            true
        }

        matches(preference, PrefKey.SHORTCUT_CREATE_TRANSFER) -> {
            addShortcut(
                R.string.transfer, TransactionsContract.Transactions.TYPE_TRANSFER,
                R.drawable.shortcut_create_transfer_icon_lollipop
            )
            true
        }

        matches(preference, PrefKey.SHORTCUT_CREATE_SPLIT) -> {
            addShortcut(
                R.string.split_transaction, TransactionsContract.Transactions.TYPE_SPLIT,
                R.drawable.shortcut_create_split_icon_lollipop
            )
            true
        }

        matches(preference, PrefKey.CUSTOMIZE_MAIN_MENU) -> {
            CustomizeMenuDialogFragment()
                .show(childFragmentManager, "CUSTOMIZE_MENU")
            true
        }

        matches(preference, PrefKey.ACCOUNT_LIST_DISPLAY_CONFIGURATION) -> {
            AccountListDisplayConfigurationDialogFragment().show(childFragmentManager, "ACCOUNT_LIST_DISPLAY_CONFIGURATION")
            true
        }

        else -> false
    }

    private fun addShortcut(
        nameId: Int,
        @TransactionsContract.Transactions.TransactionType operationType: Int,
        @DrawableRes iconIdLegacy: Int
    ) {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 -> {
                addShortcutLegacy(nameId, operationType, getBitmapForShortcut(iconIdLegacy))
            }
            //on Build.VERSION_CODES.N_MR1 we do not provide the feature
            Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 -> {
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
        }
    }

    // credits Financisto
    // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
    @Suppress("DEPRECATION")
    private fun addShortcutLegacy(nameId: Int, operationType: Int, bitmap: Bitmap) {
        val shortcutIntent =
            ShortcutHelper.createIntentForNewTransaction(requireContext(), operationType)

        val intent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(nameId))
            putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
            action = "com.android.launcher.action.INSTALL_SHORTCUT"
        }

        if (Utils.isIntentReceiverAvailable(requireActivity(), intent)) {
            requireActivity().sendBroadcast(intent)
            preferenceActivity.showSnackBar(getString(R.string.pref_shortcut_added))
        } else {
            preferenceActivity.showSnackBar(getString(R.string.pref_shortcut_not_added))
        }
    }

    private fun getBitmapForShortcut(@DrawableRes iconId: Int) = UiUtils.drawableToBitmap(
        ResourcesCompat.getDrawable(
            resources,
            iconId,
            null
        )!!
    )

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

    companion object {
        fun Context.compactItemRendererTitle() =
            "${getString(R.string.style)} : ${getString(R.string.compact)}"
    }
}