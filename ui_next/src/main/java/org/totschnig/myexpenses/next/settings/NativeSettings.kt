package org.totschnig.myexpenses.next.settings

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.next.core.common.R as NextR
import org.totschnig.myexpenses.fragment.preferences.PreferenceUiFragment.Companion.getLocaleDisplayName
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.*
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.validateDateFormat
import java.text.DateFormatSymbols
import java.text.DecimalFormatSymbols
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar

/** Where the value of a setting is kept */
enum class Storage { PREFERENCES, DATA_STORE }

/** Reads the current values of other settings, e.g. to find out whether one is turned off */
interface SettingValues {
    fun string(key: PrefKey): String?
    fun boolean(key: PrefKey): Boolean
}

/**
 * A setting that the new settings show themselves instead of opening its old screen. What is not
 * given here comes from its [LegacySpec].
 *
 * @param options value to label, instead of those of the old screen, e.g. the weekdays of the locale
 * @param defaultValue instead of the one of the old screen
 * @param isEnabled besides the dependency of the old screen, e.g. the quality only for lossy formats
 * @param isVisible false where the old screen hides the setting, e.g. crash reports in some builds
 * @param summary instead of the one of the old screen, which it sets in code
 * @param validate a text before it is stored, returns the error to show or null if it is fine
 * @param read / [write] instead of [storage], for a value that is kept elsewhere, e.g. the language
 * @param range of a slider, which then stops at every whole number, instead of min and max of the
 * old screen
 * @param valueText of a slider, as shown next to its title
 * @param sampleScale of the font of a sample text below a slider, e.g. for the font size
 * @param legend of a value of its own besides the [options], e.g. what the letters of a pattern
 * stand for; without it only the options can be picked
 * @param preview how a value of its own looks, e.g. today in a date pattern
 */
class NativeSetting(
    val storage: Storage = Storage.PREFERENCES,
    val options: (NextSettings.() -> List<Pair<String, String>>)? = null,
    val defaultValue: (Context.() -> String)? = null,
    val isEnabled: ((SettingValues) -> Boolean)? = null,
    val isVisible: (NextSettings.() -> Boolean)? = null,
    val summary: (Context.() -> String)? = null,
    val validate: (Context.(String) -> String?)? = null,
    val read: (NextSettings.() -> String?)? = null,
    val write: (NextSettings.(String) -> Unit)? = null,
    val range: IntRange? = null,
    val valueText: (NextSettings.(Int) -> String)? = null,
    val sampleScale: (NextSettings.(Int) -> Float)? = null,
    val legend: (NextSettings.() -> String)? = null,
    val preview: (NextSettings.(String) -> String?)? = null,
)

object NativeSettings {

    /**
     * As LocalizedFormatEditTextPreference: the format of the region until another one is set,
     * which is removed again when it is empty or the same as that of the region. Picked by an
     * example or typed as a pattern of its own.
     */
    private fun localizedFormat(
        key: PrefKey,
        default: NextSettings.() -> String?,
        patterns: NextSettings.() -> List<String>,
        example: NextSettings.(String) -> String?,
        legend: NextSettings.() -> String,
        validate: Context.(String) -> String?,
    ) = NativeSetting(
        options = {
            formatOptions(default(), patterns(), { example(it) }) { getString(NextR.string.next_settings_format_region, it) }
        },
        read = { prefHandler.getString(key, null)?.takeIf { it.isNotEmpty() } ?: default() },
        write = { format ->
            if (format.isEmpty() || format == default()) prefHandler.remove(key) else prefHandler.putString(key, format)
        },
        validate = { format -> if (format.isEmpty()) null else validate(format) },
        legend = legend,
        preview = { format -> if (format.isEmpty()) default()?.let { example(it) } else example(format) }
    )

    /**
     * A number picked from [values], and the one stored if it is none of them, e.g. one set by the
     * slider of the old screen
     */
    private fun numberChoice(
        key: PrefKey,
        default: Int,
        values: List<Int>,
        isEnabled: ((SettingValues) -> Boolean)? = null,
        label: NextSettings.(Int) -> String,
    ) = NativeSetting(
        options = {
            (values + prefHandler.getInt(key, default)).distinct().sorted().map { it.toString() to label(it) }
        },
        read = { prefHandler.getInt(key, default).toString() },
        write = { prefHandler.putInt(key, it.toInt()) },
        isEnabled = isEnabled
    )

    private val inDataStore = NativeSetting(storage = Storage.DATA_STORE)
    private val plain = NativeSetting()

    val all: Map<PrefKey, NativeSetting> = buildMap {
        // Appearance
        put(UI_THEME, plain)
        put(
            UI_FONT_SIZE, NativeSetting(
                // As FontSizeDialogPreference: steps of 10 % above the size of the system
                range = 0..3,
                valueText = { if (it == 0) getString(R.string.system_default) else "+${it * 10} %" },
                // The app shows its texts in the size set now already
                sampleScale = { (1 + it / 10f) / (1 + prefHandler.getInt(UI_FONT_SIZE, 0) / 10f) }
            )
        )
        put(
            UI_LANGUAGE, NativeSetting(
                options = {
                    resources.getStringArray(R.array.pref_ui_language_values).map { it to getLocaleDisplayName(it) }
                },
                read = {
                    AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: MyApplication.DEFAULT_LANGUAGE
                },
                write = { changeLanguage(it) }
            )
        )
        put(CONTENT_BASED_COLORS, plain)
        put(
            CUSTOM_DECIMAL_FORMAT, localizedFormat(
                CUSTOM_DECIMAL_FORMAT,
                default = { (NumberFormat.getCurrencyInstance(Utils.localeFromContext(this)) as DecimalFormat).toLocalizedPattern() },
                patterns = { numberPatterns.map(::localizedNumberPattern) },
                example = { numberExample(it, homeCurrency.code, homeCurrency.symbol) },
                legend = {
                    val symbols = DecimalFormatSymbols.getInstance()
                    getString(
                        NextR.string.next_settings_number_legend,
                        symbols.groupingSeparator.toString(), symbols.decimalSeparator.toString()
                    )
                },
                // As NumberFormatPreference
                validate = { format ->
                    try {
                        DecimalFormat().applyLocalizedPattern(format)
                        null
                    } catch (e: IllegalArgumentException) {
                        getString(R.string.number_format_illegal) + " (${e.safeMessage})"
                    }
                }
            )
        )
        put(
            CUSTOM_DATE_FORMAT, localizedFormat(
                CUSTOM_DATE_FORMAT,
                default = { (Utils.getFrameworkDateFormatSafe(this) as? SimpleDateFormat)?.toPattern() },
                patterns = { datePatterns },
                example = { dateExample(it, getLocale()) },
                legend = { getString(NextR.string.next_settings_date_legend) },
                validate = { validateDateFormat(this, it) }
            )
        )
        put(UI_START_SCREEN, plain)
        // Lists & periods
        put(UI_ITEM_RENDERER_LEGACY, inDataStore)
        put(UI_ITEM_RENDERER_ORIGINAL_AMOUNT, plain)
        put(
            TRANSACTION_AMOUNT_COLOR_SOURCE, NativeSetting(
                storage = Storage.DATA_STORE,
                options = {
                    val type = listOf(R.string.expense, R.string.income, R.string.transfer).joinToString(" / ", transform = ::getString)
                    val sign = listOf(R.string.pm_type_debit, R.string.pm_type_credit).joinToString(" / ", transform = ::getString)
                    listOf(
                        ColorSource.TYPE.name to type,
                        ColorSource.SIGN.name to sign,
                        ColorSource.TYPE_WITH_SIGN.name to "$sign / ${getString(R.string.transfer)}"
                    )
                }
            )
        )
        put(CRITERION_FUTURE, inDataStore)
        put(
            GROUP_WEEK_STARTS, NativeSetting(
                options = {
                    val weekdays = DateFormatSymbols(getLocale()).weekdays
                    (Calendar.SUNDAY..Calendar.SATURDAY).map { it.toString() to weekdays[it] }
                },
                defaultValue = { Utils.getFirstDayOfWeek(getLocale()).toString() }
            )
        )
        put(
            GROUP_MONTH_STARTS, NativeSetting(
                options = { (1..31).map { it.toString() to Utils.toLocalizedString(it) } }
            )
        )
        // Entering transactions
        put(
            DEFAULT_ACTION, NativeSetting(
                storage = Storage.DATA_STORE,
                // Without a fixed action, the plus button of the new account page opens a menu of all kinds
                options = {
                    listOf(
                        "LastVisited" to getString(NextR.string.next_settings_default_action_menu),
                        "Expense" to getString(R.string.expense),
                        "Income" to getString(R.string.income),
                        "Transfer" to getString(R.string.transfer),
                        "Split" to getString(R.string.split_transaction),
                        "Scan" to getString(R.string.button_scan)
                    )
                }
            )
        )
        listOf(
            AUTO_FILL_SWITCH, AUTO_FILL_AMOUNT, AUTO_FILL_CATEGORY, AUTO_FILL_COMMENT, AUTO_FILL_ACCOUNT,
            AUTO_FILL_METHOD, AUTO_FILL_DEBT, AUTO_FILL_FOCUS, TRANSACTION_WITH_TIME, TRANSACTION_WITH_VALUE_DATE,
            CLONE_WITH_CURRENT_DATE, CAMERA_CHOOSER, OPTIMIZE_PICTURE, OPTIMIZE_PICTURE_FORMAT, COPY_ATTACHMENT
        ).forEach { put(it, plain) }
        put(
            CAMERA_APP, NativeSetting(
                // The apps that can take a photo, and the one stored if it is not installed any more
                options = {
                    val stored = prefHandler.getString(CAMERA_APP).orEmpty()
                    val apps = packageManager.queryIntentActivities(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
                        .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
                        .distinctBy { it.first }
                        .sortedBy { it.second }
                    listOf("" to getString(R.string.system_default)) + apps +
                            listOfNotNull((stored to stored).takeIf { stored.isNotEmpty() && apps.none { it.first == stored } })
                },
                read = { prefHandler.getString(CAMERA_APP).orEmpty() }
            )
        )
        put(
            ATTACHMENT_MIME_TYPES, NativeSetting(
                // Kinds of files instead of MIME types, and those stored that are none of them
                options = {
                    val kinds = listOf(
                        "image/*" to getString(NextR.string.next_settings_mime_images),
                        "application/pdf" to "PDF",
                        "*/*" to getString(NextR.string.next_settings_mime_all)
                    )
                    val stored = prefHandler.getString(ATTACHMENT_MIME_TYPES)?.split(',')?.map { it.trim() }.orEmpty()
                    kinds + stored.filter { type -> type.isNotEmpty() && kinds.none { it.first == type } }.map { it to it }
                }
            )
        )
        put(
            OPTIMIZE_PICTURE_MAX_SIZE, numberChoice(OPTIMIZE_PICTURE_MAX_SIZE, 1000, listOf(1000, 2000, 4000)) { size ->
                when (size) {
                    1000 -> getString(NextR.string.next_settings_picture_small, size)
                    2000 -> getString(NextR.string.next_settings_picture_medium, size)
                    4000 -> getString(NextR.string.next_settings_picture_large, size)
                    else -> getString(NextR.string.next_settings_pixels, size)
                }
            }
        )
        put(
            OPTIMIZE_PICTURE_QUALITY, NativeSetting(
                isEnabled = { values -> values.string(OPTIMIZE_PICTURE_FORMAT) != "PNG" }
            )
        )
        // Manage data
        put(
            HOME_CURRENCY, NativeSetting(
                options = { currencies.map { it.code to it.toString() } },
                read = { homeCurrency.code },
                // As the old screen: only after a warning, as prices and equivalent amounts are calculated anew
                write = { code -> if (code != homeCurrency.code) homeCurrencyRequest = code }
            )
        )
        listOf(UNMAPPED_TRANSACTION_AS_TRANSFER, EQUIVALENT_AMOUNTS_AUTOMATIC_UPDATE, PLANNER_EXECUTION_TIME, PLANNER_MANUAL_TIME)
            .forEach { put(it, plain) }
        // Security & privacy, as the old screen: what depends on the screen lock only while it is on
        val whenProtected = NativeSetting(isEnabled = { it.boolean(PROTECTION_LEGACY) || it.boolean(PROTECTION_DEVICE_LOCK_SCREEN) })
        listOf(
            PROTECTION_ALLOW_SCREENSHOT, PROTECTION_ENABLE_ACCOUNT_WIDGET,
            PROTECTION_ENABLE_TEMPLATE_WIDGET, PROTECTION_ENABLE_BUDGET_WIDGET, PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET
        ).forEach { put(it, whenProtected) }
        put(
            PROTECTION_DELAY_SECONDS, numberChoice(
                PROTECTION_DELAY_SECONDS, 15, listOf(1, 15, 30, 60, 120, 300), whenProtected.isEnabled
            ) { seconds ->
                if (seconds < 60 || seconds % 60 != 0) resources.getQuantityString(NextR.plurals.next_settings_seconds, seconds, seconds)
                else resources.getQuantityString(NextR.plurals.next_settings_minutes, seconds / 60, seconds / 60)
            }
        )
        put(UI_HOME_SCREEN_SHORTCUTS_SHOW_NEW_BALANCE, plain)
        put(ENCRYPT_DATABASE_INFO, NativeSetting(isVisible = { prefHandler.encryptDatabase }))
        val withCrashReports = { DistributionHelper.distribution.supportsTrackingAndCrashReporting }
        put(TRACKING, NativeSetting(isVisible = { withCrashReports() }))
        put(
            CRASHREPORT_ENABLED, NativeSetting(
                isVisible = { withCrashReports() },
                summary = { Utils.getTextWithAppName(this, R.string.crash_reports_user_info).toString() }
            )
        )
        put(CRASHREPORT_USEREMAIL, NativeSetting(isVisible = { withCrashReports() }))
        // Advanced
        listOf(WEBDAV_ALLOW_UNVERIFIED_HOST, DEBUG_LOGGING, DB_SAFE_MODE).forEach { put(it, plain) }
        put(
            WEBDAV_TIMEOUT, NativeSetting(
                valueText = { resources.getQuantityString(NextR.plurals.next_settings_seconds, it, it) }
            )
        )
        // As the old screen: only in some builds
        put(DEBUG_ADS, NativeSetting(isVisible = { resources.getBoolean(R.bool.pref_debug_show_ads_visible) }))
        // Synchronization
        put(
            SYNC_FREQUCENCY, numberChoice(SYNC_FREQUCENCY, 12, listOf(0, 1, 3, 6, 12, 24)) { hours ->
                // As the old screen: 0 turns automatic synchronization off
                if (hours == 0) getString(NextR.string.next_settings_sync_by_hand)
                else resources.getQuantityString(NextR.plurals.next_settings_every_hours, hours, hours)
            }
        )
        listOf(SYNC_WIFI_ONLY, SYNC_CHANGES_IMMEDIATELY, SYNC_NOTIFICATION, SYNC_NOW_ALL, WEBUI_PASSWORD, WEBUI_HTTPS)
            .forEach { put(it, plain) }
        // Backup & import
        listOf(
            AUTO_BACKUP, AUTO_BACKUP_TIME, BACKUP_FILE_PREFIX, EXPORT_PASSWORD, PURGE_BACKUP, PURGE_BACKUP_KEEP,
            PURGE_BACKUP_REQUIRE_CONFIRMATION, PERFORM_SHARE, CSV_EXPORT_SPLIT_CATEGORIES, CSV_EXPORT_CATEGORY_SEPARATOR,
            CSV_EXPORT_SPLIT_AMOUNT, CSV_EXPORT_SPLIT_DATE_TIME, CSV_EXPORT_ORIGINAL_EQUIVALENT_AMOUNTS
        ).forEach { put(it, plain) }
        put(
            AUTO_BACKUP_UNENCRYPTED_INFO, NativeSetting(
                isVisible = { prefHandler.encryptDatabase },
                summary = { getString(R.string.warning_unencrypted_backup, getString(R.string.pref_security_export_passphrase_title)) }
            )
        )
        put(
            SHARE_TARGET, NativeSetting(
                summary = {
                    getString(R.string.pref_share_target_summary) + " " + BaseFunctionalityViewModel.Scheme.entries
                        .joinToString(separator = ", ", prefix = "(", postfix = ")") { it.name.lowercase() }
                },
                validate = { target ->
                    // As the old screen
                    if (target.isEmpty()) null else {
                        val uri = BaseFunctionalityViewModel.parseUri(target)
                        when {
                            uri == null -> getString(R.string.ftp_uri_malformed, target)
                            BaseFunctionalityViewModel.Scheme.entries.none { it.name == uri.scheme.uppercase() } ->
                                getString(R.string.share_scheme_not_supported, uri.scheme)
                            else -> null
                        }
                    }
                }
            )
        )
    }
}
