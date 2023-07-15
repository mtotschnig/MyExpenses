package org.totschnig.myexpenses.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
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
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.*
import org.totschnig.myexpenses.preference.PreferenceDataStore
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.AppDirHelper.getContentUriForFile
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, OnDialogResultListener {

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

    private val viewModel: SettingsViewModel by viewModels()


    val preferenceActivity get() = requireActivity() as MyPreferenceActivity

    private fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun requireApplication(): MyApplication {
        return (requireActivity().application as MyApplication)
    }

    fun matches(preference: Preference, vararg prefKey: PrefKey) =
        prefKey.any { prefHandler.getKey(it) == preference.key }

    fun getKey(prefKey: PrefKey): String {
        return prefHandler.getKey(prefKey)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        when (rootKey) {

            null -> { //ROOT screen

                lifecycleScope.launchWhenStarted {
                    viewModel.hasStaleImages.collect { result ->
                        requirePreference<Preference>(PrefKey.MANAGE_STALE_IMAGES).isVisible =
                            result
                    }
                }

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

    override fun onPreferenceClick(preference: Preference): Boolean {
        return when {

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