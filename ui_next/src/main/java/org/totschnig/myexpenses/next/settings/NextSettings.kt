package org.totschnig.myexpenses.next.settings

import android.appwidget.AppWidgetProvider
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.changeHomeCurrency
import org.totschnig.myexpenses.designsystem.ConfirmDialog
import org.totschnig.myexpenses.designsystem.DestructiveTextButton
import org.totschnig.myexpenses.designsystem.MultiChoiceSheet
import org.totschnig.myexpenses.designsystem.PickDialog
import org.totschnig.myexpenses.dialog.CustomizeMenuDialogFragment
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.dialog.MoreInfoDialogFragment
import org.totschnig.myexpenses.dialog.SelectDefaultTransferCategoryDialogFragment
import org.totschnig.myexpenses.dialog.SelectDefaultTransferCategoryDialogFragment.Companion.SELECT_CATEGORY_REQUEST
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.next.NextData
import org.totschnig.myexpenses.next.NextFeatures
import org.totschnig.myexpenses.next.NextTheme
import org.totschnig.myexpenses.next.core.common.R as NextR
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.provider.KEY_PATH
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.service.AutoBackupWorker
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.FileInfo
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ui.setNightMode
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.ManageCategoriesViewModel
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.PriceCalculationViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import java.io.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The settings of the new UI, see [SettingsCatalog]. Settings in [NativeSettings] are changed
 * right here, the others open the old screen that holds them, without the list of all old screens.
 */
class NextSettings : ProtectedFragmentActivity() {

    private val specs by lazy { LegacySpecs(this, prefHandler) }

    private val dataStore get() = injector.preferencesDataStore()

    /** Changes with every change of the preferences, so that the controls read them anew */
    private var preferencesVersion by mutableIntStateOf(0)

    private var dataStorePreferences by mutableStateOf<Preferences?>(null)

    /** A choice between actions, e.g. the menus that can be customized */
    private class PickRequest(val title: String, val options: List<Pair<String, () -> Unit>>)

    private var pickRequest by mutableStateOf<PickRequest?>(null)

    private val settingsViewModel: SettingsViewModel by viewModels()

    private val ledgerSource get() = NextData.of(this).ledgerSource

    /** The settings of the areas of the new UI, e.g. the rules of categories */
    private val areaSettings by lazy { NextFeatures.settings.associateWith { it.create(this) } }

    private var hasStaleImages by mutableStateOf(false)
    private var isDataCorrupted by mutableStateOf(false)
    private var offersCalendarRemoval by mutableStateOf(false)
    private var isPrivacyOptionsRequired by mutableStateOf(false)

    private val categoryViewModel: ManageCategoriesViewModel by viewModels()

    private val currencyViewModel: CurrencyViewModel by viewModels()
    private val progressViewModel: ModalProgressViewModel by viewModels()
    private val priceCalculationViewModel: PriceCalculationViewModel by viewModels()

    /** The currencies the home currency is picked from */
    internal var currencies by mutableStateOf<List<Currency>>(emptyList())
        private set

    /** The home currency picked, until the user agrees to calculate amounts anew */
    internal var homeCurrencyRequest by mutableStateOf<String?>(null)

    /** Path of the category of transfers, as the old screen shows it */
    private var defaultTransferCategory by mutableStateOf<String?>(null)

    /** Files in the app directory, e.g. backups and exports, and those picked to share or delete */
    private var appFiles by mutableStateOf<List<FileInfo>>(emptyList())
    private var isAppFilesSheetShown by mutableStateOf(false)
    private var pickedAppFiles by mutableStateOf<Set<String>>(emptySet())
    private var isDeleteConfirmationShown by mutableStateOf(false)

    private val values = object : SettingValues {
        override fun string(key: PrefKey) = prefHandler.getString(key)
        override fun boolean(key: PrefKey) = prefHandler.getBoolean(key, false)
    }

    private val legacyScreen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Handed on to the main screen, which restarts after a restore
        if (it.resultCode == RESULT_RESTORE_OK || it.resultCode == RESULT_INVALIDATE_OPTIONS_MENU) {
            setResult(it.resultCode)
            if (it.resultCode == RESULT_RESTORE_OK) finish()
        }
    }

    private val restore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_RESTORE_OK) {
            setResult(RESULT_RESTORE_OK)
            finish()
        }
    }

    private val pickAppDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefHandler.putString(PrefKey.APP_DIR, it.toString())
            settingsViewModel.loadAppDirInfo()
            settingsViewModel.loadAppData()
        }
    }

    private val actions = object : SettingsActions {
        override fun title(entry: SettingEntry) = specs.title(entry)

        override fun explanation(entry: SettingEntry) =
            (entry.key as? SettingKey.Pref)?.prefKey?.let(SettingTexts.explanations::get)?.let(::getString)

        override fun details(entry: SettingEntry): String {
            (entry.key as? SettingKey.Next)?.setting?.summary?.let { return getString(it) }
            val native = (entry.key as? SettingKey.Pref)?.prefKey?.let(NativeSettings.all::get)
            val spec = specs.spec(entry)
            return listOfNotNull(
                explanation(entry),
                native?.summary?.invoke(this@NextSettings) ?: spec?.summary,
                spec?.summaryOn,
                spec?.summaryOff,
                (native?.options?.invoke(this@NextSettings) ?: spec?.options)?.joinToString(" ") { it.second }
            ).joinToString(" ")
        }

        override fun isVisible(entry: SettingEntry): Boolean {
            // Read, so that entries show up and hide when the version of the main screen changes
            preferencesVersion
            val prefKey = (entry.key as? SettingKey.Pref)?.prefKey ?: return true
            NativeSettings.all[prefKey]?.isVisible?.let { return it(this@NextSettings) }
            // As the old screens
            return when (prefKey) {
                PrefKey.MANAGE_STALE_IMAGES -> hasStaleImages
                PrefKey.MANAGE_APP_DIR_FILES -> appFiles.isNotEmpty()
                PrefKey.DEBUG_REPAIR_987 -> isDataCorrupted
                PrefKey.REMOVE_LOCAL_CALENDAR -> offersCalendarRemoval
                PrefKey.NO_ADS -> !adHandlerFactory.isAdDisabled
                PrefKey.PERSONALIZED_AD_CONSENT -> !adHandlerFactory.isAdDisabled && isPrivacyOptionsRequired
                PrefKey.FEATURE_UNINSTALL_FEATURES, PrefKey.FEATURE_UNINSTALL_LANGUAGES -> featureManager.allowsUninstall()
                else -> true
            }
        }

        override fun canOpen(entry: SettingEntry) = when (entry.legacy) {
            null -> entry.key is SettingKey.Next
            LegacyScreen.MAIN -> (entry.key as? SettingKey.Pref)?.prefKey in nativeMainEntries
            else -> true
        }

        override fun open(entry: SettingEntry) {
            val prefKey = (entry.key as? SettingKey.Pref)?.prefKey
            val intent = specs.spec(entry)?.intent
            when {
                entry.legacy == null -> {}
                entry.legacy == LegacyScreen.MAIN -> openMainEntry(prefKey!!)
                // As the old screen: after a restore the main screen starts anew
                prefKey == PrefKey.RESTORE && intent != null -> restore.launch(intent)
                // The old screen starts the intent itself, unless it handles the click
                intent != null -> startActivity(intent)
                prefKey == PrefKey.IMPORT_CSV -> contribFeatureRequested(ContribFeature.CSV_IMPORT)
                prefKey == PrefKey.CUSTOMIZE_MENU_V2 -> customizeMenu(specs.title(entry))
                prefKey == PrefKey.RATE -> {
                    prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, -1)
                    dispatchCommand(R.id.RATE_COMMAND, null)
                }
                prefKey == PrefKey.SEND_FEEDBACK -> dispatchCommand(R.id.FEEDBACK_COMMAND, null)
                prefKey == PrefKey.TELL_A_FRIEND -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, Utils.getTellAFriendMessage(this@NextSettings).toString())
                    type = "text/plain"
                }, getString(R.string.menu_share)))
                prefKey == PrefKey.DEFAULT_TRANSFER_CATEGORY ->
                    SelectDefaultTransferCategoryDialogFragment().show(supportFragmentManager, "SELECT_DEFAULT")
                prefKey == PrefKey.NO_ADS -> contribFeatureRequested(ContribFeature.AD_FREE)
                prefKey == PrefKey.PERSONALIZED_AD_CONSENT -> checkGdprConsent(true)
                entry.legacy == LegacyScreen.CONTRIB && !licenceHandler.hasAnyLicence ->
                    dispatchCommand(R.id.CONTRIB_INFO_COMMAND, null)
                else -> openLegacy(entry.legacy)
            }
        }

        @Composable
        override fun page(setting: InsightSetting, onBack: () -> Unit) = InsightPage(setting, onBack)

        @Composable
        override fun control(entry: SettingEntry): SettingControl? {
            (entry.key as? SettingKey.Next)?.let { return insightControl(it.setting) }
            val prefKey = (entry.key as? SettingKey.Pref)?.prefKey ?: return null
            if (prefKey == PrefKey.DEFAULT_TRANSFER_CATEGORY) {
                return SettingControl.Action(defaultTransferCategory, isEnabled = true) { open(entry) }
            }
            val native = NativeSettings.all[prefKey] ?: return null
            val spec = specs.spec(entry) ?: return null
            val explanation = explanation(entry)
            // Read, so that the control is shown anew when a value changes
            preferencesVersion
            val stored = dataStorePreferences
            val key = prefHandler.getKey(prefKey)
            val default = native.defaultValue?.invoke(this@NextSettings) ?: spec.defaultValue
            val dependencyMet = spec.dependency?.let { settings.getBoolean(it, specs.defaultOf(it) == "true") } ?: true
            val nativeEnabled = native.isEnabled?.invoke(values) ?: true
            val isEnabled = dependencyMet && nativeEnabled &&
                    // until the values of the data store are read
                    (native.storage == Storage.PREFERENCES || stored != null)
            return when (entry.kind) {
                SettingKind.SWITCH -> {
                    val checked = if (native.storage == Storage.DATA_STORE)
                        stored?.get(booleanPreferencesKey(key)) ?: (default == "true")
                    else settings.getBoolean(key, default == "true")
                    SettingControl.Switch(
                        checked = checked,
                        summary = native.summary?.invoke(this@NextSettings) ?: explanation
                            ?: (if (checked) spec.summaryOn else spec.summaryOff) ?: spec.summary,
                        isEnabled = isEnabled,
                        onChange = { checked ->
                            if (native.storage == Storage.DATA_STORE) editDataStore(prefKey) { it[booleanPreferencesKey(key)] = checked }
                            else settings.edit().putBoolean(key, checked).apply()
                        }
                    )
                }
                SettingKind.CHOICE -> SettingControl.Choice(
                    selected = native.read?.invoke(this@NextSettings) ?: if (native.storage == Storage.DATA_STORE)
                        stored?.get(stringPreferencesKey(key)) ?: default
                    else settings.getString(key, default),
                    options = native.options?.invoke(this@NextSettings) ?: spec.options ?: return null,
                    explanation = explanation,
                    custom = native.legend?.let { legend ->
                        SettingControl.CustomValue(
                            legend = legend(this@NextSettings),
                            preview = { native.preview?.invoke(this@NextSettings, it) },
                            error = { native.validate?.invoke(this@NextSettings, it) }
                        )
                    },
                    isEnabled = isEnabled,
                    onSelect = { value ->
                        when {
                            native.write != null -> native.write.invoke(this@NextSettings, value)
                            native.storage == Storage.DATA_STORE ->
                                editDataStore(prefKey) { it[stringPreferencesKey(key)] = value }
                            else -> settings.edit().putString(key, value).apply()
                        }
                    }
                )
                SettingKind.TEXT, SettingKind.PASSWORD -> SettingControl.Text(
                    value = native.read?.invoke(this@NextSettings) ?: settings.getString(key, default) ?: "",
                    isNumber = false,
                    isEnabled = isEnabled,
                    onChange = { text ->
                        val error = native.validate?.invoke(this@NextSettings, text)
                        when {
                            error != null -> showSnackBar(error)
                            native.write != null -> native.write.invoke(this@NextSettings, text)
                            else -> settings.edit().putString(key, text).apply()
                        }
                    },
                    summary = native.summary?.invoke(this@NextSettings) ?: explanation ?: spec.summary,
                    isPassword = entry.kind == SettingKind.PASSWORD
                )
                SettingKind.NUMBER -> SettingControl.Text(
                    value = settings.getInt(key, default?.toIntOrNull() ?: 0).toString(),
                    isNumber = true,
                    isEnabled = isEnabled,
                    onChange = { text ->
                        text.toIntOrNull()?.let { settings.edit().putInt(key, it.coerceAtLeast(spec.min ?: it)).apply() }
                    }
                )
                SettingKind.SLIDER -> SettingControl.Slider(
                    value = settings.getInt(key, default?.toIntOrNull() ?: 0),
                    range = native.range ?: (spec.min ?: 0)..(spec.max ?: 100),
                    isEnabled = isEnabled,
                    onChange = { settings.edit().putInt(key, it).apply() },
                    isStepped = native.range != null,
                    valueText = { value -> native.valueText?.invoke(this@NextSettings, value) ?: value.toString() },
                    sampleScale = native.sampleScale?.let { scale -> { scale(this@NextSettings, it) } }
                )
                SettingKind.MULTI_CHOICE -> SettingControl.MultiChoice(
                    // As the old screen: the values separated by commas
                    selected = (settings.getString(key, default) ?: "").split(',')
                        .map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                    options = native.options?.invoke(this@NextSettings) ?: spec.options ?: return null,
                    isEnabled = isEnabled,
                    onChange = { values -> settings.edit().putString(key, values.joinToString(",")).apply() }
                )
                SettingKind.TIME -> {
                    // As TimePreference: hours times 100 plus minutes
                    val value = settings.getInt(key, default?.toIntOrNull() ?: TimePreference.DEFAULT_VALUE)
                    val time = LocalTime.of(value / 100, value % 100)
                    SettingControl.Time(
                        hour = time.hour,
                        minute = time.minute,
                        text = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        isEnabled = isEnabled,
                        onChange = { hour, minute -> settings.edit().putInt(key, 100 * hour + minute).apply() }
                    )
                }
                SettingKind.INFO -> SettingControl.Info(native.summary?.invoke(this@NextSettings))
                else -> null
            }
        }
    }

    @Composable
    private fun insightControl(setting: InsightSetting): SettingControl? =
        if (setting == InsightSetting.CONSENT) {
            SettingControl.Switch(
                checked = ledgerSource.consent.collectAsState(null).value == true,
                summary = stringResource(NextR.string.next_settings_consent_summary),
                isEnabled = true,
                onChange = ledgerSource::setConsent
            )
        } else {
            areaSettings.values.firstNotNullOfOrNull { it.control(setting) }
        }

    @Composable
    private fun InsightPage(setting: InsightSetting, close: () -> Unit) {
        val area = areaSettings.entries.firstOrNull { setting in it.key.settings }?.value
        // A setting without a page of its area, e.g. from an old back stack, closes again
        if (area != null) area.Page(setting, close) else LaunchedEffect(setting) { close() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(settingsViewModel)
        injector.inject(categoryViewModel)
        injector.inject(currencyViewModel)
        injector.inject(priceCalculationViewModel)
        categoryViewModel.defaultTransferCategory().observe(this) { defaultTransferCategory = it }
        supportFragmentManager.setFragmentResultListener(SELECT_CATEGORY_REQUEST, this) { _, result ->
            defaultTransferCategory = result.getString(KEY_PATH)
        }
        // What the old screens show only in some cases
        settingsViewModel.dataCorrupted().observe(this) { isDataCorrupted = it > 0 }
        settingsViewModel.shouldOfferCalendarRemoval().observe(this) { offersCalendarRemoval = it }
        settingsViewModel.appData.observe(this) { appFiles = it }
        lifecycleScope.launch {
            isPrivacyOptionsRequired = adHandlerFactory.isPrivacyOptionsRequired(this@NextSettings)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.hasStaleImages.collect { hasStaleImages = it }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currencyViewModel.currencies.collect { currencies = it }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStore.data.collect { dataStorePreferences = it }
            }
        }
        setContent {
            NextTheme {
                Box {
                    NextSettingsScreen(
                    actions,
                    onBack = ::finish,
                    initialGroup = intent.getStringExtra(EXTRA_GROUP)?.let { SettingsGroup.valueOf(it) }
                )
                }
                areaSettings.values.forEach { it.Dialogs() }
                homeCurrencyRequest?.let { code ->
                    ConfirmDialog(
                        title = getString(R.string.pref_home_currency_title) + ": " + currencyContext[code].description,
                        text = TextUtils.concatResStrings(
                            this, R.string.recalculate_equivalent_amounts_warning, R.string.continue_confirmation
                        ),
                        confirmLabel = getString(NextR.string.next_change),
                        onConfirm = {
                            homeCurrencyRequest = null
                            changeHomeCurrency(code, progressViewModel, priceCalculationViewModel)
                        },
                        onDismiss = { homeCurrencyRequest = null }
                    )
                }
                if (isAppFilesSheetShown) AppFilesSheet()
                pickRequest?.let { request ->
                    PickDialog(
                        title = request.title,
                        options = request.options,
                        label = { it.first },
                        onSelect = { pickRequest = null; it.second() },
                        onDismiss = { pickRequest = null }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.loadAppDirInfo()
        settingsViewModel.loadAppData()
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature == ContribFeature.CSV_IMPORT) {
            startActivity(Intent(this, CsvImportActivity::class.java))
        } else super.contribFeatureCalled(feature, tag)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        preferencesVersion++
        // Changes on an old screen get their side effects there
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        // As the old screens do, see PreferenceActivity
        when (key) {
            prefHandler.getKey(PrefKey.AUTO_BACKUP) -> {
                val autoBackup = sharedPreferences.getBoolean(key, false)
                if (autoBackup && (prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
                            prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)) &&
                    prefHandler.getString(PrefKey.EXPORT_PASSWORD, null) == null
                ) {
                    showMessage(unencryptedBackupWarning)
                }
                if (autoBackup) checkNotificationPermissionForAutoBackup()
                AutoBackupWorker.enqueueOrCancel(this, prefHandler)
            }
            prefHandler.getKey(PrefKey.AUTO_BACKUP_TIME) -> AutoBackupWorker.enqueueOrCancel(this, prefHandler)
            prefHandler.getKey(PrefKey.SYNC_FREQUCENCY) -> GenericAccountService.getAccounts(this).forEach {
                GenericAccountService.configureAutomaticAndPeriod(it, prefHandler)
            }
            // kept in the database too, for the sync adapter
            prefHandler.getKey(PrefKey.SYNC_NOTIFICATION), prefHandler.getKey(PrefKey.SYNC_WIFI_ONLY) ->
                settingsViewModel.storeSetting(key, sharedPreferences.getBoolean(key, false).toString())
                    .observe(this) { if (!it) showSnackBar("ERROR") }
            prefHandler.getKey(PrefKey.UI_THEME) -> {
                setNightMode(prefHandler, this)
                updateWidgets(AccountWidget::class.java, TemplateWidget::class.java)
            }
            prefHandler.getKey(PrefKey.UI_FONT_SIZE) -> {
                updateWidgets(AccountWidget::class.java, TemplateWidget::class.java, BudgetWidget::class.java)
                recreate()
            }
            prefHandler.getKey(PrefKey.CUSTOM_DECIMAL_FORMAT) -> currencyFormatter.invalidate(contentResolver)
            prefHandler.getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) -> updateWidgets(AccountWidget::class.java)
            prefHandler.getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) -> updateWidgets(TemplateWidget::class.java)
            prefHandler.getKey(PrefKey.PROTECTION_ENABLE_BUDGET_WIDGET) -> updateWidgets(BudgetWidget::class.java)
            prefHandler.getKey(PrefKey.TRACKING) -> tracker.setEnabled(sharedPreferences.getBoolean(key, false))
            prefHandler.getKey(PrefKey.CRASHREPORT_USEREMAIL) -> crashHandler.setUserEmail(sharedPreferences.getString(key, null))
            prefHandler.getKey(PrefKey.CRASHREPORT_ENABLED) -> {
                crashHandler.setEnabled(sharedPreferences.getBoolean(key, false))
                showSnackBar(R.string.app_restart_required)
            }
            prefHandler.getKey(PrefKey.PLANNER_EXECUTION_TIME) -> enqueuePlanner(false)
            prefHandler.getKey(PrefKey.UNMAPPED_TRANSACTION_AS_TRANSFER) -> {
                contentResolver.notifyChange(TRANSACTIONS_URI, null, false)
                contentResolver.notifyChange(ACCOUNTS_URI, null, false)
            }
        }
    }

    private fun updateWidgets(vararg providers: Class<out AppWidgetProvider>) {
        providers.forEach { updateWidgets(this, it, WIDGET_CONTEXT_CHANGED) }
    }

    private fun editDataStore(prefKey: PrefKey, change: (MutablePreferences) -> Unit) {
        lifecycleScope.launch {
            dataStore.edit(change)
            // As the old screen does, the list of an account shows future transactions differently
            if (prefKey == PrefKey.CRITERION_FUTURE) contentResolver.notifyChange(ACCOUNTS_URI, null, false)
        }
    }

    /** As the old screen does, a language that is not installed yet is downloaded first */
    internal fun changeLanguage(language: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && language != MyApplication.DEFAULT_LANGUAGE) {
            featureManager.requestLocale(language)
        } else {
            setLanguage(language)
        }
    }

    private fun customizeMenu(title: String) {
        pickRequest = PickRequest(
            title,
            listOf(MenuItem.MenuContext.V2Navigation, MenuItem.MenuContext.V2Transactions).map { menuContext ->
                menuContext.title(this) to {
                    CustomizeMenuDialogFragment.newInstance(menuContext).show(supportFragmentManager, "CUSTOMIZE_MENU")
                }
            }
        )
    }

    @Composable
    private fun AppFilesSheet() {
        val close = { isAppFilesSheetShown = false; pickedAppFiles = emptySet() }
        MultiChoiceSheet(
            title = stringResource(R.string.files_label),
            options = appFiles,
            selected = appFiles.filterTo(mutableSetOf()) { it.name in pickedAppFiles },
            onToggle = { file ->
                pickedAppFiles = if (file.name in pickedAppFiles) pickedAppFiles - file.name else pickedAppFiles + file.name
            },
            label = { it.format(this) },
            onConfirm = { shareAppFiles(pickedAppFiles); close() },
            onDismiss = close,
            footer = {
                DestructiveTextButton(
                    stringResource(R.string.menu_delete),
                    onClick = { isDeleteConfirmationShown = true },
                    icon = Icons.Default.Delete
                )
            },
            confirmEnabled = pickedAppFiles.isNotEmpty()
        )
        if (isDeleteConfirmationShown) {
            ConfirmDialog(
                title = stringResource(R.string.files_label),
                text = pluralStringResource(R.plurals.delete_files_confirmation_message, pickedAppFiles.size, pickedAppFiles.size),
                confirmLabel = stringResource(R.string.menu_delete),
                onConfirm = {
                    isDeleteConfirmationShown = false
                    settingsViewModel.deleteAppFiles(pickedAppFiles.toTypedArray()).observe(this@NextSettings) {
                        showSnackBar(resources.getQuantityString(R.plurals.delete_success, it, it))
                    }
                    close()
                },
                onDismiss = { isDeleteConfirmationShown = false }
            )
        }
    }

    /** As the old main screen */
    private fun shareAppFiles(names: Set<String>) {
        val appDir = settingsViewModel.appDirInfo.value?.getOrNull() ?: return
        startActivity(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(names.mapNotNull { name ->
                    appDir.documentFile.findFile(name)?.uri?.let { AppDirHelper.ensureContentUri(it, this@NextSettings) }
                })
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
    }

    private fun openLegacy(screen: LegacyScreen) {
        val header = when (val key = screen.header) {
            is SettingKey.Pref -> prefHandler.getKey(key.prefKey)
            is SettingKey.Plain -> key.key
            else -> null
        }
        legacyScreen.launch(PreferenceActivity.getIntent(this, header, singleScreen = true))
    }

    /** Entries of the list of all old screens, which the new settings do not show */
    private fun openMainEntry(prefKey: PrefKey) {
        when (prefKey) {
            PrefKey.MORE_INFO_DIALOG -> MoreInfoDialogFragment().show(supportFragmentManager, "MORE_INFO")
            PrefKey.BANKING_FINTS -> contribFeatureRequested(ContribFeature.BANKING)
            PrefKey.MANAGE_APP_DIR_FILES -> isAppFilesSheetShown = true
            PrefKey.APP_DIR -> if (prefHandler.getString(PrefKey.APP_DIR) == null) launchAppDirPicker() else {
                pickRequest = PickRequest(
                    getString(R.string.pref_app_dir_title),
                    listOf(
                        getString(R.string.checkbox_is_default) to { prefHandler.putString(PrefKey.APP_DIR, null) },
                        getString(R.string.select) to ::launchAppDirPicker
                    )
                )
            }
            else -> {}
        }
    }

    private fun launchAppDirPicker() {
        try {
            pickAppDir.launch(null)
        } catch (_: ActivityNotFoundException) {
            showSnackBar("Could not open directory picker. This feature may not be supported on your device.")
        }
    }

    companion object {
        private const val EXTRA_GROUP = "group"

        /** @param group opened right away, back then leaves the settings, e.g. from the tab of an analysis */
        fun intent(context: Context, group: SettingsGroup? = null) =
            Intent(context, NextSettings::class.java).putExtra(EXTRA_GROUP, group?.name)

        /** Entries of the list of all old screens that [openMainEntry] handles */
        private val nativeMainEntries =
            setOf(PrefKey.MORE_INFO_DIALOG, PrefKey.BANKING_FINTS, PrefKey.APP_DIR, PrefKey.MANAGE_APP_DIR_FILES)
    }
}
