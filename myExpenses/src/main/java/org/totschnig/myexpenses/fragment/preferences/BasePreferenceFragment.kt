package org.totschnig.myexpenses.fragment.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.XmlRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreferenceDialogFragment2
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Help
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat
import org.totschnig.myexpenses.preference.FontSizeDialogPreference
import org.totschnig.myexpenses.preference.HeaderPreference
import org.totschnig.myexpenses.preference.LegacyPasswordPreferenceDialogFragmentCompat
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceDataStore
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat
import org.totschnig.myexpenses.preference.SimplePasswordDialogFragmentCompat
import org.totschnig.myexpenses.preference.SimplePasswordPreference
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import javax.inject.Inject

abstract class BasePreferenceFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var preferenceDataStore: PreferenceDataStore

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var adHandlerFactory: AdHandlerFactory

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @get:XmlRes
    abstract val preferencesResId: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesResId, rootKey)
        headerPreference?.title = preferenceScreen.title
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            clipToPadding = false
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updatePadding(
                    top = systemBars.top,
                    bottom = systemBars.bottom
                )
                insets
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        headerPreference?.isVisible =
            !preferenceActivity.twoPanePreference.slidingPaneLayout.isSlideable
    }

    val headerPreference: HeaderPreference?
        get() = preferenceScreen.getPreference(0) as? HeaderPreference

    val preferenceActivity get() = requireActivity() as PreferenceActivity

    val viewModel: SettingsViewModel by viewModels()

    val storeInDatabaseChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            preferenceActivity.showSnackBarIndefinite(R.string.saving)
            viewModel.storeSetting(preference.key, newValue.toString())
                .observe(this@BasePreferenceFragment) { result ->
                    preferenceActivity.dismissSnackBar()
                    if ((!result)) preferenceActivity.showSnackBar("ERROR")
                }
            true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        with(injector) {
            inject(this@BasePreferenceFragment)
            inject(viewModel)
        }
        super.onCreate(savedInstanceState)
        prefHandler.preparePreferenceFragment(this)
    }

    override fun onResume() {
        super.onResume()
        settings.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        settings.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {}


    override fun onDisplayPreferenceDialog(preference: Preference) {
        val key = preference.key
        if (matches(preference, PrefKey.AUTO_BACKUP_CLOUD)) {
            if ((preference as ListPreference).entries.size == 1) {

                preferenceActivity.showSnackBar(R.string.no_sync_backends)
                return
            }
        }
        val fragment = when {
            preference is FontSizeDialogPreference -> FontSizeDialogFragmentCompat.newInstance(key)
            preference is SimplePasswordPreference ->
                SimplePasswordDialogFragmentCompat.newInstance(key)

            matches(preference, PrefKey.MANAGE_APP_DIR_FILES) ->
                MultiSelectListPreferenceDialogFragment2.newInstance(key)

            matches(preference, PrefKey.PROTECTION_LEGACY) -> {
                if (prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)) {
                    showOnlyOneProtectionWarning(false)
                    return
                } else {
                    LegacyPasswordPreferenceDialogFragmentCompat.newInstance(key)
                }
            }

            matches(preference, PrefKey.PLANNER_CALENDAR_ID) -> {
                if (PermissionGroup.CALENDAR.hasPermission(requireContext())) {
                    CalendarListPreferenceDialogFragmentCompat.newInstance(key)
                } else {
                    preferenceActivity.requestCalendarPermission()
                    return
                }
            }

            matches(
                preference,
                PrefKey.SECURITY_QUESTION
            ) -> SecurityQuestionDialogFragmentCompat.newInstance(key)

            else -> null
        }

        if (fragment != null) {
            fragment.setTargetFragment(this, 0)
            fragment.show(
                parentFragmentManager,
                "android.support.v7.preference.PreferenceFragment.DIALOG"
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    protected fun showOnlyOneProtectionWarning(legacyProtectionByPasswordIsActive: Boolean) {
        val lockScreen = getString(R.string.pref_protection_device_lock_screen_title)
        val passWord = getString(R.string.pref_protection_password_title)
        val formatArgs: Array<String> = if (legacyProtectionByPasswordIsActive) arrayOf(
            lockScreen,
            passWord
        ) else arrayOf(passWord, lockScreen)
        //noinspection StringFormatMatches
        preferenceActivity.showSnackBar(
            getString(
                R.string.pref_warning_only_one_protection,
                *formatArgs
            )
        )
    }

    fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun matches(preference: Preference, vararg prefKey: PrefKey) =
        preference.key?.let { prefHandler.matches(it, *prefKey) } == true

    fun getKey(prefKey: PrefKey) = prefHandler.getKey(prefKey)

    open val helpExtra: CharSequence? = null

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        trackPreferenceClick(preference)
        if (matches(preference, PrefKey.HELP)) {
            preference.summary?.takeIf { it.isNotEmpty() }?.also {
                preferenceActivity.startActionView(it.toString())
            } ?: run {
                startActivity(Intent(requireContext(), Help::class.java).apply {
                    putExtra(HelpDialogFragment.KEY_CONTEXT, preferenceScreen.key)
                    putExtra(
                        HelpDialogFragment.KEY_TITLE,
                        preferenceScreen.title
                    )
                    putExtra(HelpDialogFragment.KEY_EXTRA, helpExtra)
                })
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun trackPreferenceClick(preference: Preference) {
        val bundle = Bundle()
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.key)
        preferenceActivity.logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle)
    }

    fun handleContrib(prefKey: PrefKey, feature: ContribFeature, preference: Preference) =
        if (matches(preference, prefKey)) {
            preferenceActivity.contribFeatureRequested(feature)
            true
        } else false

    open fun showPreference(prefKey: String) {
        findPreference<Preference>(prefKey)?.let { onDisplayPreferenceDialog(it) }
    }

    val ioTitle: String
        get() = getString(R.string.pref_category_title_import) + " / " + getString(R.string.pref_category_title_export)

    val backupRestoreTitle: String
        get() = getString(R.string.menu_backup) + " / " + getString(R.string.pref_restore_title)

    val protectionTitle: String
        get() = getString(R.string.security_settings_title) + " / " + getString(R.string.privacy_header)
}