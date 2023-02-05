/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Pair
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.fragment.SettingsFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel
import java.io.Serializable
import java.util.*

/**
 * Present references screen defined in Layout file
 *
 * @author Michael Totschnig
 */
class MyPreferenceActivity : ProtectedFragmentActivity(), ContribIFace,
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    lateinit var binding: SettingsBinding

    private val licenceValidationViewModel: LicenceValidationViewModel by viewModels()
    private var initialPrefToShow: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        if (savedInstanceState == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(binding.fragmentContainer.id, SettingsFragment(), FRAGMENT_TAG)
            ft.commit()
        }
        initialPrefToShow =
            if (savedInstanceState == null) intent.getStringExtra(KEY_OPEN_PREF_KEY) else null

        (applicationContext as MyApplication).appComponent.inject(licenceValidationViewModel)
        observeLicenceApiResult()
    }

    private val fragment: SettingsFragment
        get() = binding.fragmentContainer.getFragment()

    override fun inflateHelpMenu(menu: Menu?) {
        //currently no help menu
    }

    private fun observeLicenceApiResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                licenceValidationViewModel.result.collect { result ->
                    result?.let {
                        fragment.setProtectionDependentsState()
                        fragment.configureContribPrefs()
                        showDismissibleSnackBar(
                            it,
                            dismissCallback
                        )
                    }
                }
            }
        }
    }

    private val dismissCallback = object : Snackbar.Callback() {
        override fun onDismissed(
            transientBottomBar: Snackbar,
            event: Int
        ) {
            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                licenceValidationViewModel.messageShown()
        }
    }

    override fun doHelp(variant: String?) = false //handled in fragment

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog? = when (id) {
        R.id.FTP_DIALOG -> DialogUtils.sendWithFTPDialog(this)
        R.id.MORE_INFO_DIALOG -> {
            val builder = MaterialAlertDialogBuilder(this)
            val view = LayoutInflater.from(builder.context).inflate(R.layout.more_info, null)
            (view.findViewById<View>(R.id.aboutVersionCode) as TextView).text =
                getVersionInfo(this)
            val projectContainer = view.findViewById<TextView>(R.id.project_container)
            projectContainer.text = Utils.makeBulletList(this,
                Utils.getProjectDependencies(this)
                    .map { project: Map<String, String> ->
                        val name = project["name"]
                        "${if (project.containsKey("extra_info")) "$name (${project["extra_info"]})" else name}, from ${project["url"]}, licenced under ${project["licence"]}"
                    }.toList(), R.drawable.ic_menu_forward
            )
            val additionalContainer = view.findViewById<TextView>(R.id.additional_container)
            val lines: List<CharSequence> = listOf(
                *resources.getStringArray(R.array.additional_credits),
                "${getString(R.string.translated_by)}: ${buildTranslationCredits()}"
            )
            additionalContainer.text =
                Utils.makeBulletList(this, lines, R.drawable.ic_menu_forward)
            val iconContainer = view.findViewById<LinearLayout>(R.id.additional_icons_container)
            val iconLines =
                listOf<CharSequence>(*resources.getStringArray(R.array.additional_icon_credits))
            val ar = resources.obtainTypedArray(R.array.additional_icon_credits_keys)
            val len = ar.length()
            val height = UiUtils.dp2Px(32f, resources)
            val drawablePadding = UiUtils.dp2Px(8f, resources)
            var i = 0
            while (i < len) {
                val textView = TextView(this)
                textView.gravity = Gravity.CENTER_VERTICAL
                val layoutParams =
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                textView.layoutParams = layoutParams
                textView.compoundDrawablePadding = drawablePadding
                textView.text = iconLines[i]
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ar.getResourceId(i, 0),
                    0,
                    0,
                    0
                )
                iconContainer.addView(textView)
                i++
            }
            ar.recycle()
            builder.setTitle(R.string.pref_more_info_dialog_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }
        else -> {
            CrashHandler.report(IllegalStateException("onCreateDialog called with $id"))
            super.onCreateDialog(id)
        }
    }

    private fun buildTranslationCredits() =
        resources.getStringArray(R.array.pref_ui_language_values)
            .map { lang ->
                val parts = lang.split("-".toRegex()).toTypedArray()
                Pair.create(
                    lang,
                    getTranslatorsArrayResId(
                        parts[0],
                        if (parts.size == 2) parts[1].lowercase(Locale.ROOT) else null
                    )
                )
            }
            .filter { pair -> pair.second != 0 }
            .map { pair -> Pair.create(pair.first, resources.getStringArray(pair.second)) }
            .flatMap { pair -> pair.second.map { name -> Pair.create(name, pair.first) } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { entry -> "${entry.key} (${entry.value.joinToString(", ")})" }
            .joinToString(", ")

    fun getTranslatorsArrayResId(language: String, country: String?): Int {
        var result = 0
        val prefix = "translators_"
        if (!TextUtils.isEmpty(language)) {
            if (!TextUtils.isEmpty(country)) {
                result = resources.getIdentifier(
                    prefix + language + "_" + country,
                    "array", packageName
                )
            }
            if (result == 0) {
                result = resources.getIdentifier(
                    prefix + language,
                    "array", packageName
                )
            }
        }
        return result
    }

    fun validateLicence() {
        showSnackBarIndefinite(R.string.progress_validating_licence)
        licenceValidationViewModel.validateLicence()
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        if (feature == Feature.WEBUI) {
            fragment.bindToWebUiService()
            activateWebUi()
        }
        if (feature == Feature.OCR) {
            fragment.configureOcrEnginePrefs()
        }
    }

    fun showRestartInfo() {
        showSnackBar(R.string.app_restart_required)
    }

    private fun activateWebUi() {
        fragment.activateWebUi()
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature === ContribFeature.CSV_IMPORT) {
            val i = Intent(this, CsvImportActivity::class.java)
            startActivity(i)
        }
        if (feature === ContribFeature.WEB_UI) {
            if (featureViewModel.isFeatureAvailable(this, Feature.WEBUI)) {
                activateWebUi()
            } else {
                featureViewModel.requestFeature(this, Feature.WEBUI)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            initialPrefToShow = prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (initialPrefToShow != null) {
            fragment.showPreference(initialPrefToShow)
            initialPrefToShow = null
        }
    }

    override fun onPreferenceStartScreen(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ): Boolean {
        val key = preferenceScreen.key
        if (key == prefHandler.getKey(PrefKey.PERFORM_PROTECTION_SCREEN) &&
            (application as MyApplication).isProtected
        ) {
            confirmCredentials(
                CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST,
                { startPerformProtectionScreen() },
                false
            )
            return true
        }
        if (key == prefHandler.getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                //TODO on O we will be able to pin the shortcuts
                showSnackBar(R.string.home_screen_shortcuts_nougate_info)
                return true
            }
        }
        startPreferenceScreen(key)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                startPerformProtectionScreen()
            }
        }
    }

    private fun startPerformProtectionScreen() {
        startPreferenceScreen(prefHandler.getKey(PrefKey.PERFORM_PROTECTION_SCREEN))
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        if (command == R.id.REMOVE_LICENCE_COMMAND) {
            showSnackBarIndefinite(R.string.progress_removing_licence)
            licenceValidationViewModel.removeLicence()
            return true
        }
        if (command == R.id.CHANGE_COMMAND) {
            fragment.updateHomeCurrency(tag as String)
            return true
        }
        return false
    }

    private fun startPreferenceScreen(key: String) {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key)
        fragment.arguments = args
        ft.replace(R.id.fragment_container, fragment, key)
        ft.addToBackStack(key)
        ft.commitAllowingStateLoss()
    }

    fun showUnencryptedBackupWarning() {
        if (prefHandler.getString(PrefKey.EXPORT_PASSWORD, null) == null) showMessage(
            unencryptedBackupWarning
        )
    }

    companion object {
        const val KEY_OPEN_PREF_KEY = "openPrefKey"
        const val FRAGMENT_TAG = "Settings"
    }
}