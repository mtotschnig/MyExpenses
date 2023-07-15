package org.totschnig.myexpenses.fragment

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.CompoundButton
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceChangeListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.SimpleListDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.*
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TransactionType
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.*
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference.OnValidationErrorListener
import org.totschnig.myexpenses.preference.PreferenceDataStore
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.AppDirHelper.getContentUriForFile
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel.Companion.parseUri
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), OnValidationErrorListener,
    OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, OnDialogResultListener {

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var adHandlerFactory: AdHandlerFactory

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var preferenceDataStore: PreferenceDataStore

    @Inject
    lateinit var tracker: Tracker

    private val currencyViewModel: CurrencyViewModel by viewModels()
    private val viewModel: SettingsViewModel by viewModels()

    private var masterSwitchChangeLister: CompoundButton.OnCheckedChangeListener? = null

    //TODO: these settings need to be authoritatively stored in Database, instead of just mirrored
    private val storeInDatabaseChangeListener =
        OnPreferenceChangeListener { preference, newValue ->
            preferenceActivity.showSnackBarIndefinite(R.string.saving)
            viewModel.storeSetting(preference.key, newValue.toString())
                .observe(this@BaseSettingsFragment) { result ->
                    preferenceActivity.dismissSnackBar()
                    if ((!result)) preferenceActivity.showSnackBar("ERROR")
                }
            true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        with(requireActivity().injector) {
            inject(currencyViewModel)
            inject(viewModel)
            super.onCreate(savedInstanceState)
            inject(this@BaseSettingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        settings.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        settings.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onValidationError(message: String) {
        preferenceActivity.showSnackBar(message)
    }

    val preferenceActivity get() = requireActivity() as MyPreferenceActivity

    private fun configureUninstallPrefs() {
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_FEATURES,
            featureManager.installedFeatures(requireContext(), prefHandler),
            featureManager::uninstallFeatures
        ) { module ->
            Feature.fromModuleName(module)?.let { getString(it.labelResId) } ?: module
        }
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_LANGUAGES,
            featureManager.installedLanguages(),
            featureManager::uninstallLanguages
        ) { language ->
            Locale(language).let { it.getDisplayName(it) }
        }
    }

    private fun configureMultiSelectListPref(
        prefKey: PrefKey,
        entries: Set<String>,
        action: (Set<String>) -> Unit,
        prettyPrint: (String) -> String
    ) {
        (requirePreference(prefKey) as? MultiSelectListPreference)?.apply {
            if (entries.isEmpty()) {
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { _, newValue ->
                    @Suppress("UNCHECKED_CAST")
                    (newValue as? Set<String>)?.let { action(it) }
                    false
                }
                setEntries(entries.map(prettyPrint).toTypedArray())
                entryValues = entries.toTypedArray()
            }
        }
    }

    private fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun requireApplication(): MyApplication {
        return (requireActivity().application as MyApplication)
    }

    private fun onScreen(vararg prefKey: PrefKey) = matches(preferenceScreen, *prefKey)

    fun matches(preference: Preference, vararg prefKey: PrefKey) =
        prefKey.any { prefHandler.getKey(it) == preference.key }

    fun getKey(prefKey: PrefKey): String {
        return prefHandler.getKey(prefKey)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        when (key) {

            getKey(PrefKey.CUSTOM_DECIMAL_FORMAT) -> {
                currencyFormatter.invalidateAll(requireContext().contentResolver)
            }

            getKey(PrefKey.GROUP_MONTH_STARTS), getKey(PrefKey.GROUP_WEEK_STARTS) -> {
                preferenceActivity.initLocaleContext()
            }

            getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) -> {
                //Log.d("DEBUG","shared preference changed: Account Widget");
                updateWidgetsForClass(AccountWidget::class.java)
            }

            getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) -> {
                //Log.d("DEBUG","shared preference changed: Template Widget");
                updateWidgetsForClass(TemplateWidget::class.java)
            }

            getKey(PrefKey.PLANNER_EXECUTION_TIME) -> {
                preferenceActivity.enqueuePlanner(false)
            }

            getKey(PrefKey.OPTIMIZE_PICTURE_FORMAT) -> {
                requirePreference<Preference>(PrefKey.OPTIMIZE_PICTURE_QUALITY).isEnabled =
                    prefHandler.enumValueOrDefault(
                        key,
                        Bitmap.CompressFormat.WEBP
                    ) != Bitmap.CompressFormat.PNG
            }
        }
    }

    override fun onPreferenceChange(pref: Preference, value: Any): Boolean {
        when {

            matches(pref, PrefKey.SHARE_TARGET) -> {
                val target = value as String
                val uri: URI?
                if (target != "") {
                    uri = parseUri(target)
                    if (uri == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.ftp_uri_malformed,
                                target
                            )
                        )
                        return false
                    }
                    val scheme = uri.scheme
                    if (enumValueOrNull<ShareViewModel.Scheme>(scheme.uppercase()) == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.share_scheme_not_supported,
                                scheme
                            )
                        )
                        return false
                    }
                    val intent: Intent
                    if (scheme == "ftp") {
                        intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(target)
                        if (!Utils.isIntentAvailable(requireActivity(), intent)) {
                            preferenceActivity.showDialog(R.id.FTP_DIALOG)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun updateWidgetsForClass(provider: Class<out AppWidgetProvider>) {
        updateWidgets(preferenceActivity, provider, WIDGET_CONTEXT_CHANGED)
    }

    private fun trackPreferenceClick(preference: Preference) {
        val bundle = Bundle()
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.key)
        preferenceActivity.logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle)
    }

    /**
     * sets listener and allows multi-line title for every preference in group, recursively
     */
    private fun configureRecursive(
        preferenceGroup: PreferenceGroup,
        listener: Preference.OnPreferenceClickListener
    ) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                configureRecursive(preference, listener)
            } else {
                preference.onPreferenceClickListener = listener
                preference.isSingleLineTitle = false
            }
        }
    }

    private fun unsetIconSpaceReservedRecursive(preferenceGroup: PreferenceGroup) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                unsetIconSpaceReservedRecursive(preference)
            }
            preference.isIconSpaceReserved = false
        }
    }

    private val homeScreenShortcutPrefClickHandler =
        Preference.OnPreferenceClickListener { preference: Preference ->
            trackPreferenceClick(preference)
            when {
                matches(preference, PrefKey.SHORTCUT_CREATE_TRANSACTION) -> {
                    addShortcut(
                        R.string.transaction, Transactions.TYPE_TRANSACTION,
                        R.drawable.shortcut_create_transaction_icon_lollipop
                    )
                    true
                }

                matches(preference, PrefKey.SHORTCUT_CREATE_TRANSFER) -> {
                    addShortcut(
                        R.string.transfer, Transactions.TYPE_TRANSFER,
                        R.drawable.shortcut_create_transfer_icon_lollipop
                    )
                    true
                }

                matches(preference, PrefKey.SHORTCUT_CREATE_SPLIT) -> {
                    addShortcut(
                        R.string.split_transaction, Transactions.TYPE_SPLIT,
                        R.drawable.shortcut_create_split_icon_lollipop
                    )
                    true
                }

                else -> false
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        configureRecursive(
            preferenceScreen, if (getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS) == rootKey)
                homeScreenShortcutPrefClickHandler
            else
                this
        )
        unsetIconSpaceReservedRecursive(preferenceScreen)

        when (rootKey) {

            null -> { //ROOT screen

                requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DECIMAL_FORMAT).onValidationErrorListener =
                    this

                requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DATE_FORMAT).onValidationErrorListener =
                    this

                lifecycleScope.launchWhenStarted {
                    preferenceDataStore.handleToggle(requirePreference(PrefKey.GROUP_HEADER))
                }

                lifecycleScope.launchWhenStarted {
                    preferenceDataStore.handleList(requirePreference(PrefKey.CRITERION_FUTURE))
                }

                lifecycleScope.launchWhenStarted {
                    viewModel.hasStaleImages.collect { result ->
                        requirePreference<Preference>(PrefKey.MANAGE_STALE_IMAGES).isVisible =
                            result
                    }
                }

                if (!featureManager.allowsUninstall()) {
                    requirePreference<Preference>(PrefKey.FEATURE_UNINSTALL).isVisible = false
                }
                requirePreference<Preference>(PrefKey.AUTO_BACKUP_CLOUD).onPreferenceChangeListener =
                    storeInDatabaseChangeListener

                requirePreference<Preference>(PrefKey.NEWS).title =
                    "${getString(R.string.pref_news_title)} (Mastodon)"

                requirePreference<Preference>(PrefKey.ENCRYPT_DATABASE_INFO).isVisible =
                    prefHandler.encryptDatabase
            }

            getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS) -> {
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
            }

            getKey(PrefKey.PERFORM_SHARE) -> {
                with(requirePreference<Preference>(PrefKey.SHARE_TARGET)) {
                    summary = getString(R.string.pref_share_target_summary) + " " +
                            ShareViewModel.Scheme.values().joinToString(
                                separator = ", ", prefix = "(", postfix = ")"
                            ) { it.name.lowercase() }
                    onPreferenceChangeListener = this@BaseSettingsFragment
                }
            }

            getKey(PrefKey.FEATURE_UNINSTALL) -> {
                configureUninstallPrefs()
            }

            getKey(PrefKey.DEBUG_SCREEN) -> {
                requirePreference<Preference>(PrefKey.CRASHLYTICS_USER_ID).let {
                    if (DistributionHelper.isGithub ||
                        !prefHandler.getBoolean(PrefKey.CRASHREPORT_ENABLED, false)
                    ) {
                        preferenceScreen.removePreference(it)
                    } else {
                        it.summary =
                            prefHandler.getString(PrefKey.CRASHLYTICS_USER_ID, null).toString()
                    }
                }
                viewModel.dataCorrupted().observe(this) {
                    if (it > 0) {
                        with(requirePreference<Preference>(PrefKey.DEBUG_REPAIR_987)) {
                            isVisible = true
                            title = "Inspect Corrupted Data ($it)"
                        }
                    }
                }
            }

            getKey(PrefKey.CSV_EXPORT) -> {
                preferenceScreen.title = getString(R.string.export_to_format, "CSV")
            }

            getKey(PrefKey.UI_TRANSACTION_LIST) -> {
                requirePreference<TwoStatePreference>(PrefKey.UI_ITEM_RENDERER_LEGACY).let {
                    it.title = requireContext().compactItemRendererTitle()
                    lifecycleScope.launchWhenStarted {
                        preferenceDataStore.handleToggle(it)
                    }
                }
                lifecycleScope.launchWhenStarted {
                    preferenceDataStore.handleToggle(requirePreference(PrefKey.UI_ITEM_RENDERER_CATEGORY_ICON))
                }
            }
        }
    }

    private fun getBitmapForShortcut(@DrawableRes iconId: Int) = UiUtils.drawableToBitmap(
        ResourcesCompat.getDrawable(
            resources,
            iconId,
            null
        )!!
    )

    private fun addShortcut(
        nameId: Int,
        @TransactionType operationType: Int,
        @DrawableRes iconIdLegacy: Int
    ) {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 -> {
                addShortcutLegacy(nameId, operationType, getBitmapForShortcut(iconIdLegacy))
            }
            //on Build.VERSION_CODES.N_MR1 we do not provide the feature
            Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 -> {
                try {
                    requireContext().getSystemService(ShortcutManager::class.java).requestPinShortcut(
                        ShortcutInfo.Builder(
                            requireContext(), when (operationType) {
                                Transactions.TYPE_SPLIT -> ShortcutHelper.ID_SPLIT
                                Transactions.TYPE_TRANSACTION -> ShortcutHelper.ID_TRANSACTION
                                Transactions.TYPE_TRANSFER -> ShortcutHelper.ID_TRANSFER
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

    /**
     * Configures the current screen with a Master Switch, if it has the given key
     * if we are on the root screen, the preference summary for the given key is updated with the
     * current value (On/Off)
     *
     * @param prefKey PrefKey of screen
     * @return true if we have handle the given key as a subScreen
     */
    fun handleScreenWithMasterSwitch(prefKey: PrefKey, disableDependents: Boolean): Boolean {
        if (onScreen(prefKey)) {
            preferenceActivity.supportActionBar?.let { actionBar ->
                val status = prefHandler.getBoolean(prefKey, false)
                val actionBarSwitch = requireActivity().layoutInflater.inflate(
                    R.layout.pref_master_switch, null
                ) as SwitchCompat
                actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM
                )
                actionBar.customView = actionBarSwitch
                actionBarSwitch.isChecked = status
                masterSwitchChangeLister = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    if (onPreferenceChange(preferenceScreen, isChecked)) {
                        prefHandler.putBoolean(prefKey, isChecked)
                        if (disableDependents) {
                            updateDependents(isChecked)
                        }
                    } else {
                        actionBarSwitch.isChecked = !isChecked
                    }
                }
                actionBarSwitch.setOnCheckedChangeListener(masterSwitchChangeLister)
                if (disableDependents) {
                    updateDependents(status)
                }
            }
            return true
        } else if (onScreen(PrefKey.ROOT_SCREEN)) {
            setOnOffSummary(prefKey)
        }
        return false
    }

    private fun setOnOffSummary(prefKey: PrefKey) {
        setOnOffSummary(prefKey, prefHandler.getBoolean(prefKey, false))
    }

    private fun setOnOffSummary(key: PrefKey, status: Boolean) {
        requirePreference<Preference>(key).summary = getString(
            if (status)
                R.string.switch_on_text
            else
                R.string.switch_off_text
        )
    }

    private fun updateDependents(enabled: Boolean) {
        for (i in 0 until preferenceScreen.preferenceCount) {
            preferenceScreen.getPreference(i).isEnabled = enabled
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        trackPreferenceClick(preference)
        return when {

            matches(preference, PrefKey.SEND_FEEDBACK) -> {
                preferenceActivity.dispatchCommand(R.id.FEEDBACK_COMMAND, null)
                true
            }

            matches(preference, PrefKey.DEBUG_LOG_SHARE) -> {
                viewModel.logData().observe(this) {
                    SimpleListDialog.build().choiceMode(CustomListDialog.MULTI_CHOICE)
                        .title(R.string.pref_debug_logging_share_summary)
                        .items(it)
                        .neg()
                        .pos(android.R.string.ok)
                        .show(this, DIALOG_SHARE_LOGS)
                }
                true
            }

            matches(preference, PrefKey.RATE) -> {
                prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, -1)
                preferenceActivity.dispatchCommand(R.id.RATE_COMMAND, null)
                true
            }

            matches(preference, PrefKey.DEBUG_REPAIR_987) -> {
                viewModel.prettyPrintCorruptedData(currencyFormatter).observe(this) { message ->
                    MessageDialogFragment.newInstance(
                        "Inspect Corrupted Data",
                        message,
                        MessageDialogFragment.okButton(),
                        null,
                        null
                    )
                        .show(parentFragmentManager, "INSPECT")
                }
                true
            }

            else -> false
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {

            DIALOG_SHARE_LOGS -> {
                if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                    val logDir = File(requireContext().getExternalFilesDir(null), "logs")
                    startActivity(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                        putExtra(Intent.EXTRA_SUBJECT, "[${getString(R.string.app_name)}]: Logs")
                        type = "text/plain"
                        val arrayList = ArrayList(
                            extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS)!!.map {
                                getContentUriForFile(
                                    requireContext(),
                                    File(logDir, it)
                                )
                            })
                        Timber.d("ATTACHMENTS" + arrayList.joinToString())
                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            arrayList
                        )
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                }
            }
        }
        return true
    }

    companion object {
        const val DIALOG_SHARE_LOGS = "shareLogs"
        const val KEY_CHECKED_FILES = "checkedFiles"

        fun Context.compactItemRendererTitle() =
            "${getString(R.string.style)} : ${getString(R.string.compact)}"

        fun newInstance(rootKey: String?) = SettingsFragment().apply {
            rootKey?.let {
                arguments = Bundle().apply {
                    putString(ARG_PREFERENCE_ROOT, it)
                }
            }
        }
    }
}